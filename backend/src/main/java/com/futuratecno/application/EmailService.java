package com.futuratecno.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Envío de emails por la API HTTP de Resend (https://resend.com).
 *
 * <p>No usamos SMTP: Railway bloquea el tráfico saliente a los puertos SMTP (25/465/587) como
 * política antispam, así que cualquier intento contra smtp.gmail.com muere en
 * "ConnectException: Connection timed out" después de colgar el hilo del request varios minutos.
 * Resend manda por HTTPS/443, que sí sale.
 *
 * <p>Mismo criterio que el resto de integraciones: si no hay RESEND_API_KEY / MAIL_FROM,
 * {@link #estaConfigurado()} es false y el sistema sigue funcionando sin mandar correos (útil en dev).
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;

    @Value("${app.mail.resend-api-key:}")
    private String apiKey;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:FuturaTecno}")
    private String fromName;

    public EmailService(@Qualifier("mailRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank() && from != null && !from.isBlank();
    }

    /**
     * Envía un email HTML de forma sincrónica. Lanza RuntimeException si falla.
     * Para todo lo que dispara un usuario final preferí {@link #enviarHtmlAsync}: este método
     * hace esperar al request hasta que Resend responda.
     */
    public void enviarHtml(String para, String asunto, String htmlBody) {
        if (!estaConfigurado()) {
            logger.warn("Email NO enviado (Resend sin configurar: faltan RESEND_API_KEY / MAIL_FROM). Destinatario: {}", para);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "from", fromName + " <" + from + ">",
                    "to", List.of(para),
                    "subject", asunto,
                    "html", htmlBody);

            restTemplate.postForObject(API_URL, new HttpEntity<>(body, headers), String.class);
            logger.info("Email enviado a {} (asunto: {})", para, asunto);
        } catch (Exception e) {
            logger.error("Falló el envío de email a {}: {}", para, e.toString());
            throw new RuntimeException("No se pudo enviar el email.", e);
        }
    }

    /**
     * Igual que {@link #enviarHtml} pero en otro hilo y sin propagar errores: el mail es un efecto
     * secundario y nunca debe demorar ni hacer fallar la operación que lo dispara (un pedido se
     * guarda igual aunque Resend esté caído). El resultado queda en el log.
     */
    @Async
    public void enviarHtmlAsync(String para, String asunto, String htmlBody) {
        try {
            enviarHtml(para, asunto, htmlBody);
        } catch (Exception e) {
            // Ya quedó logueado en enviarHtml; acá solo evitamos que la excepción muera
            // silenciosamente en el executor.
        }
    }
}
