package com.futuratecno.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un pedido confirmado por un cliente desde el catálogo.
 *
 * <p>Los importes están congelados al momento de confirmar (ver {@link PedidoItem}): el precio de
 * venta se recalcula todos los días con la cotización y los costos del mayorista, así que un pedido
 * tiene que recordar lo que el cliente efectivamente aceptó.
 */
@Entity
@Table(name = "pedidos")
public class Pedido extends BaseEntity {

    /** Número legible para hablar con el cliente (FT-000123). Único, sale de pedidos_numero_seq. */
    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "total_usd", nullable = false)
    private BigDecimal totalUsd;

    @Column(name = "total_ars", nullable = false)
    private BigDecimal totalArs;

    /** Cotización USD/ARS usada para este pedido, para poder explicar el total en pesos. */
    @Column(name = "cotizacion_usada", nullable = false)
    private BigDecimal cotizacionUsada;

    @Column(name = "nombre_contacto", length = 150)
    private String nombreContacto;

    @Column(name = "telefono_contacto", length = 50)
    private String telefonoContacto;

    @Column(length = 1000)
    private String notas;

    /** Próximo corte de las 06:30 AR. Pasado ese momento el pedido se marca VENCIDO. */
    @Column(name = "vence_en", nullable = false)
    private LocalDateTime venceEn;

    // Envío elegido en el checkout (V15). Todo nullable: null = retiro / a coordinar. El costo
    // está congelado al confirmar (recotizado server-side); si Andreani no respondió en ese
    // momento queda la modalidad con costo null ("a cotizar").
    @Column(name = "cp_destino", length = 10)
    private String cpDestino;

    @Column(name = "modo_envio", length = 30)
    private String modoEnvio;

    @Column(name = "costo_envio_ars")
    private BigDecimal costoEnvioArs;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago = EstadoPago.SIN_INICIAR;

    @Column(name = "mercado_pago_preference_id", length = 120)
    private String mercadoPagoPreferenceId;

    @Column(name = "mercado_pago_payment_id")
    private Long mercadoPagoPaymentId;

    @Column(name = "mercado_pago_status_detail", length = 150)
    private String mercadoPagoStatusDetail;

    @Column(name = "mercado_pago_checkout_url", length = 1000)
    private String mercadoPagoCheckoutUrl;

    /** Importe total enviado a Mercado Pago, incluidos los gastos de envío ya cotizados. */
    @Column(name = "monto_pago_ars")
    private BigDecimal montoPagoArs;

    @Column(name = "pagado_en")
    private LocalDateTime pagadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public BigDecimal getTotalUsd() {
        return totalUsd;
    }

    public void setTotalUsd(BigDecimal totalUsd) {
        this.totalUsd = totalUsd;
    }

    public BigDecimal getTotalArs() {
        return totalArs;
    }

    public void setTotalArs(BigDecimal totalArs) {
        this.totalArs = totalArs;
    }

    public BigDecimal getCotizacionUsada() {
        return cotizacionUsada;
    }

    public void setCotizacionUsada(BigDecimal cotizacionUsada) {
        this.cotizacionUsada = cotizacionUsada;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public LocalDateTime getVenceEn() {
        return venceEn;
    }

    public void setVenceEn(LocalDateTime venceEn) {
        this.venceEn = venceEn;
    }

    public String getCpDestino() {
        return cpDestino;
    }

    public void setCpDestino(String cpDestino) {
        this.cpDestino = cpDestino;
    }

    public String getModoEnvio() {
        return modoEnvio;
    }

    public void setModoEnvio(String modoEnvio) {
        this.modoEnvio = modoEnvio;
    }

    public BigDecimal getCostoEnvioArs() {
        return costoEnvioArs;
    }

    public void setCostoEnvioArs(BigDecimal costoEnvioArs) {
        this.costoEnvioArs = costoEnvioArs;
    }

    public EstadoPago getEstadoPago() { return estadoPago; }
    public void setEstadoPago(EstadoPago estadoPago) { this.estadoPago = estadoPago; }

    public String getMercadoPagoPreferenceId() { return mercadoPagoPreferenceId; }
    public void setMercadoPagoPreferenceId(String mercadoPagoPreferenceId) { this.mercadoPagoPreferenceId = mercadoPagoPreferenceId; }

    public Long getMercadoPagoPaymentId() { return mercadoPagoPaymentId; }
    public void setMercadoPagoPaymentId(Long mercadoPagoPaymentId) { this.mercadoPagoPaymentId = mercadoPagoPaymentId; }

    public String getMercadoPagoStatusDetail() { return mercadoPagoStatusDetail; }
    public void setMercadoPagoStatusDetail(String mercadoPagoStatusDetail) { this.mercadoPagoStatusDetail = mercadoPagoStatusDetail; }

    public String getMercadoPagoCheckoutUrl() { return mercadoPagoCheckoutUrl; }
    public void setMercadoPagoCheckoutUrl(String mercadoPagoCheckoutUrl) { this.mercadoPagoCheckoutUrl = mercadoPagoCheckoutUrl; }

    public BigDecimal getMontoPagoArs() { return montoPagoArs; }
    public void setMontoPagoArs(BigDecimal montoPagoArs) { this.montoPagoArs = montoPagoArs; }

    public LocalDateTime getPagadoEn() { return pagadoEn; }
    public void setPagadoEn(LocalDateTime pagadoEn) { this.pagadoEn = pagadoEn; }

    public List<PedidoItem> getItems() {
        return items;
    }

    public void setItems(List<PedidoItem> items) {
        this.items = items;
    }
}
