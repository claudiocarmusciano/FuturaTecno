package com.futuratecno.api;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.ForgotPasswordRequest;
import com.futuratecno.api.dto.GoogleLoginRequest;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.api.dto.RegisterRequest;
import com.futuratecno.api.dto.ResetPasswordRequest;
import com.futuratecno.application.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
        // El origen del sitio (ej. https://futuratecno.com.ar) sale del propio request → el enlace
        // se arma con el host que el usuario está usando (localhost, Railway o dominio propio).
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
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
