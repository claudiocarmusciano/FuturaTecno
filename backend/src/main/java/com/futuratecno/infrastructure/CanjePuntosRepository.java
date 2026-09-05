package com.futuratecno.infrastructure;

import com.futuratecno.domain.CanjePuntos;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanjePuntosRepository extends JpaRepository<CanjePuntos, Long> {
    Optional<CanjePuntos> findByPedidoId(Long pedidoId);
    List<CanjePuntos> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
}
