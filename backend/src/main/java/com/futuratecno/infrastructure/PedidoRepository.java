package com.futuratecno.infrastructure;

import com.futuratecno.domain.EstadoPedido;
import com.futuratecno.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    List<Pedido> findByEstadoOrderByCreatedAtDesc(EstadoPedido estado);

    List<Pedido> findAllByOrderByCreatedAtDesc();

    Optional<Pedido> findByNumero(String numero);

    /** Pedidos que quedaron sin cerrar pasado su corte de las 06:30. Los marca VENCIDO el scheduler. */
    List<Pedido> findByEstadoAndVenceEnBefore(EstadoPedido estado, LocalDateTime momento);

    long countByEstado(EstadoPedido estado);

    /**
     * Número correlativo del pedido. Sale de una secuencia dedicada (V13) en vez del id para que
     * el número sea legible y estable aunque después se borren filas.
     */
    @Query(value = "SELECT nextval('pedidos_numero_seq')", nativeQuery = true)
    Long siguienteNumero();
}
