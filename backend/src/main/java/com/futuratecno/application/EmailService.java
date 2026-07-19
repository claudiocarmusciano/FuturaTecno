package com.futuratecno.application;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envío de emails por SMTP. Configurable con env vars (mismo criterio que el resto de integraciones):
 * si no hay MAIL_HOST/MAIL_FROM, {@link #estaConfigurado()} es false y el sistema sigue funcionando
 * sin mandar correos (útil en dev). Hoy se usa para el "olvidé mi contraseña".
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:FuturaTecno}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean estaConfigurado() {
        return host != null && !host.isBlank() && from != null && !from.isBlank();
    }

    /** Envía un email HTML. Lanza RuntimeException si falla el envío. */
    public void enviarHtml(String para, String asunto, String htmlBody) {
        if (!estaConfigurado()) {
            logger.warn("Email NO enviado (SMTP sin configurar: faltan MAIL_HOST / MAIL_FROM). Destinatario: {}", para);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            logger.info("Email enviado a {} (asunto: {})", para, asunto);
        } catch (Exception e) {
            logger.error("Falló el envío de email a {}: {}", para, e.toString());
            throw new RuntimeException("No se pudo enviar el email.", e);
        }
    }
}
