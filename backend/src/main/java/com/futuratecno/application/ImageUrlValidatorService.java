package com.futuratecno.application;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Evita guardar como imagen enlaces a fichas, HTML, logos o URLs caídas.
 * La comprobación usa HEAD para no descargar archivos completos durante la búsqueda masiva.
 */
@Service
public class ImageUrlValidatorService {
    private final RestTemplate restTemplate;

    public ImageUrlValidatorService(@Qualifier("imageRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean esImagenDirecta(String url) {
        if (url == null || url.isBlank() || !url.trim().matches("https?://.+")) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120 Safari/537.36");
            headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
            ResponseEntity<Void> response = restTemplate.exchange(
                    url.trim(), HttpMethod.HEAD, new HttpEntity<>(headers), Void.class);
            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString().toLowerCase()
                    : "";
            return response.getStatusCode().is2xxSuccessful() && contentType.startsWith("image/");
        } catch (Exception ignored) {
            return false;
        }
    }
}
