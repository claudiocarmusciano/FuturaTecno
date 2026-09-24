package com.futuratecno.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Memoria de descripciones escritas a mano, espejo de {@link ImagenManualService}. La "descripción"
 * de un producto es {@code Variante.especificaciones}; se recuerda por marca+modelo para que
 * sobreviva a un reimport y se comparta entre proveedores.
 *
 * <p>No guarda las de Elit ni Invid: esos mayoristas traen su propia ficha en cada sincronización,
 * así que recordarla no aporta y taparía el texto propio.
 */
@Service
public class DescripcionManualService {

    private static final Set<String> FUENTES_MAYORISTAS = Set.of("ELIT", "INVID");

    private final JdbcTemplate jdbc;

    public DescripcionManualService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Misma normalización que las imágenes: conserva color, capacidad y nombre de combo. */
    static String normalizar(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static boolean esDeMayorista(String fuente) {
        return fuente != null && FUENTES_MAYORISTAS.contains(fuente.strip().toUpperCase(Locale.ROOT));
    }

    public Optional<String> buscar(String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return Optional.empty();
        return jdbc.query("SELECT descripcion FROM descripciones_manuales WHERE marca = ? AND modelo = ?",
                        (rs, row) -> rs.getString("descripcion"), m, mod)
                .stream().findFirst();
    }

    /** La descripción manual de cualquiera de esos nombres, en orden (ver {@link NombreArticulo}). */
    public Optional<String> buscar(java.util.List<NombreArticulo> nombres) {
        for (NombreArticulo n : NombreArticulo.distintos(nombres)) {
            Optional<String> d = buscar(n.marca(), n.modelo());
            if (d.isPresent()) return d;
        }
        return Optional.empty();
    }

    /**
     * Guarda la descripción para esa marca+modelo. Una descripción vacía borra la memoria: es cómo
     * el admin dice "olvidate de la que había", igual que vaciar el campo de imagen.
     */
    public void guardar(String marca, String modelo, String descripcion) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return;
        if (descripcion == null || descripcion.isBlank()) {
            jdbc.update("DELETE FROM descripciones_manuales WHERE marca = ? AND modelo = ?", m, mod);
        } else {
            jdbc.update("""
                    INSERT INTO descripciones_manuales (marca, modelo, descripcion) VALUES (?, ?, ?)
                    ON CONFLICT (marca, modelo) DO UPDATE SET descripcion = EXCLUDED.descripcion
                    """, m, mod, descripcion.strip());
        }
    }
}
