package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API de Invid Computers (APIv1). Autenticación JWT:
 *   POST /api/v1/auth.php {username,password} -> access_token (Bearer, 24 h).
 * Catálogo: GET /api/v1/articulo.php (Bearer), paginado por next_page_url (100/página, 50 req/hora).
 * Credenciales por variables de entorno (INVID_BASE_URL, INVID_USERNAME, INVID_PASSWORD) — nunca en código.
 */
@Service
public class InvidApiClient {
    private static final Logger logger = LoggerFactory.getLogger(InvidApiClient.class);
    private static final int TOPE_PAGINAS = 300;          // tope de seguridad (300 x 100 = 30.000 items)
    private static final long CACHE_MINUTOS = 55;          // la cuota oficial es por hora: evita re-recorrer el catálogo en la misma ventana

    private final RestTemplate restTemplate;

    @Value("${invid.base-url:}")
    private String baseUrl;

    @Value("${invid.username:}")
    private String username;

    @Value("${invid.password:}")
    private String password;

    // Token JWT cacheado.
    private String token;
    private Instant tokenExpira;

    // Catálogos cacheados por modo de stock. Así preview+import reutilizan exactamente la misma
    // descarga; no mezclamos el catálogo completo con el que Invid ya filtró sin stock.
    private final Map<Boolean, List<JsonNode>> articulosCache = new HashMap<>();
    private final Map<Boolean, Instant> articulosCacheTs = new HashMap<>();

    public InvidApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return notBlank(baseUrl) && notBlank(username) && notBlank(password);
    }

    /** Devuelve un access_token válido, reautenticando si está vencido. */
    private synchronized String obtenerToken() {
        if (token != null && tokenExpira != null && Instant.now().isBefore(tokenExpira)) {
            return token;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("username", username.trim());
        body.put("password", password);

        String authUrl = base() + "/api/v1/auth.php";
        JsonNode resp;
        try {
            resp = restTemplate.postForObject(authUrl, new HttpEntity<>(body, headers), JsonNode.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            throw new IllegalStateException("Invid: la autenticación devolvió HTTP " + e.getStatusText()
                    + " en " + authUrl + ". Revisá INVID_BASE_URL, INVID_USERNAME y INVID_PASSWORD.");
        } catch (org.springframework.web.client.RestClientException e) {
            throw new IllegalStateException("Invid: no se pudo conectar a " + authUrl
                    + " — " + e.getMessage() + ". Revisá que INVID_BASE_URL sea el host correcto (con https://).");
        }
        if (resp == null || !resp.path("access_token").isTextual()) {
            throw new IllegalStateException("Invid: la respuesta de auth no trae access_token. "
                    + "Revisá usuario/contraseña. Respuesta: " + (resp != null ? resp.toString() : "vacía"));
        }
        token = resp.path("access_token").asText();
        long segundos = resp.path("expiration_time").asLong(86400);
        tokenExpira = Instant.now().plusSeconds(Math.max(segundos - 60, 60)); // margen de 60s
        logger.info("Invid: token JWT renovado (vence en {}s)", segundos);
        return token;
    }

    /** Trae TODO el catálogo siguiendo next_page_url. Cacheado {@value #CACHE_MINUTOS} min. */
    public synchronized List<JsonNode> obtenerArticulos() {
        return obtenerArticulos(false);
    }

    /**
     * Cuando se piden solo artículos con stock, el filtro se envía a Invid en cada página: hacerlo
     * después de descargar el catálogo no ahorra consultas. Los precios cero siempre se descartan
     * en el importador, por lo que también se excluyen desde el origen sin cambiar el resultado.
     */
    public synchronized List<JsonNode> obtenerArticulos(boolean soloConStock) {
        Instant cacheTs = articulosCacheTs.get(soloConStock);
        List<JsonNode> cache = articulosCache.get(soloConStock);
        if (cache != null && cacheTs != null
                && Duration.between(cacheTs, Instant.now()).toMinutes() < CACHE_MINUTOS) {
            return cache;
        }
        String jwt = obtenerToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> req = new HttpEntity<>(headers);

        List<JsonNode> acumulado = new ArrayList<>();
        String url = urlCatalogo(base() + "/api/v1/articulo.php", soloConStock);
        int paginas = 0;

        while (url != null && paginas < TOPE_PAGINAS) {
            ResponseEntity<JsonNode> resp;
            try {
                resp = restTemplate.exchange(url, HttpMethod.GET, req, JsonNode.class);
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                logger.warn("Invid: rate-limit (429) alcanzado en la página {}", paginas);
                String espera = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
                String detalle = "Probá de nuevo más tarde.";
                if (espera != null) {
                    try {
                        detalle = "Esperá aproximadamente " + Math.max(1, Long.parseLong(espera) / 60) + " minuto(s).";
                    } catch (NumberFormatException ignored) {
                        // Si Invid mandase un Retry-After no numérico, mantenemos un error claro.
                    }
                }
                throw new IllegalStateException("Invid limitó las consultas (50/hora). "
                        + detalle);
            }
            JsonNode bodyResp = resp.getBody();
            if (bodyResp == null) break;

            JsonNode data = bodyResp.path("data");
            if (data.isArray()) {
                data.forEach(acumulado::add);
            } else if (data.isObject()) {
                acumulado.add(data);
            }

            String next = bodyResp.path("next_page_url").asText(null);
            url = (next == null || next.isBlank() || "null".equalsIgnoreCase(next)) ? null : urlCatalogo(resolver(next), soloConStock);
            paginas++;
        }

        articulosCache.put(soloConStock, acumulado);
        articulosCacheTs.put(soloConStock, Instant.now());
        logger.info("Invid: catálogo traído ({} artículos, {} páginas, soloConStock={})", acumulado.size(), paginas, soloConStock);
        return acumulado;
    }

    /** Fuerza refrescar el catálogo en la próxima llamada. */
    public synchronized void invalidarCache() {
        articulosCache.clear();
        articulosCacheTs.clear();
    }

    /** Agrega filtros soportados por la API manteniéndolos también en las URLs de paginación. */
    private String urlCatalogo(String url, boolean soloConStock) {
        StringBuilder out = new StringBuilder(url);
        if (!url.contains("exclude_zero_price=")) out.append(url.contains("?") ? '&' : '?').append("exclude_zero_price=1");
        if (soloConStock && !url.contains("exclude_zero_stock=")) out.append(out.indexOf("?") >= 0 ? '&' : '?').append("exclude_zero_stock=1");
        return out.toString();
    }

    /**
     * Resuelve next_page_url a una URL absoluta y le fuerza la extensión .php en el path de
     * articulo. La API la devuelve sin .php (ej: /api/v1/articulo?offset=100) y el servidor,
     * por negociación de contenido de Apache, responde 406 a esa variante cuando el request
     * pide Accept: application/json — solo la ruta con .php acepta ese Accept.
     */
    private String resolver(String next) {
        String absoluta = (next.startsWith("http://") || next.startsWith("https://"))
                ? next
                : base() + (next.startsWith("/") ? next : "/" + next);
        return absoluta.replaceFirst("(?i)/articulo(\\?|$)", "/articulo.php$1");
    }

    /**
     * Normaliza el base-url a solo esquema+host (ej: https://invidcomputers.com), aunque el usuario
     * haya pegado de más una ruta como /api/v1/auth.php o una barra final. Evita rutas duplicadas.
     */
    private String base() {
        String b = baseUrl.trim();
        try {
            java.net.URI u = java.net.URI.create(b);
            if (u.getScheme() != null && u.getHost() != null) {
                String origin = u.getScheme() + "://" + u.getHost();
                if (u.getPort() != -1) origin += ":" + u.getPort();
                return origin;
            }
        } catch (Exception ignored) {
            // cae al fallback de abajo
        }
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
