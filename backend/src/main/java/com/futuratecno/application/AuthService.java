package com.futuratecno.application;

import com.futuratecno.api.dto.AuthResponse;
import com.futuratecno.api.dto.LoginRequest;
import com.futuratecno.api.dto.RegisterRequest;
import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.UsuarioRepository;
import com.futuratecno.infrastructure.security.GoogleTokenVerifier;
import com.futuratecno.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, GoogleTokenVerifier googleTokenVerifier) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
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
}
