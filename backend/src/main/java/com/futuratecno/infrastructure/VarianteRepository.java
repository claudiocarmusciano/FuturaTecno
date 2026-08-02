package com.futuratecno.infrastructure;

import com.futuratecno.domain.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VarianteRepository extends JpaRepository<Variante, Long> {
    List<Variante> findByProductoIdAndActivo(Long productoId, Boolean activo);

    /**
     * Variantes de varios productos en una sola consulta. La usan el catálogo y el admin para no
     * pagar una query por producto al listar (con miles de productos, eso vuelve el listado
     * lentísimo — ver CatalogoService/ProductoAdminService).
     */
    List<Variante> findByProductoIdInAndActivo(List<Long> productoIds, Boolean activo);

    List<Variante> findByProductoId(Long productoId);

    Optional<Variante> findByProductoIdAndEspecificaciones(Long productoId, String especificaciones);
}
