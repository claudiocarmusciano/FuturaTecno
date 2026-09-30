package com.futuratecno.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuración pública que el frontend necesita conocer en runtime.
 *
 * El Google Client ID no es un secreto (viaja en el HTML de cualquier sitio con login de Google),
 * así que exponerlo acá permite tener una única fuente de verdad (la env var GOOGLE_CLIENT_ID del
 * backend) sin tener que rehornear el frontend con un build-arg de Vite. Si está vacío, el frontend
 * simplemente no muestra el botón de Google.
 *
 * <p>Lo mismo con la analítica: el ID de medición de GA4 y el del Pixel de Meta son públicos. Se
 * validan por formato porque el frontend los usa para armar la URL de un script: un valor mal
 * cargado en Railway se descarta (queda sin medir) en lugar de terminar dentro de la página.
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private static final Pattern GA4 = Pattern.compile("G-[A-Z0-9]{4,20}");
    private static final Pattern PIXEL = Pattern.compile("\\d{8,20}");

    private final String googleClientId;
    private final String ga4MeasurementId;
    private final String metaPixelId;

    public ConfigController(@Value("${google.client-id:}") String googleClientId,
                            @Value("${analitica.ga4-measurement-id:}") String ga4MeasurementId,
                            @Value("${analitica.meta-pixel-id:}") String metaPixelId) {
        this.googleClientId = googleClientId == null ? "" : googleClientId;
        this.ga4MeasurementId = valido(GA4, ga4MeasurementId);
        this.metaPixelId = valido(PIXEL, metaPixelId);
    }

    static String valido(Pattern formato, String valor) {
        String v = valor == null ? "" : valor.trim();
        return formato.matcher(v).matches() ? v : "";
    }

    @GetMapping
    public Map<String, String> config() {
        return Map.of("googleClientId", googleClientId,
                "ga4MeasurementId", ga4MeasurementId,
                "metaPixelId", metaPixelId);
    }
}
