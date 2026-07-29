package com.futuratecno.infrastructure;

import com.futuratecno.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Subcategorías colgando de una categoría. Se usa para no borrar una que tenga hijos. */
    long countByPadreId(Long padreId);
}
