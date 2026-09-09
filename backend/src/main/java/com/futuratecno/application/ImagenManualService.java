package com.futuratecno.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Optional;

/** Memoria compartida entre proveedores. No elimina colores, capacidades ni nombres de combos. */
@Service
public class ImagenManualService {
    private final JdbcTemplate jdbc;

    public ImagenManualService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    static String normalizar(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public Optional<String> buscar(String marca, String modelo) {
        return jdbc.query("SELECT url FROM imagenes_manuales WHERE marca = ? AND modelo = ?",
                (rs, row) -> rs.getString("url"), normalizar(marca), normalizar(modelo))
                .stream().findFirst();
    }

    public void guardar(String marca, String modelo, String url) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return;
        if (url == null || url.isBlank()) {
            jdbc.update("DELETE FROM imagenes_manuales WHERE marca = ? AND modelo = ?", m, mod);
        } else {
            jdbc.update("""
                    INSERT INTO imagenes_manuales (marca, modelo, url) VALUES (?, ?, ?)
                    ON CONFLICT (marca, modelo) DO UPDATE SET url = EXCLUDED.url
                    """, m, mod, url.trim());
        }
    }
}
