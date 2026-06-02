package com.futuratecno.infrastructure;

import com.futuratecno.domain.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Long> {
    List<Imagen> findByVarianteIdOrderByOrden(Long varianteId);
}
