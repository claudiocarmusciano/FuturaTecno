package com.futuratecno.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(name = "margen_porcentaje", nullable = false)
    private BigDecimal margenPorcentaje;

    @Column(name = "flete_porcentaje", nullable = false)
    private BigDecimal fletePorcentaje;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL)
    private List<Producto> productos;

    @Column(nullable = false)
    private Boolean activo = true;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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
