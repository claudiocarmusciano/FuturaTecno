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
 * Catálogo: GET /api/v1/articulo.php (Bearer), paginado por next_page_url (100/página, 200 req/hora).
 * Credenciales por variables de entorno (INVID_BASE_URL, INVID_USERNAME, INVID_PASSWORD) — nunca en código.
 */
@Service
public class InvidApiClient {
    private static final Logger logger = LoggerFactory.getLogger(InvidApiClient.class);
    private static final int TOPE_PAGINAS = 300;          // tope de seguridad (300 x 100 = 30.000 items)
    private static final long CACHE_MINUTOS = 10;          // cachea el catálogo para respetar el rate-limit

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

    // Catálogo cacheado (para no llamar a la API en filtros + preview + import por separado).
    private List<JsonNode> articulosCache;
    private Instant articulosCacheTs;

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
        if (articulosCache != null && articulosCacheTs != null
                && Duration.between(articulosCacheTs, Instant.now()).toMinutes() < CACHE_MINUTOS) {
            return articulosCache;
        }
        String jwt = obtenerToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> req = new HttpEntity<>(headers);

        List<JsonNode> acumulado = new ArrayList<>();
        String url = base() + "/api/v1/articulo.php";
        int paginas = 0;

        while (url != null && paginas < TOPE_PAGINAS) {
            ResponseEntity<JsonNode> resp;
            try {
                resp = restTemplate.exchange(url, HttpMethod.GET, req, JsonNode.class);
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                logger.warn("Invid: rate-limit (429) alcanzado en la página {}", paginas);
                throw new IllegalStateException("Invid limitó las consultas (200/hora). Probá de nuevo más tarde.");
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
            url = (next == null || next.isBlank() || "null".equalsIgnoreCase(next)) ? null : resolver(next);
            paginas++;
        }

        articulosCache = acumulado;
        articulosCacheTs = Instant.now();
        logger.info("Invid: catálogo traído ({} artículos, {} páginas)", acumulado.size(), paginas);
        return acumulado;
    }

    /** Fuerza refrescar el catálogo en la próxima llamada. */
    public synchronized void invalidarCache() {
        articulosCache = null;
        articulosCacheTs = null;
    }

    private String resolver(String next) {
        if (next.startsWith("http://") || next.startsWith("https://")) return next;
        return base() + (next.startsWith("/") ? next : "/" + next);
    }

    private String base() {
        String b = baseUrl.trim();
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
