package com.futuratecno.infrastructure;

import com.futuratecno.domain.EstadoPuntoCredito;
import com.futuratecno.domain.PuntoCredito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuntoCreditoRepository extends JpaRepository<PuntoCredito, Long> {
    Optional<PuntoCredito> findByPedidoId(Long pedidoId);
    List<PuntoCredito> findByUsuarioIdOrderByAcreditadoEnDesc(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PuntoCredito c where c.usuario.id = :usuarioId and c.estado = :estado " +
            "and c.puntosDisponibles > 0 and c.venceEn > :ahora order by c.venceEn asc, c.id asc")
    List<PuntoCredito> disponiblesParaCanje(@Param("usuarioId") Long usuarioId,
                                            @Param("estado") EstadoPuntoCredito estado,
                                            @Param("ahora") LocalDateTime ahora);

    List<PuntoCredito> findByEstadoAndVenceEnBefore(EstadoPuntoCredito estado, LocalDateTime ahora);
}
