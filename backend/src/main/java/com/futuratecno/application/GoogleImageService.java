package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Busca imágenes de productos vía Google Custom Search API (Programmable Search Engine).
 * Requiere una API key de Google Cloud y un Search Engine ID (cx) con búsqueda de imágenes habilitada.
 * Free tier: 100 búsquedas por día.
 */
@Service
public class GoogleImageService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleImageService.class);
    private static final String BASE_URL = "https://www.googleapis.com/customsearch/v1";

    private final RestTemplate restTemplate;

    @Value("${google.search.api-key:}")
    private String apiKey;

    @Value("${google.search.cx:}")
    private String cx;

    public GoogleImageService(@org.springframework.beans.factory.annotation.Qualifier("imageRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return !apiKey.isBlank() && !cx.isBlank();
    }

    /**
     * Busca la primera imagen relevante para el texto dado (ej: "Notebook gamer HP VICTUS 15-FB2063").
     */
    public Optional<String> buscarImagen(String consulta) {
        if (!estaConfigurado() || consulta == null || consulta.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("key", apiKey)
                    .queryParam("cx", cx)
                    .queryParam("q", consulta)
                    .queryParam("searchType", "image")
                    .queryParam("num", 1)
                    .queryParam("safe", "active")
                    .encode()
                    .toUriString();

            JsonNode resp = restTemplate.getForObject(url, JsonNode.class);
            if (resp == null) return Optional.empty();

            JsonNode items = resp.path("items");
            if (items.isArray() && items.size() > 0) {
                String link = items.get(0).path("link").asText("");
                if (link.startsWith("http")) {
                    logger.info("Imagen de Google encontrada para '{}'", consulta);
                    return Optional.of(link);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error buscando imagen en Google para '{}': {}", consulta, e.getMessage());
            return Optional.empty();
        }
    }
}
