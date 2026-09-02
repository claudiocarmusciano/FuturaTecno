package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Un pedido para mostrar (historial del cliente o bandeja del admin). */
public class PedidoDTO {
    private Long id;
    private String numero;
    private String estado;
    private BigDecimal totalUsd;
    private BigDecimal totalArs;
    private BigDecimal cotizacionUsada;
    private String nombreContacto;
    private String telefonoContacto;
    private String notas;
    private LocalDateTime venceEn;
    private LocalDateTime createdAt;
    private List<PedidoItemDTO> items;

    // Envío elegido en el checkout. Todo null = retiro / a coordinar. Costo null con modo
    // cargado = la modalidad quedó elegida pero Andreani no respondió al confirmar ("a cotizar").
    private String cpDestino;
    private String modoEnvio;
    private BigDecimal costoEnvioArs;
    private BigDecimal totalCobroArs;
    private String medioPago;
    private String estadoPago;
    private Long mercadoPagoPaymentId;
    private String mercadoPagoStatusDetail;
    private LocalDateTime pagadoEn;

    /** Email del cliente. Solo se completa para el admin; en el historial propio sobra. */
    private String usuarioEmail;

    public PedidoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getTotalUsd() { return totalUsd; }
    public void setTotalUsd(BigDecimal totalUsd) { this.totalUsd = totalUsd; }

    public BigDecimal getTotalArs() { return totalArs; }
    public void setTotalArs(BigDecimal totalArs) { this.totalArs = totalArs; }

    public BigDecimal getCotizacionUsada() { return cotizacionUsada; }
    public void setCotizacionUsada(BigDecimal cotizacionUsada) { this.cotizacionUsada = cotizacionUsada; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime venceEn) { this.venceEn = venceEn; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<PedidoItemDTO> getItems() { return items; }
    public void setItems(List<PedidoItemDTO> items) { this.items = items; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getCpDestino() { return cpDestino; }
    public void setCpDestino(String cpDestino) { this.cpDestino = cpDestino; }

    public String getModoEnvio() { return modoEnvio; }
    public void setModoEnvio(String modoEnvio) { this.modoEnvio = modoEnvio; }

    public BigDecimal getCostoEnvioArs() { return costoEnvioArs; }
    public void setCostoEnvioArs(BigDecimal costoEnvioArs) { this.costoEnvioArs = costoEnvioArs; }

    public BigDecimal getTotalCobroArs() { return totalCobroArs; }
    public void setTotalCobroArs(BigDecimal totalCobroArs) { this.totalCobroArs = totalCobroArs; }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public Long getMercadoPagoPaymentId() { return mercadoPagoPaymentId; }
    public void setMercadoPagoPaymentId(Long mercadoPagoPaymentId) { this.mercadoPagoPaymentId = mercadoPagoPaymentId; }

    public String getMercadoPagoStatusDetail() { return mercadoPagoStatusDetail; }
    public void setMercadoPagoStatusDetail(String mercadoPagoStatusDetail) { this.mercadoPagoStatusDetail = mercadoPagoStatusDetail; }

    public LocalDateTime getPagadoEn() { return pagadoEn; }
    public void setPagadoEn(LocalDateTime pagadoEn) { this.pagadoEn = pagadoEn; }
}
