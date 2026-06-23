package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca imágenes reales de productos usando el endpoint (no oficial) de imágenes de DuckDuckGo.
 * No requiere API key ni credenciales. Como es no oficial, puede fallar ocasionalmente.
 */
@Service
public class DuckDuckGoImageService {
    private static final Logger logger = LoggerFactory.getLogger(DuckDuckGoImageService.class);
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static final Pattern VQD_1 = Pattern.compile("vqd=[\"']([0-9-]+)[\"']");
    private static final Pattern VQD_2 = Pattern.compile("vqd=([0-9-]+)&");
    private static final Pattern VQD_3 = Pattern.compile("vqd=([0-9-]{10,})");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DuckDuckGoImageService(@Qualifier("imageRestTemplate") RestTemplate restTemplate,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean estaConfigurado() {
        return true; // no necesita credenciales
    }

    public Optional<String> buscarImagen(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return Optional.empty();
        }
        try {
            String vqd = obtenerVqd(consulta);
            if (vqd == null) {
                logger.warn("DuckDuckGo: no se obtuvo token vqd para '{}'", consulta);
                return Optional.empty();
            }
            logger.debug("DuckDuckGo: vqd='{}' para '{}'", vqd, consulta);

            String q = URLEncoder.encode(consulta, StandardCharsets.UTF_8);
            String apiUrl = "https://duckduckgo.com/i.js?l=us-en&o=json&q=" + q + "&vqd=" + vqd + "&f=,,,&p=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, UA);
            headers.set(HttpHeaders.REFERER, "https://duckduckgo.com/");
            headers.setAccept(List.of(MediaType.ALL));

            // Pedimos la respuesta como TEXTO y la parseamos a mano (DDG suele devolver
            // content-type text/javascript, que el conversor JSON de Spring no acepta).
            ResponseEntity<String> resp = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            String cuerpo = resp.getBody();
            if (cuerpo == null || cuerpo.isBlank()) {
                logger.warn("DuckDuckGo: respuesta i.js vacía para '{}'", consulta);
                return Optional.empty();
            }

            JsonNode body = objectMapper.readTree(cuerpo);
            JsonNode results = body.path("results");
            if (results.isArray() && results.size() > 0) {
                String img = results.get(0).path("image").asText("");
                if (img.startsWith("http")) {
                    logger.info("DuckDuckGo: imagen encontrada para '{}'", consulta);
                    return Optional.of(img);
                }
            }
            logger.warn("DuckDuckGo: sin resultados en i.js para '{}'", consulta);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("DuckDuckGo: error para '{}': {}", consulta, e.toString());
            return Optional.empty();
        }
    }

    private String obtenerVqd(String consulta) {
        String url = UriComponentsBuilder.fromHttpUrl("https://duckduckgo.com/")
                .queryParam("q", consulta)
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, UA);
        headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.ALL));

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String html = resp.getBody();
        if (html == null || html.isEmpty()) {
            logger.warn("DuckDuckGo: HTML vacío al pedir vqd (¿bloqueo de IP?)");
            return null;
        }

        Matcher m = VQD_1.matcher(html);
        if (m.find()) return m.group(1);
        m = VQD_2.matcher(html);
        if (m.find()) return m.group(1);
        m = VQD_3.matcher(html);
        if (m.find()) return m.group(1);
        logger.warn("DuckDuckGo: no se encontró vqd en el HTML (len={})", html.length());
        return null;
    }
}
