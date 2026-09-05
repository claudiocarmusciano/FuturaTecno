package com.futuratecno.application;

import com.futuratecno.api.dto.MisPuntosDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.CanjePuntosRepository;
import com.futuratecno.infrastructure.PuntoCreditoRepository;
import com.futuratecno.infrastructure.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas del programa: 1 punto cada US$100, 1 punto = US$1, vigencia de 12 meses. */
@Service
public class PuntosService {
    private final PuntoCreditoRepository creditoRepository;
    private final CanjePuntosRepository canjeRepository;
    private final UsuarioRepository usuarioRepository;

    public PuntosService(PuntoCreditoRepository creditoRepository, CanjePuntosRepository canjeRepository,
                         UsuarioRepository usuarioRepository) {
        this.creditoRepository = creditoRepository;
        this.canjeRepository = canjeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public MisPuntosDTO misPuntos(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        LocalDateTime ahora = LocalDateTime.now();
        List<PuntoCredito> creditos = creditoRepository.findByUsuarioIdOrderByAcreditadoEnDesc(usuario.getId());
        MisPuntosDTO dto = new MisPuntosDTO();
        int saldo = creditos.stream().filter(c -> c.getEstado() == EstadoPuntoCredito.VIGENTE
                        && c.getVenceEn().isAfter(ahora))
                .mapToInt(c -> c.getPuntosDisponibles() == null ? 0 : c.getPuntosDisponibles()).sum();
        dto.setPuntosDisponibles(saldo);
        creditos.stream().filter(c -> c.getEstado() == EstadoPuntoCredito.VIGENTE && c.getPuntosDisponibles() > 0
                        && c.getVenceEn().isAfter(ahora))
                .map(PuntoCredito::getVenceEn).min(LocalDateTime::compareTo).ifPresent(dto::setProximoVencimiento);

        List<MisPuntosDTO.Movimiento> movimientos = new ArrayList<>();
        for (PuntoCredito c : creditos) {
            MisPuntosDTO.Movimiento m = new MisPuntosDTO.Movimiento();
            m.setTipo("ACREDITACION"); m.setPuntos(c.getPuntosOtorgados());
            m.setDetalle("Puntos por pedido " + c.getPedido().getNumero());
            m.setFecha(c.getAcreditadoEn()); m.setVenceEn(c.getVenceEn()); movimientos.add(m);
        }
        for (CanjePuntos c : canjeRepository.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId())) {
            if (c.getEstado() == EstadoCanjePuntos.REVERTIDO) continue;
            MisPuntosDTO.Movimiento m = new MisPuntosDTO.Movimiento();
            m.setTipo(c.getEstado() == EstadoCanjePuntos.APLICADO ? "CANJE" : "RESERVA");
            m.setPuntos(-c.getPuntos()); m.setDetalle("Canje en pedido " + c.getPedido().getNumero());
            m.setFecha(c.getCreatedAt()); movimientos.add(m);
        }
        movimientos.sort(Comparator.comparing(MisPuntosDTO.Movimiento::getFecha).reversed());
        dto.setMovimientos(movimientos);
        return dto;
    }

    /** Reserva en FIFO los puntos solicitados; el pedido aún puede vencer o quedar impago. */
    @Transactional
    public void reservar(Pedido pedido, int puntos) {
        if (puntos <= 0) return;
        if (canjeRepository.findByPedidoId(pedido.getId()).isPresent()) {
            throw new IllegalArgumentException("El pedido ya tiene un canje de puntos.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<PuntoCredito> creditos = creditoRepository.disponiblesParaCanje(pedido.getUsuario().getId(), EstadoPuntoCredito.VIGENTE, ahora);
        int disponibles = creditos.stream().mapToInt(PuntoCredito::getPuntosDisponibles).sum();
        if (puntos > disponibles) throw new IllegalArgumentException("No tenés suficientes puntos disponibles.");

        CanjePuntos canje = new CanjePuntos();
        canje.setUsuario(pedido.getUsuario()); canje.setPedido(pedido); canje.setPuntos(puntos);
        int pendiente = puntos;
        for (PuntoCredito credito : creditos) {
            if (pendiente == 0) break;
            int usados = Math.min(pendiente, credito.getPuntosDisponibles());
            credito.setPuntosDisponibles(credito.getPuntosDisponibles() - usados);
            CanjePuntoItem item = new CanjePuntoItem();
            item.setCanje(canje); item.setCredito(credito); item.setPuntos(usados);
            canje.getItems().add(item); pendiente -= usados;
        }
        canjeRepository.save(canje);
    }

    /** Aplica el canje y acredita la compra, siempre de manera idempotente. */
    @Transactional
    public void procesarPagoAprobado(Pedido pedido) {
        canjeRepository.findByPedidoId(pedido.getId()).ifPresent(c -> {
            if (c.getEstado() == EstadoCanjePuntos.RESERVADO) c.setEstado(EstadoCanjePuntos.APLICADO);
        });
        if (creditoRepository.findByPedidoId(pedido.getId()).isPresent()) return;
        int puntos = pedido.getTotalUsd().divide(new BigDecimal("100"), 0, RoundingMode.DOWN).intValue();
        if (puntos <= 0) return;
        LocalDateTime acreditado = pedido.getPagadoEn() != null ? pedido.getPagadoEn() : LocalDateTime.now();
        PuntoCredito credito = new PuntoCredito();
        credito.setUsuario(pedido.getUsuario()); credito.setPedido(pedido);
        credito.setPuntosOtorgados(puntos); credito.setPuntosDisponibles(puntos);
        credito.setAcreditadoEn(acreditado); credito.setVenceEn(acreditado.plusMonths(12));
        creditoRepository.save(credito);
    }

    @Transactional
    public void revertirReserva(Pedido pedido) {
        canjeRepository.findByPedidoId(pedido.getId()).ifPresent(canje -> {
            if (canje.getEstado() != EstadoCanjePuntos.RESERVADO) return;
            LocalDateTime ahora = LocalDateTime.now();
            for (CanjePuntoItem item : canje.getItems()) {
                PuntoCredito credito = item.getCredito();
                if (credito.getEstado() == EstadoPuntoCredito.VIGENTE && credito.getVenceEn().isAfter(ahora)) {
                    credito.setPuntosDisponibles(credito.getPuntosDisponibles() + item.getPuntos());
                }
            }
            canje.setEstado(EstadoCanjePuntos.REVERTIDO);
        });
    }

    @Transactional
    public int vencerCreditos() {
        List<PuntoCredito> vencidos = creditoRepository.findByEstadoAndVenceEnBefore(EstadoPuntoCredito.VIGENTE, LocalDateTime.now());
        for (PuntoCredito c : vencidos) { c.setPuntosDisponibles(0); c.setEstado(EstadoPuntoCredito.VENCIDO); }
        return vencidos.size();
    }
}
