package com.futuratecno.infrastructure;

import com.futuratecno.domain.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VarianteRepository extends JpaRepository<Variante, Long> {
    List<Variante> findByProductoIdAndActivo(Long productoId, Boolean activo);
}
