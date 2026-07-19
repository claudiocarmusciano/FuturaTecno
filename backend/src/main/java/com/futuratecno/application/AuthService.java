package com.futuratecno.application;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.api.dto.RegisterRequest;
import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.UsuarioRepository;
import com.futuratecno.infrastructure.security.GoogleTokenVerifier;
import com.futuratecno.infrastructure.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RESET_TOKEN_TTL_MINUTOS = 60;   // el enlace de reseteo vale 1 hora

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailService emailService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, GoogleTokenVerifier googleTokenVerifier,
                       EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty() || req.getPassword() == null || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("Email válido y contraseña de al menos 6 caracteres son obligatorios.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email.");
        }

        Usuario u = new Usuario();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNombre(req.getNombre() != null ? req.getNombre().trim() : null);
        u.setRol("USUARIO");
        u.setActivo(true);
        usuarioRepository.save(u);

        String token = jwtService.generarToken(u.getEmail(), u.getRol());
        return new AuthResponse(token, u.getEmail(), u.getNombre(), u.getRol());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        Usuario u = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                .orElse(null);

        if (u == null || req.getPassword() == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos.");
        }

        String token = jwtService.generarToken(u.getEmail(), u.getRol());
        return new AuthResponse(token, u.getEmail(), u.getNombre(), u.getRol());
    }

    /**
     * Login/registro con Google. Verifica el ID token contra Google y:
     *  - si ya hay una cuenta con ese "sub" de Google, la usa;
     *  - si no, pero existe una cuenta con el mismo email (creada con email/contraseña),
     *    la vincula al Google de esa persona (así no quedan cuentas duplicadas);
     *  - si no existe ninguna, crea una nueva con rol USUARIO (sin contraseña local).
     * Nunca otorga rol ADMIN: los usuarios de Google son siempre clientes.
     */
    @Transactional
    public AuthResponse loginConGoogle(String credential) {
        GoogleTokenVerifier.GoogleUser g = googleTokenVerifier.verificar(credential);

        Usuario u = usuarioRepository.findByGoogleSub(g.sub()).orElse(null);
        if (u == null) {
            u = usuarioRepository.findByEmailIgnoreCase(g.email()).orElse(null);
            if (u != null) {
                // Cuenta existente con ese email → la vinculamos a Google.
                u.setGoogleSub(g.sub());
                if ((u.getNombre() == null || u.getNombre().isBlank()) && g.nombre() != null) {
                    u.setNombre(g.nombre());
                }
                usuarioRepository.save(u);
            } else {
                // Cuenta nueva creada desde Google.
                u = new Usuario();
                u.setEmail(g.email());
                u.setNombre(g.nombre());
                u.setGoogleSub(g.sub());
                u.setPassword(null); // sin contraseña local
                u.setRol("USUARIO");
                u.setActivo(true);
                usuarioRepository.save(u);
            }
        }

        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new IllegalArgumentException("La cuenta está deshabilitada.");
        }

        String token = jwtService.generarToken(u.getEmail(), u.getRol());
        return new AuthResponse(token, u.getEmail(), u.getNombre(), u.getRol());
    }

    /**
     * "Olvidé mi contraseña": si existe una cuenta activa con ese email, genera un token de reseteo,
     * guarda su hash con vencimiento y manda el enlace por email. Nunca revela si el email existe o no
     * (para no filtrar qué cuentas están registradas): siempre termina sin error.
     *
     * @param baseUrl origen del sitio (ej. https://futuratecno.com.ar), para armar el enlace.
     */
    @Transactional
    public void solicitarReset(String email, String baseUrl) {
        String normalizado = email != null ? email.trim().toLowerCase() : "";
        if (normalizado.isEmpty()) return;

        Usuario u = usuarioRepository.findByEmailIgnoreCase(normalizado)
                .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                .orElse(null);
        if (u == null) return;   // no existe / inactivo → salimos en silencio (sin filtrar info)

        String tokenPlano = generarTokenPlano();
        u.setResetToken(hash(tokenPlano));
        u.setResetTokenExpira(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTOS));
        usuarioRepository.save(u);

        String enlace = baseUrl + "/restablecer?token=" + tokenPlano;
        try {
            emailService.enviarHtml(u.getEmail(), "Restablecer tu contraseña — FuturaTecno", emailReset(enlace));
        } catch (Exception e) {
            // No propagamos el error al cliente (evita filtrar existencia del email); queda en el log.
            logger.error("No se pudo enviar el email de reseteo a {}: {}", u.getEmail(), e.toString());
        }
    }

    /**
     * Aplica una contraseña nueva a partir de un token válido y no vencido. El token es de un solo uso:
     * se limpia al usarse.
     */
    @Transactional
    public void resetearPassword(String tokenPlano, String nuevaPassword) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new IllegalArgumentException("Enlace de reseteo inválido.");
        }
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }

        Usuario u = usuarioRepository.findByResetToken(hash(tokenPlano)).orElse(null);
        if (u == null || u.getResetTokenExpira() == null || u.getResetTokenExpira().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de reseteo es inválido o expiró. Pedí uno nuevo.");
        }

        u.setPassword(passwordEncoder.encode(nuevaPassword));
        u.setResetToken(null);           // un solo uso
        u.setResetTokenExpira(null);
        usuarioRepository.save(u);
    }

    private String generarTokenPlano() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: lo que se guarda en la base (nunca el token plano). */
    private String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hashear el token.", e);
        }
    }

    private String emailReset(String enlace) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #16181d;">
              <h2 style="color: #16181d;">Restablecer tu contraseña</h2>
              <p>Recibimos un pedido para restablecer la contraseña de tu cuenta en FuturaTecno.</p>
              <p>Hacé clic en el botón para elegir una nueva contraseña. El enlace vence en 1 hora.</p>
              <p style="text-align: center; margin: 28px 0;">
                <a href="%s" style="background: #C8E048; color: #16181d; text-decoration: none;
                   padding: 12px 24px; border-radius: 8px; font-weight: bold; display: inline-block;">
                  Restablecer contraseña
                </a>
              </p>
              <p style="font-size: 13px; color: #666;">Si no pediste esto, ignorá este email: tu contraseña no cambia.</p>
              <p style="font-size: 12px; color: #999;">Si el botón no funciona, copiá y pegá este enlace:<br>%s</p>
            </div>
            """.formatted(enlace, enlace);
    }
}
