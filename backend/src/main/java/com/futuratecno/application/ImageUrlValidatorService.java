package com.futuratecno.application;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Verifica tipo y firma de imagen; valida cada destino antes de seguir redirecciones. */
@Service
public class ImageUrlValidatorService {
    private final RestTemplate restTemplate;
    public ImageUrlValidatorService(@Qualifier("imageRestTemplate") RestTemplate restTemplate) { this.restTemplate = restTemplate; }
    private record Resultado(boolean imagen, String siguiente) {}

    /**
     * MUERTA solo con 404/410: es lo único definitivo. Un 403, un 5xx o un timeout pueden ser un
     * sitio que bloquea pedidos desde un datacenter y carga perfecto en el navegador del cliente,
     * así que quedan como DUDOSA y no alcanzan para descartar una foto.
     */
    public enum Verificacion { IMAGEN, MUERTA, DUDOSA }

    public boolean esImagenDirecta(String url) {
        return verificar(url) == Verificacion.IMAGEN;
    }

    public Verificacion verificar(String url) {
        String destino = url == null ? "" : url.trim();
        for (int i = 0; i < 3; i++) {
            if (!UrlPublica.permitida(destino)) return Verificacion.DUDOSA;
            try {
                Resultado r = restTemplate.execute(destino, HttpMethod.GET, request -> {
                    request.getHeaders().set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
                    request.getHeaders().set(HttpHeaders.RANGE, "bytes=0-1023");
                    request.getHeaders().set(HttpHeaders.ACCEPT, "image/*");
                }, response -> {
                    if (response.getStatusCode().is3xxRedirection()) return new Resultado(false, response.getHeaders().getFirst(HttpHeaders.LOCATION));
                    MediaType type = response.getHeaders().getContentType();
                    if (!response.getStatusCode().is2xxSuccessful() || type == null || !"image".equalsIgnoreCase(type.getType())) return new Resultado(false, null);
                    return new Resultado(esFirmaImagen(response.getBody().readNBytes(32)), null);
                });
                if (r == null || r.siguiente() == null) return r != null && r.imagen() ? Verificacion.IMAGEN : Verificacion.DUDOSA;
                destino = URI.create(destino).resolve(r.siguiente()).toString();
            } catch (HttpClientErrorException e) {
                int codigo = e.getStatusCode().value();
                return codigo == 404 || codigo == 410 ? Verificacion.MUERTA : Verificacion.DUDOSA;
            } catch (Exception e) { return Verificacion.DUDOSA; }
        }
        return Verificacion.DUDOSA;
    }

    static boolean esFirmaImagen(byte[] b) {
        if (b.length < 12) return false;
        String head = new String(b, StandardCharsets.ISO_8859_1);
        return ((b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
                || head.startsWith("\u0089PNG\r\n\u001a\n") || head.startsWith("GIF87a") || head.startsWith("GIF89a")
                || (head.startsWith("RIFF") && head.substring(8, 12).equals("WEBP"))
                || (head.substring(4, 8).equals("ftyp") && (head.contains("avif") || head.contains("avis")));
    }
}
