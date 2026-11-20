package com.futuratecno.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Un presupuesto de "Armá tu PC" guardado por un cliente registrado. */
@Entity
@Table(name = "presupuestos_pc")
public class PresupuestoPc extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String seleccion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "total_ars", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalArs;

    @Column(name = "total_usd", precision = 14, scale = 2)
    private BigDecimal totalUsd;

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSeleccion() { return seleccion; }
    public void setSeleccion(String seleccion) { this.seleccion = seleccion; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public BigDecimal getTotalArs() { return totalArs; }
    public void setTotalArs(BigDecimal totalArs) { this.totalArs = totalArs; }
    public BigDecimal getTotalUsd() { return totalUsd; }
    public void setTotalUsd(BigDecimal totalUsd) { this.totalUsd = totalUsd; }
}
