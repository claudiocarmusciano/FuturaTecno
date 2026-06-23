package com.futuratecno.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "variantes")
public class Variante extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column
    private String especificaciones;

    @Column(name = "almacenamiento_gb")
    private Integer almacenamientoGb;

    @Column(name = "ram_gb")
    private Integer ramGb;

    @Column
    private String color;

    @Column(name = "costo_usd", nullable = false)
    private BigDecimal costoUsd;

    @Column(name = "moneda_origen")
    private String monedaOrigen;

    @Column(name = "precio_origen")
    private BigDecimal precioOrigen;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "variante", cascade = CascadeType.ALL)
    private List<Imagen> imagenes;

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(String especificaciones) {
        this.especificaciones = especificaciones;
    }

    public Integer getAlmacenamientoGb() {
        return almacenamientoGb;
    }

    public void setAlmacenamientoGb(Integer almacenamientoGb) {
        this.almacenamientoGb = almacenamientoGb;
    }

    public Integer getRamGb() {
        return ramGb;
    }

    public void setRamGb(Integer ramGb) {
        this.ramGb = ramGb;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getCostoUsd() {
        return costoUsd;
    }

    public void setCostoUsd(BigDecimal costoUsd) {
        this.costoUsd = costoUsd;
    }

    public String getMonedaOrigen() {
        return monedaOrigen;
    }

    public void setMonedaOrigen(String monedaOrigen) {
        this.monedaOrigen = monedaOrigen;
    }

    public BigDecimal getPrecioOrigen() {
        return precioOrigen;
    }

    public void setPrecioOrigen(BigDecimal precioOrigen) {
        this.precioOrigen = precioOrigen;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<Imagen> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<Imagen> imagenes) {
        this.imagenes = imagenes;
    }
}
