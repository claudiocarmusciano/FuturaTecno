package com.futuratecno.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Crédito de puntos originado en un pedido efectivamente cobrado. */
@Entity
@Table(name = "puntos_creditos")
public class PuntoCredito extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "puntos_otorgados", nullable = false)
    private Integer puntosOtorgados;

    @Column(name = "puntos_disponibles", nullable = false)
    private Integer puntosDisponibles;

    @Column(name = "acreditado_en", nullable = false)
    private LocalDateTime acreditadoEn;

    @Column(name = "vence_en", nullable = false)
    private LocalDateTime venceEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPuntoCredito estado = EstadoPuntoCredito.VIGENTE;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Integer getPuntosOtorgados() { return puntosOtorgados; }
    public void setPuntosOtorgados(Integer puntosOtorgados) { this.puntosOtorgados = puntosOtorgados; }
    public Integer getPuntosDisponibles() { return puntosDisponibles; }
    public void setPuntosDisponibles(Integer puntosDisponibles) { this.puntosDisponibles = puntosDisponibles; }
    public LocalDateTime getAcreditadoEn() { return acreditadoEn; }
    public void setAcreditadoEn(LocalDateTime acreditadoEn) { this.acreditadoEn = acreditadoEn; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime venceEn) { this.venceEn = venceEn; }
    public EstadoPuntoCredito getEstado() { return estado; }
    public void setEstado(EstadoPuntoCredito estado) { this.estado = estado; }
}
