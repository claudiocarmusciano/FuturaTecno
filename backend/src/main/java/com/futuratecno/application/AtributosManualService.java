package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Memoria de categoría y medidas por marca+modelo, hermana de {@link ImagenManualService} y
 * {@link DescripcionManualService}. Existe porque la fila del producto se busca por
 * proveedor+marca+modelo: el mismo artículo cargado con otro proveedor nacía sin categoría ni
 * medidas, y sin categoría no se resuelve el peso para cotizar envío.
 *
 * <p>Acá no se excluye a Elit ni Invid: ningún mayorista trae peso ni dimensiones, y el grueso
 * de la categorización es sobre su catálogo.
 */
@Service
public class AtributosManualService {

    /** Lo recordado para un marca+modelo. Cualquier campo puede venir en null. */
    public record Atributos(Long categoriaId, Integer pesoGramos, Integer altoCm,
                            Integer anchoCm, Integer largoCm) {
        public boolean tieneAlgo() {
            return categoriaId != null || pesoGramos != null || altoCm != null
                    || anchoCm != null || largoCm != null;
        }
    }

    private final JdbcTemplate jdbc;

    public AtributosManualService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    static String normalizar(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public Optional<Atributos> buscar(String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return Optional.empty();
        return jdbc.query("""
                        SELECT categoria_id, peso_gramos, alto_cm, ancho_cm, largo_cm
                        FROM atributos_manuales WHERE marca = ? AND modelo = ?
                        """,
                        (rs, row) -> new Atributos(
                                (Long) rs.getObject("categoria_id"),
                                (Integer) rs.getObject("peso_gramos"),
                                (Integer) rs.getObject("alto_cm"),
                                (Integer) rs.getObject("ancho_cm"),
                                (Integer) rs.getObject("largo_cm")),
                        m, mod)
                .stream().findFirst();
    }

    /**
     * Guarda lo que el producto tenga cargado. Los campos en null no borran lo recordado: que el
     * admin no haya puesto un override de peso no significa que quiera olvidar el que ya había
     * medido para ese modelo. Para eso está {@link #olvidar}.
     */
    public void recordar(Producto p) {
        if (p == null) return;
        String m = normalizar(p.getMarca()), mod = normalizar(p.getModelo());
        if (m.isEmpty() || mod.isEmpty()) return;
        Atributos nuevos = new Atributos(p.getCategoriaId(), p.getPesoGramos(),
                p.getAltoCm(), p.getAnchoCm(), p.getLargoCm());
        if (!nuevos.tieneAlgo()) return;
        jdbc.update("""
                INSERT INTO atributos_manuales (marca, modelo, categoria_id, peso_gramos, alto_cm, ancho_cm, largo_cm)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (marca, modelo) DO UPDATE SET
                    categoria_id = coalesce(EXCLUDED.categoria_id, atributos_manuales.categoria_id),
                    peso_gramos  = coalesce(EXCLUDED.peso_gramos,  atributos_manuales.peso_gramos),
                    alto_cm      = coalesce(EXCLUDED.alto_cm,      atributos_manuales.alto_cm),
                    ancho_cm     = coalesce(EXCLUDED.ancho_cm,     atributos_manuales.ancho_cm),
                    largo_cm     = coalesce(EXCLUDED.largo_cm,     atributos_manuales.largo_cm)
                """, m, mod, nuevos.categoriaId(), nuevos.pesoGramos(),
                nuevos.altoCm(), nuevos.anchoCm(), nuevos.largoCm());
    }

    /** Borra lo recordado para ese marca+modelo. */
    public void olvidar(String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return;
        jdbc.update("DELETE FROM atributos_manuales WHERE marca = ? AND modelo = ?", m, mod);
    }

    /**
     * Completa los huecos del producto con lo recordado. Nunca pisa un valor que el producto ya
     * tenga: lo cargado en esa fila manda sobre la memoria.
     */
    public void aplicar(Producto p) {
        if (p == null) return;
        buscar(p.getMarca(), p.getModelo()).ifPresent(a -> {
            if (p.getCategoriaId() == null) p.setCategoriaId(a.categoriaId());
            if (p.getPesoGramos() == null) p.setPesoGramos(a.pesoGramos());
            if (p.getAltoCm() == null) p.setAltoCm(a.altoCm());
            if (p.getAnchoCm() == null) p.setAnchoCm(a.anchoCm());
            if (p.getLargoCm() == null) p.setLargoCm(a.largoCm());
        });
    }
}
