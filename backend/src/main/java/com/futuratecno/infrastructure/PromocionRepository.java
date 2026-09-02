package com.futuratecno.infrastructure;

import com.futuratecno.domain.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    List<Promocion> findAllByOrderByOrdenAscIdAsc();
    long countByActivoTrue();

    @Query(value = """
            SELECT id, titulo, texto, enlace, orden, fecha_inicio AS "fechaInicio",
                   fecha_fin AS "fechaFin", activo, (imagen_movil IS NOT NULL) AS "tieneImagenMovil",
                   updated_at AS "updatedAt"
            FROM promociones ORDER BY orden, id
            """, nativeQuery = true)
    List<PromocionResumen> listarResumen();

    interface PromocionResumen {
        Long getId(); String getTitulo(); String getTexto(); String getEnlace(); Integer getOrden();
        LocalDateTime getFechaInicio(); LocalDateTime getFechaFin(); Boolean getActivo();
        Boolean getTieneImagenMovil(); LocalDateTime getUpdatedAt();
    }
}
