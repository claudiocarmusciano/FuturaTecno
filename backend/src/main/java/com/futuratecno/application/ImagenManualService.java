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

    /**
     * Imagen conocida para esa marca+modelo, de la fuente más confiable a la menos.
     *
     * <p>La tercera fuente es el catálogo en vivo, y es la que más ahorra: las dos tablas de
     * memoria solo se escriben cuando el admin edita una imagen, cuando el generador encuentra
     * una buscando, o cuando se da de baja un producto. Un artículo que entró con foto desde Elit,
     * Invid o una carga JSON **no** queda registrado ahí, así que el generador salía a buscar —y a
     * gastar crédito— por una imagen que ya estaba en la base. Al 2026-09-14 eso eran 3.215
     * productos, el 98% del catálogo.
     *
     * <p>Entre productos gana el activo: si el mismo marca+modelo existe publicado y dado de baja,
     * la foto del publicado es la que el cliente está viendo hoy.
     */
    public Optional<String> buscar(String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return Optional.empty();
        return jdbc.query("""
                SELECT url FROM (
                    SELECT url, 0 prioridad FROM imagenes_manuales WHERE marca = ? AND modelo = ?
                    UNION ALL
                    SELECT url, 1 prioridad FROM imagenes_automaticas WHERE marca = ? AND modelo = ?
                    UNION ALL
                    SELECT imagen_url AS url, CASE WHEN activo THEN 2 ELSE 3 END AS prioridad
                    FROM productos
                    WHERE lower(regexp_replace(trim(marca), '\\s+', ' ', 'g')) = ?
                      AND lower(regexp_replace(trim(modelo), '\\s+', ' ', 'g')) = ?
                      AND nullif(trim(imagen_url), '') IS NOT NULL
                ) memoria ORDER BY prioridad LIMIT 1
                """,
                (rs, row) -> rs.getString("url"), m, mod, m, mod, m, mod)
                .stream().findFirst();
    }

    public void guardarAutomatica(String marca, String modelo, String url) {
        if (normalizar(marca).isEmpty() || normalizar(modelo).isEmpty() || url == null || url.isBlank()) return;
        jdbc.update("""
                INSERT INTO imagenes_automaticas (marca, modelo, url) VALUES (?, ?, ?)
                ON CONFLICT (marca, modelo) DO NOTHING
                """, normalizar(marca), normalizar(modelo), url.trim());
    }

    public void guardar(String marca, String modelo, String url) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return;
        if (url == null || url.isBlank()) {
            jdbc.update("DELETE FROM imagenes_automaticas WHERE marca = ? AND modelo = ?", m, mod);
            jdbc.update("DELETE FROM imagenes_manuales WHERE marca = ? AND modelo = ?", m, mod);
        } else {
            jdbc.update("""
                    INSERT INTO imagenes_manuales (marca, modelo, url) VALUES (?, ?, ?)
                    ON CONFLICT (marca, modelo) DO UPDATE SET url = EXCLUDED.url
                    """, m, mod, url.trim());
        }
    }
}
