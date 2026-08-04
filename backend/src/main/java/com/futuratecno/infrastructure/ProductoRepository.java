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

    Optional<Producto> findByProveedorIdAndMarcaAndModelo(Long proveedorId, String marca, String modelo);

    Optional<Producto> findByProveedorIdAndCodigoExterno(Long proveedorId, String codigoExterno);

    /** Cuántos productos quedarían sin categoría si se borrara esa categoría. */
    long countByCategoriaId(Long categoriaId);

    /** Productos activos de un proveedor, para mostrarlo en el panel sin traerlos todos. */
    long countByProveedorIdAndActivo(Long proveedorId, Boolean activo);

    /** Baja lógica en una sola query para la selección masiva del panel admin. */
    @Modifying
    @Query("update Producto p set p.activo = false, p.updatedAt = CURRENT_TIMESTAMP where p.id in :ids and p.activo = true")
    int desactivarPorIds(@Param("ids") List<Long> ids);
}
