package com.futuratecno.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** Reserva de puntos de un pedido. Se aplica sólo al acreditarse el pago. */
@Entity
@Table(name = "canjes_puntos")
public class CanjePuntos extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Column(nullable = false)
    private Integer puntos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCanjePuntos estado = EstadoCanjePuntos.RESERVADO;

    @OneToMany(mappedBy = "canje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CanjePuntoItem> items = new ArrayList<>();

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Integer getPuntos() { return puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
    public EstadoCanjePuntos getEstado() { return estado; }
    public void setEstado(EstadoCanjePuntos estado) { this.estado = estado; }
    public List<CanjePuntoItem> getItems() { return items; }
}
