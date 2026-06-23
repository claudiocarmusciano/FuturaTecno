package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca la imagen de un producto usando la herramienta de búsqueda web de Claude (API de Anthropic):
 *   1) Claude busca la página de compra del producto (sitio oficial de la marca o MercadoLibre Argentina).
 *   2) Se descarga esa página y se extrae el meta "og:image" (la imagen principal del producto).
 *
 * Usa la misma API de Anthropic que el parsing (no depende de Google/Bing/DuckDuckGo).
 */
@Service
public class AnthropicImageService {
    private static final Logger logger = LoggerFactory.getLogger(AnthropicImageService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'\\)\\]]+");
    private static final Pattern OG_1 = Pattern.compile("og:image[\"'][^>]+content=[\"']([^\"']+)[\"']");
    private static final Pattern OG_2 = Pattern.compile("content=[\"']([^\"']+)[\"'][^>]+og:image");

    // Si la og:image contiene alguno de estos fragmentos, es un logo/placeholder, no la foto del producto.
    private static final String[] BASURA = {"logo", "frontend-assets", "homes-palpatine", "/assets/", "placeholder", "sprite"};

    private final RestTemplate anthropicRestTemplate;
    private final RestTemplate pageRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-haiku-4-5-20251001}")
    private String model;

    public AnthropicImageService(RestTemplate restTemplate,
                                 @Qualifier("imageRestTemplate") RestTemplate imageRestTemplate,
                                 ObjectMapper objectMapper) {
        this.anthropicRestTemplate = restTemplate;       // sin timeout corto: la búsqueda web tarda
        this.pageRestTemplate = imageRestTemplate;        // timeout corto para descargar páginas
        this.objectMapper = objectMapper;
    }

    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> buscarImagen(String consulta) {
        if (!estaConfigurado() || consulta == null || consulta.isBlank()) {
            return Optional.empty();
        }
        try {
            String pagina = buscarPaginaProducto(consulta);
            if (pagina == null) {
                logger.warn("Anthropic: no se obtuvo página para '{}'", consulta);
                return Optional.empty();
            }
            String img = extraerOgImage(pagina);
            if (img != null) {
                logger.info("Anthropic: imagen encontrada para '{}' ({})", consulta, pagina);
                return Optional.of(img);
            }
            logger.warn("Anthropic: la página '{}' no tenía og:image válida para '{}'", pagina, consulta);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Anthropic: error para '{}': {}", consulta, e.toString());
            return Optional.empty();
        }
    }

    /** Pide a Claude (con web_search) la URL de la página de compra del producto. */
    private String buscarPaginaProducto(String consulta) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "web_search_20250305");
        tool.put("name", "web_search");
        tool.put("max_uses", 1); // 1 sola búsqueda por producto: capea el costo en ~US$0,01

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content",
                "Necesito la URL de la FICHA ESPECÍFICA de este producto para extraer su foto: " + consulta + " . "
                + "Preferí en este orden: (1) una publicación de MercadoLibre Argentina del producto exacto "
                + "(formato https://articulo.mercadolibre.com.ar/MLA-... o https://www.mercadolibre.com.ar/...-/p/MLA...), "
                + "(2) la ficha oficial del producto en el sitio de la marca. "
                + "La URL debe ser la página de UN producto puntual (no una categoría, búsqueda, home ni listado). "
                + "NO uses Amazon ni eBay. Respondé SOLO con la URL, sin ningún otro texto.");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("tools", List.of(tool));
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        JsonNode resp = anthropicRestTemplate.postForObject(API_URL, req, JsonNode.class);
        if (resp == null) return null;

        StringBuilder texto = new StringBuilder();
        for (JsonNode block : resp.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                texto.append(block.path("text").asText()).append(" ");
            }
        }
        Matcher m = URL_PATTERN.matcher(texto.toString());
        return m.find() ? m.group(0) : null;
    }

    /** Descarga la página y extrae el meta og:image, descartando logos/placeholders. */
    private String extraerOgImage(String pageUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, UA);
            headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.ALL));

            ResponseEntity<String> resp = pageRestTemplate.exchange(
                    pageUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String html = resp.getBody();
            if (html == null || html.isEmpty()) return null;

            Matcher m = OG_1.matcher(html);
            String url = m.find() ? m.group(1) : null;
            if (url == null) {
                m = OG_2.matcher(html);
                url = m.find() ? m.group(1) : null;
            }
            if (url == null) return null;

            url = url.replace("&amp;", "&").trim();
            if (!url.startsWith("http")) return null;

            String lower = url.toLowerCase();
            for (String basura : BASURA) {
                if (lower.contains(basura)) return null; // es un logo/placeholder, no el producto
            }
            return url;
        } catch (Exception e) {
            logger.debug("No se pudo extraer og:image de {}: {}", pageUrl, e.getMessage());
            return null;
        }
    }
}
