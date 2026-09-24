package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * Memoria del margen y el flete puestos a mano en un producto (V41), para que sobrevivan a que su
 * fila se vuelva a crear — por ejemplo si el mayorista le cambia el código interno y el import lo
 * da de alta como producto nuevo.
 *
 * <p><b>Lleva el proveedor en la clave, a diferencia de las otras memorias.</b> La imagen o el peso
 * de un iPhone 15 son los mismos venga de donde venga, pero el margen es una decisión comercial
 * atada a lo que ESE mayorista cobra: aplicarle a un producto de Invid el margen que definiste para
 * el mismo modelo en Elit daría un precio equivocado sin que nadie se entere.
 */
@Service
public class MargenManualService {

    public record Margenes(BigDecimal margenPorcentaje, BigDecimal fletePorcentaje) {
        public boolean tieneAlgo() {
            return margenPorcentaje != null || fletePorcentaje != null;
        }
    }

    private final JdbcTemplate jdbc;

    public MargenManualService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Misma normalización que el resto de las memorias: minúsculas y espacios colapsados. */
    static String normalizar(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public Optional<Margenes> buscar(Long proveedorId, String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (proveedorId == null || m.isEmpty() || mod.isEmpty()) return Optional.empty();
        return jdbc.query("""
                SELECT margen_porcentaje, flete_porcentaje FROM margenes_manuales
                WHERE proveedor_id = ? AND marca = ? AND modelo = ?
                """,
                (rs, row) -> new Margenes(rs.getBigDecimal("margen_porcentaje"), rs.getBigDecimal("flete_porcentaje")),
                proveedorId, m, mod).stream().findFirst();
    }

    /**
     * Guarda lo que el producto tenga cargado. Un valor en null NO borra lo recordado: que el admin
     * no haya puesto override de flete no significa que quiera olvidar el margen que sí definió.
     * Para eso está {@link #olvidar}.
     */
    public void recordar(Producto p) {
        if (p == null || p.getProveedor() == null) return;
        String m = normalizar(p.getMarca()), mod = normalizar(p.getModelo());
        if (m.isEmpty() || mod.isEmpty()) return;
        if (p.getMargenPorcentaje() == null && p.getFletePorcentaje() == null) return;
        jdbc.update("""
                INSERT INTO margenes_manuales (proveedor_id, marca, modelo, margen_porcentaje, flete_porcentaje)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (proveedor_id, marca, modelo) DO UPDATE SET
                    margen_porcentaje = coalesce(EXCLUDED.margen_porcentaje, margenes_manuales.margen_porcentaje),
                    flete_porcentaje  = coalesce(EXCLUDED.flete_porcentaje,  margenes_manuales.flete_porcentaje)
                """, p.getProveedor().getId(), m, mod, p.getMargenPorcentaje(), p.getFletePorcentaje());
    }

    /** Borra lo recordado. Es cómo el admin dice "volvé al margen del proveedor y olvidate". */
    public void olvidar(Long proveedorId, String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (proveedorId == null || m.isEmpty() || mod.isEmpty()) return;
        jdbc.update("DELETE FROM margenes_manuales WHERE proveedor_id = ? AND marca = ? AND modelo = ?",
                proveedorId, m, mod);
    }

    /** Completa los huecos del producto con lo recordado. Nunca pisa un valor ya cargado en la fila. */
    public void aplicar(Producto p) {
        if (p == null) return;
        aplicar(p, java.util.List.of(new NombreArticulo(p.getMarca(), p.getModelo())));
    }

    /**
     * Igual, probando varios nombres del mismo artículo. Sigue siendo por proveedor: el margen está
     * atado a lo que ESE mayorista cobra, aunque el artículo se haya redactado distinto.
     */
    public void aplicar(Producto p, java.util.List<NombreArticulo> nombres) {
        if (p == null || p.getProveedor() == null) return;
        if (p.getMargenPorcentaje() != null && p.getFletePorcentaje() != null) return;
        NombreArticulo.distintos(nombres).stream()
                .map(n -> buscar(p.getProveedor().getId(), n.marca(), n.modelo()))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(m -> {
            if (p.getMargenPorcentaje() == null) p.setMargenPorcentaje(m.margenPorcentaje());
            if (p.getFletePorcentaje() == null) p.setFletePorcentaje(m.fletePorcentaje());
        });
    }
}
