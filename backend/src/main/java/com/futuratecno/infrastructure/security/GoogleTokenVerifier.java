package com.futuratecno.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Verifica el ID token (credential) que devuelve "Sign in with Google" en el frontend.
 *
 * Usa el endpoint público de Google {@code tokeninfo}, que valida la firma y la expiración
 * del token del lado de Google. Nosotros solo comprobamos que el token fue emitido PARA
 * nuestra app ({@code aud} == nuestro Client ID), que lo emitió Google ({@code iss}) y que
 * el email está verificado. Este enfoque no agrega dependencias nuevas (mismo patrón
 * RestTemplate que el resto de integraciones externas del proyecto).
 *
 * El Client ID es un valor público (no un secreto): se configura con la env var
 * {@code GOOGLE_CLIENT_ID} y es el único lugar donde vive.
 */
@Component
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final RestTemplate restTemplate;

    @Value("${google.client-id:}")
    private String clientId;

    public GoogleTokenVerifier(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return clientId != null && !clientId.isBlank();
    }

    public String getClientId() {
        return clientId == null ? "" : clientId;
    }

    /** Datos mínimos que necesitamos de la cuenta de Google. */
    public record GoogleUser(String sub, String email, String nombre) {}

    public GoogleUser verificar(String credential) {
        if (!estaConfigurado()) {
            throw new IllegalStateException("El login con Google no está configurado en el servidor.");
        }
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("Falta el token de Google.");
        }

        JsonNode info;
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TOKENINFO_URL)
                    .queryParam("id_token", credential)
                    .toUriString();
            info = restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            // tokeninfo devuelve 400 si el token es inválido o expiró.
            throw new IllegalArgumentException("Token de Google inválido o expirado.");
        }
        if (info == null) {
            throw new IllegalArgumentException("Token de Google inválido.");
        }

        // El token debe haber sido emitido para NUESTRA app.
        if (!clientId.equals(info.path("aud").asText(""))) {
            throw new IllegalArgumentException("El token de Google no corresponde a esta aplicación.");
        }
        // ...y por Google.
        String iss = info.path("iss").asText("");
        if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
            throw new IllegalArgumentException("Emisor del token de Google no válido.");
        }

        String email = info.path("email").asText("").toLowerCase();
        boolean emailVerified = "true".equalsIgnoreCase(info.path("email_verified").asText("false"));
        if (email.isBlank() || !emailVerified) {
            throw new IllegalArgumentException("La cuenta de Google no tiene un email verificado.");
        }

        String sub = info.path("sub").asText("");
        if (sub.isBlank()) {
            throw new IllegalArgumentException("Token de Google inválido.");
        }

        String nombre = info.hasNonNull("name") ? info.get("name").asText() : null;
        return new GoogleUser(sub, email, nombre);
    }
}
