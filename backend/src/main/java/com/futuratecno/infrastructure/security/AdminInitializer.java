package com.futuratecno.infrastructure.security;

import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea un usuario ADMIN por defecto al arrancar, si todavía no existe ninguno. */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@futuratecno.com}")
    private String adminEmail;

    @Value("${admin.password:admin1234}")
    private String adminPassword;

    public AdminInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.countByRol("ADMIN") > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNombre("Administrador");
        admin.setRol("ADMIN");
        admin.setActivo(true);
        usuarioRepository.save(admin);
        logger.warn("=== Usuario ADMIN creado: {} (contraseña por defecto: '{}'). ¡Cambiala! ===", adminEmail, adminPassword);
    }
}
