package com.futuratecno.infrastructure;

import com.futuratecno.domain.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByActivo(Boolean activo);

    Optional<Proveedor> findByNombreIgnoreCase(String nombre);

    Optional<Proveedor> findByCodigoIgnoreCase(String codigo);
}
