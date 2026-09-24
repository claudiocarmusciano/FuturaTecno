package com.futuratecno.infrastructure;

import com.futuratecno.domain.Producto;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByProveedorIdAndActivo(Long proveedorId, Boolean activo);

    List<Producto> findByActivo(Boolean activo);

    List<Producto> findByActivoAndFuenteIn(Boolean activo, java.util.Collection<String> fuentes);

    Optional<Producto> findByProveedorIdAndMarcaAndModelo(Long proveedorId, String marca, String modelo);

    Optional<Producto> findByProveedorIdAndCodigoExterno(Long proveedorId, String codigoExterno);

    /**
     * Mismo producto del mismo proveedor, aunque la IA haya redactado el modelo distinto en esta
     * carga. Compara la columna generada `clave_suelta` (V37), que ignora espacios, puntuación y
     * mayúsculas pero conserva capacidad, color y versión. Devuelve el id y no la entidad porque
     * `clave_suelta` no está mapeada en `Producto` y no tiene por qué estarlo: la calcula la base.
     *
     * <p>Se prefiere el activo, y a igualdad el más viejo: es el que viene acumulando la categoría,
     * las medidas y la imagen que se cargaron a mano.
     */
    @Query(value = """
            SELECT id FROM productos
            WHERE proveedor_id = :proveedorId AND clave_suelta = :clave
            ORDER BY activo DESC, id ASC LIMIT 1
            """, nativeQuery = true)
    Optional<Long> idPorClaveSuelta(@Param("proveedorId") Long proveedorId, @Param("clave") String clave);

    /**
     * Candidatos a ser el mismo artículo en una carga por JSON, del mismo proveedor y activos o
     * no: por identidad resuelta (V42), por familia, por clave suelta (V37) o por nombre exacto.
     * Devuelve una lista y no un Optional a propósito: con duplicados históricos, un Optional
     * lanza una excepción y tira la carga entera; así se ven y se mandan a revisión.
     */
    @Query(value = """
            SELECT * FROM productos
            WHERE proveedor_id = :proveedorId
              AND (identidad_clave = :identidad
                   OR identidad_familia = :familia
                   OR clave_suelta = :claveSuelta
                   OR (lower(regexp_replace(trim(marca), '\\s+', ' ', 'g')) = :marca
                       AND lower(regexp_replace(trim(modelo), '\\s+', ' ', 'g')) = :modelo))
            ORDER BY id
            """, nativeQuery = true)
    List<Producto> candidatosDeIdentidad(@Param("proveedorId") Long proveedorId,
                                         @Param("identidad") String identidad,
                                         @Param("familia") String familia,
                                         @Param("claveSuelta") String claveSuelta,
                                         @Param("marca") String marcaNormalizada,
                                         @Param("modelo") String modeloNormalizado);

    /**
     * Todos los productos de esas marcas en el proveedor, para los teléfonos: un producto viejo
     * sin identidad asignada solo se reconoce resolviéndolo, porque su nombre puede no compartir
     * ni la clave suelta ("G04 4G 64GB / 4GB RAM" vs. "G04 4G 64GB").
     */
    @Query(value = """
            SELECT * FROM productos
            WHERE proveedor_id = :proveedorId
              AND lower(regexp_replace(trim(marca), '\\s+', ' ', 'g')) IN (:marcas)
            ORDER BY id
            """, nativeQuery = true)
    List<Producto> deMarcasEnProveedor(@Param("proveedorId") Long proveedorId,
                                       @Param("marcas") java.util.Collection<String> marcasNormalizadas);

    /** Otros nombres con los que se cargó el mismo artículo, en cualquier proveedor (memorias). */
    @Query(value = """
            SELECT DISTINCT marca, modelo FROM productos
            WHERE identidad_clave = :identidad
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> nombresPorIdentidad(@Param("identidad") String identidad);

    /** Cuántos productos quedarían sin categoría si se borrara esa categoría. */
    long countByCategoriaId(Long categoriaId);

    /** Productos activos de un proveedor, para mostrarlo en el panel sin traerlos todos. */
    long countByProveedorIdAndActivo(Long proveedorId, Boolean activo);

    /**
     * Marca los productos que el mayorista sigue teniendo en su feed (V40). Se hace con un UPDATE
     * masivo en vez de tocar cada entidad **a propósito**: un `save()` por producto dispararía
     * `@PreUpdate` y pisaría `updated_at`, que es el único dato que indica cuándo cambió el precio
     * de verdad — el catálogo pasaría a decir "Actualizado: hoy" en todo. Un UPDATE JPQL no pasa
     * por el ciclo de vida de la entidad, así que solo escribe esta columna.
     */
    @Modifying
    @Query("update Producto p set p.vistoEnSyncAt = :ahora where p.id in :ids")
    int marcarVistosEnSync(@Param("ids") List<Long> ids, @Param("ahora") java.time.LocalDateTime ahora);

    /**
     * Igual que el anterior pero en tandas, porque un `IN` con 1.400 ids es innecesariamente
     * grande. Todas las tandas comparten la misma hora: una sincronización es un solo momento.
     * Necesita una transacción abierta — la ponen los métodos públicos de los ImportService.
     */
    default int marcarVistos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        int marcados = 0;
        for (int desde = 0; desde < ids.size(); desde += 500) {
            marcados += marcarVistosEnSync(ids.subList(desde, Math.min(desde + 500, ids.size())), ahora);
        }
        return marcados;
    }

    /** Baja lógica en una sola query para la selección masiva del panel admin. */
    @Modifying
    @Query("update Producto p set p.activo = false, p.updatedAt = CURRENT_TIMESTAMP where p.id in :ids and p.activo = true")
    int desactivarPorIds(@Param("ids") List<Long> ids);
}
