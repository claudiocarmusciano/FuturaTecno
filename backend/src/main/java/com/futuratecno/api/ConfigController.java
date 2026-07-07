package com.futuratecno.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Configuración pública que el frontend necesita conocer en runtime.
 *
 * El Google Client ID no es un secreto (viaja en el HTML de cualquier sitio con login de Google),
 * así que exponerlo acá permite tener una única fuente de verdad (la env var GOOGLE_CLIENT_ID del
 * backend) sin tener que rehornear el frontend con un build-arg de Vite. Si está vacío, el frontend
 * simplemente no muestra el botón de Google.
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final String googleClientId;

    public ConfigController(@Value("${google.client-id:}") String googleClientId) {
        this.googleClientId = googleClientId == null ? "" : googleClientId;
    }

    @GetMapping
    public Map<String, String> config() {
        return Map.of("googleClientId", googleClientId);
    }
}
