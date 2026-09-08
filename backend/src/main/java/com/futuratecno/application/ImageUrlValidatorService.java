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
 * La comprobación intenta primero HEAD para no descargar archivos completos. Algunos CDNs
 * (por ejemplo, el de Frávega) contestan HEAD sin Content-Type aunque la imagen exista; en
 * ese caso se hace un GET con Range, se lee sólo la cabecera y se valida el MIME real.
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
        HttpHeaders headers = encabezadosImagen();
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url.trim(), HttpMethod.HEAD, new HttpEntity<>(headers), Void.class);
            if (esRespuestaImagen(response.getStatusCode().is2xxSuccessful(), response.getHeaders())) {
                return true;
            }
        } catch (Exception ignored) {
            // Hay servidores que directamente rechazan HEAD. El GET parcial de abajo es el
            // fallback seguro y evita marcar como rota una URL de imagen válida.
        }

        try {
            headers.set(HttpHeaders.RANGE, "bytes=0-1023");
            Boolean esImagen = restTemplate.execute(url.trim(), HttpMethod.GET,
                    request -> request.getHeaders().putAll(headers),
                    response -> esRespuestaImagen(response.getStatusCode().is2xxSuccessful(), response.getHeaders()));
            return Boolean.TRUE.equals(esImagen);
        } catch (Exception ignored) {
            return false;
        }
    }

    private HttpHeaders encabezadosImagen() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        return headers;
    }

    private boolean esRespuestaImagen(boolean estadoExitoso, HttpHeaders headers) {
        String contentType = headers.getContentType() != null
                ? headers.getContentType().toString().toLowerCase()
                : "";
        return estadoExitoso && contentType.startsWith("image/");
    }
}
