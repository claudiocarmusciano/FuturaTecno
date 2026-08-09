package com.futuratecno.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Envía el OTP por la Cloud API directa de Meta. Requiere una plantilla de autenticación aprobada. */
@Service
public class WhatsAppVerificationService {
    private final RestTemplate restTemplate;

    @Value("${whatsapp.meta.access-token:}") private String accessToken;
    @Value("${whatsapp.meta.phone-number-id:}") private String phoneNumberId;
    @Value("${whatsapp.meta.template:codigo_verificacion}") private String template;
    @Value("${whatsapp.meta.api-version:v22.0}") private String apiVersion;

    public WhatsAppVerificationService(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public boolean estaConfigurado() {
        return accessToken != null && !accessToken.isBlank() && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    public void enviarCodigo(String celularE164, String codigo) {
        if (!estaConfigurado()) throw new IllegalStateException("La verificación por WhatsApp todavía no está configurada.");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", celularE164.replace("+", ""),
                "type", "template",
                "template", Map.of("name", template, "language", Map.of("code", "es_AR"),
                        "components", List.of(Map.of("type", "body", "parameters", List.of(
                                Map.of("type", "text", "text", codigo))))));
        try {
            restTemplate.postForObject("https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages",
                    new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el código por WhatsApp. Probá nuevamente en unos minutos.");
        }
    }
}
