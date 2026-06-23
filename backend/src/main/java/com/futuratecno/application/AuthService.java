package com.futuratecno.application;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.api.dto.RegisterRequest;
import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.UsuarioRepository;
import com.futuratecno.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
}
