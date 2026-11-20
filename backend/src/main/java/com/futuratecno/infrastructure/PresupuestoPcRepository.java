package com.futuratecno.infrastructure;

import com.futuratecno.domain.PresupuestoPc;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresupuestoPcRepository extends JpaRepository<PresupuestoPc, Long> {
    List<PresupuestoPc> findByUsuarioIdOrderByUpdatedAtDesc(Long usuarioId);
    Optional<PresupuestoPc> findByIdAndUsuarioId(Long id, Long usuarioId);
    long countByUsuarioId(Long usuarioId);
    @EntityGraph(attributePaths = "usuario")
    List<PresupuestoPc> findTop500ByOrderByUpdatedAtDesc();
}
