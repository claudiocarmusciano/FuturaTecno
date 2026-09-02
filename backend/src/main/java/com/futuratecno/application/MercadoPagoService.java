package com.futuratecno.application;

import com.futuratecno.api.dto.CheckoutPagoDTO;
import com.futuratecno.domain.EstadoPago;
import com.futuratecno.domain.EstadoPedido;
import com.futuratecno.domain.Pedido;
import com.futuratecno.infrastructure.MercadoPagoGateway;
import com.futuratecno.infrastructure.PedidoRepository;
import com.mercadopago.resources.payment.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class MercadoPagoService {
    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoService.class);

    private final PedidoRepository pedidoRepository;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final PrecioService precioService;

    public MercadoPagoService(PedidoRepository pedidoRepository, MercadoPagoGateway mercadoPagoGateway,
                              PrecioService precioService) {
        this.pedidoRepository = pedidoRepository;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.precioService = precioService;
    }

    @Transactional
    public CheckoutPagoDTO iniciarCheckout(String numero, String emailUsuario) {
        Pedido pedido = pedidoRepository.findByNumeroForUpdate(numero)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        exigirPropietario(pedido, emailUsuario);

        if ("TRANSFERENCIA".equals(pedido.getMedioPago())) {
            throw new IllegalArgumentException("Este pedido fue confirmado para pagar por transferencia.");
        }

        if (pedido.getEstadoPago() == EstadoPago.APROBADO) {
            throw new IllegalArgumentException("Este pedido ya está pagado.");
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO || pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new IllegalArgumentException("Este pedido ya no admite pagos.");
        }
        if (pedido.getVenceEn().isBefore(LocalDateTime.now())) {
            pedido.setEstado(EstadoPedido.VENCIDO);
            throw new IllegalArgumentException("El pedido venció. Volvé al catálogo para actualizar precios y stock.");
        }

        BigDecimal monto = totalACobrar(pedido);
        if (pedido.getMercadoPagoPreferenceId() != null
                && pedido.getMercadoPagoCheckoutUrl() != null
                && monto.compareTo(pedido.getMontoPagoArs()) == 0) {
            return new CheckoutPagoDTO(pedido.getMercadoPagoCheckoutUrl(),
                    pedido.getMercadoPagoPreferenceId(), monto, mercadoPagoGateway.esPrueba());
        }

        MercadoPagoGateway.PreferenciaCreada preferencia = mercadoPagoGateway.crearPreferencia(pedido, monto);
        pedido.setMercadoPagoPreferenceId(preferencia.id());
        pedido.setMercadoPagoCheckoutUrl(preferencia.checkoutUrl());
        pedido.setMontoPagoArs(monto);
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        pedidoRepository.save(pedido);

        return new CheckoutPagoDTO(preferencia.checkoutUrl(), preferencia.id(), monto, preferencia.prueba());
    }

    /** Consulta el pago en MP: nunca confía en parámetros de importe o estado enviados por el navegador. */
    @Transactional
    public void procesarPago(Long paymentId) {
        if (paymentId == null || paymentId <= 0) return;
        Payment payment = mercadoPagoGateway.obtenerPago(paymentId);
        String numero = payment.getExternalReference();
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("El pago no está asociado a un pedido.");
        }

        Pedido pedido = pedidoRepository.findByNumeroForUpdate(numero)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));

        pedidoRepository.findByMercadoPagoPaymentId(paymentId)
                .filter(otro -> !otro.getId().equals(pedido.getId()))
                .ifPresent(otro -> { throw new IllegalArgumentException("El pago ya está vinculado a otro pedido."); });

        BigDecimal esperado = pedido.getMontoPagoArs() != null ? pedido.getMontoPagoArs() : totalACobrar(pedido);
        BigDecimal recibido = payment.getTransactionAmount();
        if (!"ARS".equalsIgnoreCase(payment.getCurrencyId())
                || recibido == null || esperado.compareTo(recibido.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException("El importe o la moneda del pago no coincide con el pedido.");
        }

        EstadoPago nuevoEstado = mapearEstado(payment.getStatus());
        // Una notificación tardía de otro intento fallido jamás debe desmarcar un pago aprobado.
        if (pedido.getEstadoPago() == EstadoPago.APROBADO && nuevoEstado != EstadoPago.APROBADO) return;

        pedido.setMercadoPagoPaymentId(payment.getId());
        pedido.setMercadoPagoStatusDetail(payment.getStatusDetail());
        pedido.setEstadoPago(nuevoEstado);
        pedido.setMontoPagoArs(esperado);
        if (nuevoEstado == EstadoPago.APROBADO) {
            pedido.setPagadoEn(payment.getDateApproved() != null
                    ? payment.getDateApproved().atZoneSameInstant(PedidoService.ZONA_AR).toLocalDateTime()
                    : LocalDateTime.now());
            if (pedido.getEstado() != EstadoPedido.ENTREGADO) pedido.setEstado(EstadoPedido.CONFIRMADO);
            logger.info("Pago aprobado para pedido {} (payment id {}).", pedido.getNumero(), payment.getId());
        }
        pedidoRepository.save(pedido);
    }

    @Transactional
    public void confirmarRetorno(String numero, Long paymentId, String emailUsuario) {
        Pedido pedido = pedidoRepository.findByNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        exigirPropietario(pedido, emailUsuario);
        procesarPago(paymentId);
    }

    private void exigirPropietario(Pedido pedido, String emailUsuario) {
        if (pedido.getUsuario() == null
                || !pedido.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
            throw new IllegalArgumentException("Pedido no encontrado.");
        }
    }

    private BigDecimal totalACobrar(Pedido pedido) {
        BigDecimal envio = pedido.getCostoEnvioArs() != null ? pedido.getCostoEnvioArs() : BigDecimal.ZERO;
        BigDecimal precioTransferencia = pedido.getTotalArs().add(envio).setScale(2, RoundingMode.HALF_UP);
        return precioService.precioMercadoPagoInmediato(precioTransferencia);
    }

    private EstadoPago mapearEstado(String status) {
        if (status == null) return EstadoPago.PENDIENTE;
        return switch (status) {
            case "approved" -> EstadoPago.APROBADO;
            case "in_process", "in_mediation" -> EstadoPago.EN_PROCESO;
            case "rejected" -> EstadoPago.RECHAZADO;
            case "cancelled" -> EstadoPago.CANCELADO;
            case "refunded" -> EstadoPago.REEMBOLSADO;
            case "charged_back" -> EstadoPago.CONTRACARGO;
            default -> EstadoPago.PENDIENTE;
        };
    }
}
