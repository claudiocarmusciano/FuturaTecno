package com.futuratecno.domain;

import jakarta.persistence.*;

/** Trazabilidad FIFO: permite devolver exactamente los créditos reservados si el pedido vence. */
@Entity
@Table(name = "canjes_puntos_items")
public class CanjePuntoItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canje_id", nullable = false)
    private CanjePuntos canje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id", nullable = false)
    private PuntoCredito credito;

    @Column(nullable = false)
    private Integer puntos;

    public CanjePuntos getCanje() { return canje; }
    public void setCanje(CanjePuntos canje) { this.canje = canje; }
    public PuntoCredito getCredito() { return credito; }
    public void setCredito(PuntoCredito credito) { this.credito = credito; }
    public Integer getPuntos() { return puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
}
