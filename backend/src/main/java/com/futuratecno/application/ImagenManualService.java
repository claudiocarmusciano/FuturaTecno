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
     * Clave tolerante a la redacción: minúsculas y solo letras y números. Absorbe las diferencias
     * con las que la IA escribe el mismo artículo en cada carga ("256GB" / "256 GB" / "(eSIM)")
     * sin borrar las que distinguen de verdad: 256gb ≠ 512gb, black ≠ white.
     *
     * <p>Replica exactamente la expresión de la columna generada `productos.clave_suelta` (V37).
     * Si se cambia una hay que cambiar la otra o las búsquedas dejan de encontrar en silencio.
     */
    static String claveSuelta(String texto) {
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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
                .stream().findFirst()
                .or(() -> buscarSuelto(marca, modelo));
    }

    /**
     * Último intento antes de salir a pagar una búsqueda: el mismo artículo cargado ayer, con el
     * modelo redactado apenas distinto. Solo mira el catálogo, que es donde están las imágenes
     * (98% de los productos tiene foto) y lo único indexado por clave suelta.
     *
     * <p>Se prueba también con la marca sacada del modelo, porque la IA a veces la repite:
     * "Apple" + "Apple Pencil Pro" y "Apple" + "Pencil Pro" son el mismo lápiz.
     *
     * <p>El riesgo de aflojar la clave es traer la imagen de otro artículo parecido. Se acepta
     * porque acá lo único que se decide es QUÉ FOTO mostrar —corregible desde Admin → Imágenes—,
     * nunca si dos productos son el mismo ni qué precio tienen.
     */
    private Optional<String> buscarSuelto(String marca, String modelo) {
        String clave = clave(marca, modelo);
        if (claveSuelta(marca).isEmpty() || claveSuelta(modelo).isEmpty()) return Optional.empty();
        return jdbc.query("""
                SELECT imagen_url FROM productos
                WHERE clave_suelta = ?
                  AND nullif(trim(imagen_url), '') IS NOT NULL
                ORDER BY activo DESC, id DESC LIMIT 1
                """, (rs, row) -> rs.getString("imagen_url"), clave)
                .stream().findFirst();
    }

    /**
     * Misma clave que calcula la columna generada `productos.clave_suelta` (V37). La marca se saca
     * del modelo si viene repetida ("Apple" + "Apple Pencil Pro"), y se hace de los dos lados para
     * que dé igual cuál redacción esté guardada y cuál esté entrando.
     */
    static String clave(String marca, String modelo) {
        String m = claveSuelta(marca), mod = claveSuelta(modelo);
        return m + (mod.startsWith(m) ? mod.substring(m.length()) : mod);
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
