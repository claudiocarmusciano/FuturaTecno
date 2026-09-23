package com.futuratecno.api;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.ForgotPasswordRequest;
import com.futuratecno.api.dto.GoogleLoginRequest;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.api.dto.RegisterRequest;
import com.futuratecno.api.dto.ResetPasswordRequest;
import org.springframework.security.core.Authentication;
import com.futuratecno.application.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    /**
     * Origen de los enlaces que viajan por mail (activación y reseteo). Sale de la configuración y
     * NO del request: con forward-headers-strategy=framework el host del request lo puede elegir
     * el cliente (X-Forwarded-Host / Forwarded), y un enlace de reseteo apuntando a otro dominio
     * le entregaría el token a un tercero.
     */
    private final String baseUrl;

    public AuthController(AuthService authService, @Value("${app.public-url}") String publicUrl) {
        this.authService = authService;
        this.baseUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody RegisterRequest req) {
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
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody GoogleLoginRequest req) {
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
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        authService.solicitarReset(req.getEmail(), baseUrl);
        // Respuesta genérica SIEMPRE (exista o no el email): no filtramos qué cuentas están registradas.
        return ResponseEntity.ok(Map.of("mensaje",
                "Si el email está registrado, te enviamos un enlace para restablecer la contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        try {
            authService.resetearPassword(req.getToken(), req.getPassword());
            return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña se actualizó. Ya podés iniciar sesión."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
