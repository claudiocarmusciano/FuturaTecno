package com.futuratecno.application;

import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comunica una única vez la prórroga del Sorteo Bienvenida a las cuentas que ya existían al
 * 31/08/2026. La marca de envío queda en la base: si Railway reinicia, no duplica emails; si uno
 * falla, sólo ese destinatario vuelve a intentarse en el próximo arranque.
 */
@Component
public class SorteoProrrogaNotificationRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(SorteoProrrogaNotificationRunner.class);
    private static final LocalDateTime LIMITE_BENEFICIO = LocalDateTime.of(2026, 9, 1, 0, 0);

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public SorteoProrrogaNotificationRunner(UsuarioRepository usuarioRepository, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!emailService.estaConfigurado()) {
            logger.warn("Aviso de prórroga no enviado: el servicio de email no está configurado.");
            return;
        }
        List<Usuario> destinatarios = usuarioRepository
                .findByRolAndCreatedAtBeforeAndAvisoProrrogaEnviadoEnIsNull("USUARIO", LIMITE_BENEFICIO);
        for (Usuario usuario : destinatarios) {
            try {
                emailService.enviarHtml(usuario.getEmail(), "Actualización del Sorteo Bienvenida — Futura Tecno", emailProrroga());
                usuario.setAvisoProrrogaEnviadoEn(LocalDateTime.now());
                logger.info("Aviso de prórroga enviado a {}", usuario.getEmail());
            } catch (Exception e) {
                logger.error("No se pudo enviar el aviso de prórroga a {}: {}", usuario.getEmail(), e.getMessage());
            }
        }
    }

    private String emailProrroga() {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; color: #16181d; line-height: 1.5;">
              <h2 style="margin-bottom: 8px;">Actualización del Sorteo Bienvenida</h2>
              <p>Queremos contarte una actualización importante sobre el sorteo de Futura Tecno.</p>
              <p>El sorteo se realizará cuando @futuratecnoargentina alcance los <strong>1.000 seguidores</strong> o, como fecha máxima, el <strong>31/10/2026</strong>.</p>
              <div style="margin: 22px 0; padding: 16px; border-radius: 10px; background: #eef5c9; border-left: 4px solid #C8E048;">
                <strong>Tu beneficio especial: doble chance.</strong><br>
                Como te registraste hasta el 31/08/2026, tu participación se incluirá dos veces en el padrón y la extracción una vez que completes todos los requisitos.
              </div>
              <p>Tu inscripción sigue vigente. Consultá las Bases y Condiciones actualizadas en <a href="https://futuratecno.com.ar/bases-y-condiciones" style="color: #5D6B14; font-weight: bold;">futuratecno.com.ar</a>.</p>
              <p style="font-size: 13px; color: #666;">Gracias por ser parte del comienzo de Futura Tecno.</p>
            </div>
            """;
    }
}
