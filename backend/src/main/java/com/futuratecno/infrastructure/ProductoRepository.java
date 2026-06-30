package com.futuratecno.infrastructure;

import com.futuratecno.domain.Producto;
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
}
