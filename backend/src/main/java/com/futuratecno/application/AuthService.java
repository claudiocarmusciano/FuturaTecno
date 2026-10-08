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
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RESET_TOKEN_TTL_MINUTOS = 60;   // el enlace de reseteo vale 1 hora
    private static final long ACTIVACION_TOKEN_TTL_MINUTOS = 24 * 60;
    private static final String ADMIN_SIN_GOOGLE =
            "La cuenta de administrador ingresa solo con email y contraseña.";

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
    public AuthResponse registrar(RegisterRequest req, String baseUrl) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String nombre = req.getNombre() != null ? req.getNombre().trim() : "";
        String apellido = req.getApellido() != null ? req.getApellido().trim() : "";
        String celular = normalizarCelularArgentino(req.getCelular());
        LocalDate fechaNacimiento;
        try { fechaNacimiento = LocalDate.parse(req.getFechaNacimiento()); }
        catch (Exception e) { fechaNacimiento = null; }
        if (email.isEmpty() || req.getPassword() == null || req.getPassword().length() < 6
                || nombre.isEmpty() || apellido.isEmpty() || celular == null
                || fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Completá nombre, apellido, fecha de nacimiento, celular argentino, email y contraseña.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email.");
        }
        if (usuarioRepository.existsByCelular(celular)) {
            throw new IllegalArgumentException("Ese número de celular ya está registrado en otra cuenta.");
        }
        if (!emailService.estaConfigurado()) {
            throw new IllegalStateException("La activación por email todavía no está configurada.");
        }

        Usuario u = new Usuario();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCelular(celular);
        u.setFechaNacimiento(fechaNacimiento);
        u.setRol("USUARIO");
        u.setActivo(true);
        String tokenEmail = generarTokenPlano();
        u.setEmailActivacionToken(hash(tokenEmail));
        u.setEmailActivacionExpira(LocalDateTime.now().plusMinutes(ACTIVACION_TOKEN_TTL_MINUTOS));
        usuarioRepository.save(u);

        String enlace = baseUrl + "/activar-cuenta?token=" + tokenEmail;
        emailService.enviarHtmlAsync(email, "Activá tu cuenta — Tecnópolis Olavarría", emailActivacion(enlace));

        String token = jwtService.generarToken(u.getEmail(), u.getRol());
        return new AuthResponse(token, u.getEmail(), u.getNombre(), u.getRol());
    }

    @Transactional
    public AuthResponse activarEmail(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank()) throw new IllegalArgumentException("Enlace de activación inválido.");
        Usuario u = usuarioRepository.findByEmailActivacionToken(hash(tokenPlano)).orElse(null);
        if (u == null || u.getEmailActivacionExpira() == null || u.getEmailActivacionExpira().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de activación es inválido o venció.");
        }
        u.setEmailVerificado(true);
        u.setEmailActivacionToken(null);
        u.setEmailActivacionExpira(null);
        usuarioRepository.save(u);
        String token = jwtService.generarToken(u.getEmail(), u.getRol());
        return new AuthResponse(token, u.getEmail(), u.getNombre(), u.getRol());
    }

    // El Sorteo Bienvenida (31/10/2026) ya no está en la web: lo que sigue solo existe para que el
    // admin termine de validar a quienes se inscribieron antes y se les asigne su código.
    @Transactional
    public void validarWhatsappManual(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(u.getPasoWhatsappAgendado())) {
            throw new IllegalArgumentException("El usuario todavía no confirmó que envió el mensaje por WhatsApp.");
        }
        u.setWhatsappVerificado(true);
        asignarCodigoSorteoSiCorresponde(u);
        usuarioRepository.save(u);
    }

    @Transactional
    public void validarInstagramManual(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(u.getPasoInstagramCompletado())) {
            throw new IllegalArgumentException("El usuario todavía no confirmó el paso de Instagram.");
        }
        u.setInstagramVerificado(true);
        asignarCodigoSorteoSiCorresponde(u);
        usuarioRepository.save(u);
    }

    private void asignarCodigoSorteoSiCorresponde(Usuario u) {
        if (u.getCodigoSorteo() != null || !Boolean.TRUE.equals(u.getEmailVerificado())
                || !Boolean.TRUE.equals(u.getWhatsappVerificado()) || !Boolean.TRUE.equals(u.getInstagramVerificado())) {
            return;
        }
        String codigo;
        do { codigo = generarCodigoSorteo(); } while (usuarioRepository.existsByCodigoSorteo(codigo));
        u.setCodigoSorteo(codigo);
        u.setCodigoSorteoAsignadoEn(LocalDateTime.now());
        emailService.enviarHtmlAsync(u.getEmail(), "Tu código de sorteo — Futura Tecno", emailCodigoSorteo(codigo, u.getChancesSorteo()));
    }

    private String generarCodigoSorteo() {
        final String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder codigo = new StringBuilder("FT26-");
        for (int i = 0; i < 8; i++) codigo.append(caracteres.charAt(RANDOM.nextInt(caracteres.length())));
        return codigo.toString();
    }


    /** Guarda los celulares argentinos en un único formato internacional: +54 9 + área/número. */
    private String normalizarCelularArgentino(String valor) {
        if (valor == null) return null;
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.startsWith("549")) {
            digitos = digitos.substring(3);
        } else if (digitos.startsWith("54")) {
            digitos = digitos.substring(2);
            if (digitos.startsWith("9")) digitos = digitos.substring(1);
        } else if (digitos.startsWith("0")) {
            digitos = digitos.substring(1);
        }
        return digitos.matches("[1-9][0-9]{9}") ? "+549" + digitos : null;
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
     *
     * Reglas de la vinculación, porque es una toma de cuenta si sale mal:
     *  - La cuenta ADMIN nunca entra por Google, ni vinculada ni por vincular. Un vínculo no se
     *    corta al cambiar la contraseña, así que quien lograra vincularse conservaría el admin.
     *  - Nunca se pisa un Google ya vinculado a la cuenta.
     *  - Si el email de la cuenta no estaba verificado, la contraseña la pudo haber elegido
     *    cualquiera (el registro no exige verificar): se borra al vincular, y Google, que sí
     *    probó que la persona es dueña del email, deja el email como verificado.
     */
    @Transactional
    public AuthResponse loginConGoogle(String credential) {
        GoogleTokenVerifier.GoogleUser g = googleTokenVerifier.verificar(credential);

        Usuario u = usuarioRepository.findByGoogleSub(g.sub()).orElse(null);
        if (u == null) {
            u = usuarioRepository.findByEmailIgnoreCase(g.email()).orElse(null);
            if (u != null) {
                if (esAdmin(u)) throw new IllegalArgumentException(ADMIN_SIN_GOOGLE);
                if (u.getGoogleSub() != null && !u.getGoogleSub().isBlank()) {
                    throw new IllegalArgumentException(
                            "Esta cuenta ya está vinculada a otra cuenta de Google. Ingresá con esa o con tu contraseña.");
                }
                // Cuenta existente con ese email → la vinculamos a Google.
                if (!Boolean.TRUE.equals(u.getEmailVerificado())) {
                    u.setPassword(null);
                    u.setEmailVerificado(true);
                }
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
        } else if (esAdmin(u)) {
            throw new IllegalArgumentException(ADMIN_SIN_GOOGLE);
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
        // Asíncrono a propósito: la respuesta al cliente es siempre la misma (no filtra si el email
        // existe), así que no tiene sentido hacerlo esperar a que responda el proveedor de mail.
        // Los errores quedan en el log dentro de EmailService.
        emailService.enviarHtmlAsync(u.getEmail(), "Restablecer tu contraseña — Tecnópolis Olavarría", emailReset(enlace));
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

    private static boolean esAdmin(Usuario u) {
        return "ADMIN".equalsIgnoreCase(u.getRol());
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
              <p>Recibimos un pedido para restablecer la contraseña de tu cuenta en Tecnópolis Olavarría.</p>
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

    private String emailActivacion(String enlace) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #16181d;">
              <h2>¡Bienvenido a Tecnópolis Olavarría!</h2>
              <p>Para terminar tu registro, confirmá que este email es tuyo.</p>
              <p style="text-align: center; margin: 28px 0;"><a href="%s" style="background: #C8E048; color: #16181d; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: bold; display: inline-block;">Activar cuenta</a></p>
              <p style="font-size: 13px; color: #666;">El enlace vence en 24 horas.</p>
            </div>
            """.formatted(enlace);
    }

    private String emailCodigoSorteo(String codigo, Integer chancesSorteo) {
        boolean dobleChance = chancesSorteo != null && chancesSorteo > 1;
        String beneficio = dobleChance
                ? "<p style=\"color: #5D6B14; font-weight: bold;\">Como te registraste hasta el 31/08/2026, tenés doble chance: tu participación se incluirá dos veces en el sorteo.</p>"
                : "";
        return """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #16181d;">
              <h2 style="color: #16181d;">¡Tu inscripción fue validada!</h2>
              <p>Ya cumpliste los requisitos del Sorteo Bienvenida de Futura Tecno.</p>
              <p>Tu código único de sorteo es:</p>
              <p style="margin: 24px 0; padding: 16px; text-align: center; background: #16181d; border-radius: 10px; color: #C8E048; font-size: 24px; font-weight: bold; letter-spacing: 2px;">%s</p>
              %s
              <p>Guardalo: será incluido en el padrón público anonimizado antes del sorteo.</p>
              <p style="font-size: 13px; color: #666;">El sorteo se realizará al alcanzar 1.000 seguidores en Instagram o, como máximo, el 31/10/2026.</p>
            </div>
            """.formatted(codigo, beneficio);
    }
}
