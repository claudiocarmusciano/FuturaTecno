package com.futuratecno.api;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.ForgotPasswordRequest;
import com.futuratecno.api.dto.GoogleLoginRequest;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.api.dto.RegisterRequest;
import com.futuratecno.api.dto.ResetPasswordRequest;
import org.springframework.security.core.Authentication;
import com.futuratecno.application.AuthService;
import com.futuratecno.application.LimitadorIntentos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final LimitadorIntentos limitador;

    // Topes (ver LimitadorIntentos: lo que protege una cuenta es el límite por email).
    static final int LOGIN_FALLOS_POR_EMAIL = 5;
    static final Duration LOGIN_BLOQUEO = Duration.ofMinutes(15);
    private static final int LOGIN_POR_IP = 20;
    private static final Duration VENTANA_CORTA = Duration.ofMinutes(10);
    private static final Duration HORA = Duration.ofHours(1);
    private static final String DEMASIADOS = "Demasiados intentos. Esperá unos minutos y probá de nuevo.";
    /**
     * Origen de los enlaces que viajan por mail (activación y reseteo). Sale de la configuración y
     * NO del request: con forward-headers-strategy=framework el host del request lo puede elegir
     * el cliente (X-Forwarded-Host / Forwarded), y un enlace de reseteo apuntando a otro dominio
     * le entregaría el token a un tercero.
     */
    private final String baseUrl;

    public AuthController(AuthService authService, LimitadorIntentos limitador,
                          @Value("${app.public-url}") String publicUrl) {
        this.authService = authService;
        this.limitador = limitador;
        this.baseUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody RegisterRequest req, HttpServletRequest http) {
        if (!limitador.permitir("registro-ip:" + http.getRemoteAddr(), 5, HORA)) return demasiados();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req, baseUrl));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/activar-cuenta")
    public ResponseEntity<?> activarCuenta(@RequestParam String token) {
        try {
            return ResponseEntity.ok(authService.activarEmail(token));
        } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/onboarding")
    public ResponseEntity<?> onboarding(Authentication auth) {
        return ResponseEntity.ok(authService.estadoOnboarding(auth.getName()));
    }

    @PostMapping("/onboarding/paso/{paso}")
    public ResponseEntity<?> completarPaso(Authentication auth, @PathVariable int paso) {
        try { return ResponseEntity.ok(authService.completarPaso(auth.getName(), paso)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest http) {
        String claveEmail = "login-email:" + normalizar(req.getEmail());
        // Se chequea ANTES de probar la contraseña: bloqueada la cuenta, ni siquiera se compara.
        if (limitador.agotado(claveEmail, LOGIN_FALLOS_POR_EMAIL, LOGIN_BLOQUEO)
                || !limitador.permitir("login-ip:" + http.getRemoteAddr(), LOGIN_POR_IP, VENTANA_CORTA)) {
            return demasiados();
        }
        try {
            AuthResponse r = authService.login(req);
            limitador.reiniciar(claveEmail);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            limitador.anotar(claveEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody GoogleLoginRequest req, HttpServletRequest http) {
        if (!limitador.permitir("google-ip:" + http.getRemoteAddr(), LOGIN_POR_IP, VENTANA_CORTA)) return demasiados();
        try {
            return ResponseEntity.ok(authService.loginConGoogle(req.getCredential()));
        } catch (IllegalStateException e) {
            // Login con Google no configurado en el servidor (falta GOOGLE_CLIENT_ID).
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req, HttpServletRequest http) {
        if (!limitador.permitir("reset-ip:" + http.getRemoteAddr(), 5, HORA)) return demasiados();
        // Pasado el tope por email no se manda nada, pero la respuesta es la misma de siempre:
        // así no sirve para mandar mails en masa ni para saber qué cuentas existen.
        if (limitador.permitir("reset-email:" + normalizar(req.getEmail()), 3, HORA)) {
            authService.solicitarReset(req.getEmail(), baseUrl);
        }
        // Respuesta genérica SIEMPRE (exista o no el email): no filtramos qué cuentas están registradas.
        return ResponseEntity.ok(Map.of("mensaje",
                "Si el email está registrado, te enviamos un enlace para restablecer la contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req, HttpServletRequest http) {
        if (!limitador.permitir("nueva-clave-ip:" + http.getRemoteAddr(), 10, HORA)) return demasiados();
        try {
            authService.resetearPassword(req.getToken(), req.getPassword());
            return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña se actualizó. Ya podés iniciar sesión."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseEntity<Map<String, String>> demasiados() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", DEMASIADOS));
    }
}
