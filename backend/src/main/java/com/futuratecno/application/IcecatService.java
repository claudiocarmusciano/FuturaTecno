package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Busca imágenes de productos en Open Icecat por marca + código de producto (MPN).
 * Requiere credenciales de una cuenta Icecat (MyIcecat -> Access Tokens).
 */
@Service
public class IcecatService {
    private static final Logger logger = LoggerFactory.getLogger(IcecatService.class);
    private static final String BASE_URL = "https://live.icecat.biz/api";

    private final RestTemplate restTemplate;

    @Value("${icecat.username:}")
    private String username;

    @Value("${icecat.api-token:}")
    private String apiToken;

    @Value("${icecat.content-token:}")
    private String contentToken;

    @Value("${icecat.lang:ES}")
    private String lang;

    public IcecatService(@org.springframework.beans.factory.annotation.Qualifier("imageRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return !username.isBlank() && !apiToken.isBlank();
    }

    /**
     * Intenta varios códigos candidatos derivados del modelo hasta encontrar imagen.
     * Devuelve la URL de la imagen o vacío si no encontró.
     */
    public Optional<String> buscarImagen(String marca, String modelo) {
        if (!estaConfigurado()) {
            throw new IllegalStateException("Icecat no está configurado (faltan ICECAT_USERNAME / ICECAT_API_TOKEN).");
        }
        if (marca == null || marca.isBlank() || modelo == null || modelo.isBlank()) {
            return Optional.empty();
        }

        for (String codigo : candidatosCodigo(modelo)) {
            Optional<String> url = consultarIcecat(marca.trim(), codigo);
            if (url.isPresent()) {
                logger.info("Imagen Icecat encontrada para {} {} (código '{}')", marca, modelo, codigo);
                return url;
            }
        }
        logger.info("Sin imagen en Icecat para {} {}", marca, modelo);
        return Optional.empty();
    }

    /**
     * Genera códigos candidatos a partir de un modelo "sucio".
     * Ej: "NITRO V ANV15-52-586Z" -> ["NITRO V ANV15-52-586Z", "ANV15-52-586Z", "NITROVANV15-52-586Z"]
     */
    private List<String> candidatosCodigo(String modelo) {
        Set<String> candidatos = new LinkedHashSet<>();
        String limpio = modelo.trim();
        candidatos.add(limpio);

        String[] tokens = limpio.split("\\s+");
        if (tokens.length > 1) {
            // El último token suele ser el código de modelo (MPN).
            candidatos.add(tokens[tokens.length - 1]);
            // El token más "código" (el que tiene dígitos y guiones).
            for (int i = tokens.length - 1; i >= 0; i--) {
                if (tokens[i].matches(".*\\d.*")) {
                    candidatos.add(tokens[i]);
                    break;
                }
            }
        }
        candidatos.add(limpio.replaceAll("\\s+", ""));
        return new ArrayList<>(candidatos);
    }

    private Optional<String> consultarIcecat(String marca, String productCode) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("lang", lang)
                    .queryParam("shopname", username)
                    .queryParam("Brand", marca)
                    .queryParam("ProductCode", productCode)
                    .queryParam("content", "")
                    .encode()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-token", apiToken);
            if (!contentToken.isBlank()) {
                headers.set("content-token", contentToken);
            }

            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

            JsonNode body = resp.getBody();
            if (body == null) return Optional.empty();

            return extraerUrlImagen(body);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
            return Optional.empty(); // producto no encontrado en Icecat
        } catch (Exception e) {
            logger.warn("Error consultando Icecat para {} {}: {}", marca, productCode, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extraerUrlImagen(JsonNode root) {
        // La estructura puede venir como {"data": {"Image": {...}}} o {"Image": {...}}.
        JsonNode image = root.path("data").path("Image");
        if (image.isMissingNode() || image.isEmpty()) {
            image = root.path("Image");
        }
        if (image.isMissingNode() || image.isEmpty()) {
            return Optional.empty();
        }
        for (String campo : new String[]{"Pic500x500", "HighPic", "LowPic", "ThumbPic"}) {
            String val = image.path(campo).asText("");
            if (val != null && val.startsWith("http")) {
                return Optional.of(val);
            }
        }
        return Optional.empty();
    }
}
