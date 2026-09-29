package com.futuratecno.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "proveedores")
public class Proveedor extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(length = 10)
    private String codigo;

    @Column(name = "margen_porcentaje", nullable = false)
    private BigDecimal margenPorcentaje;

    @Column(name = "flete_porcentaje", nullable = false)
    private BigDecimal fletePorcentaje;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL)
    private List<Producto> productos;

    @Column(nullable = false)
    private Boolean activo = true;

    /** Demora de entrega en días (V43). Los dos null = la entrega normal de la tienda. */
    @Column(name = "demora_entrega_min_dias")
    private Integer demoraEntregaMinDias;

    @Column(name = "demora_entrega_max_dias")
    private Integer demoraEntregaMaxDias;

    public Integer getDemoraEntregaMinDias() { return demoraEntregaMinDias; }
    public void setDemoraEntregaMinDias(Integer demoraEntregaMinDias) { this.demoraEntregaMinDias = demoraEntregaMinDias; }
    public Integer getDemoraEntregaMaxDias() { return demoraEntregaMaxDias; }
    public void setDemoraEntregaMaxDias(Integer demoraEntregaMaxDias) { this.demoraEntregaMaxDias = demoraEntregaMaxDias; }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getMargenPorcentaje() {
        return margenPorcentaje;
    }

    public void setMargenPorcentaje(BigDecimal margenPorcentaje) {
        this.margenPorcentaje = margenPorcentaje;
    }

    public BigDecimal getFletePorcentaje() {
        return fletePorcentaje;
    }

    public void setFletePorcentaje(BigDecimal fletePorcentaje) {
        this.fletePorcentaje = fletePorcentaje;
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
