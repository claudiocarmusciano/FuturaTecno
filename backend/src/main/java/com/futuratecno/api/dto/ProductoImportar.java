package com.futuratecno.api.dto;

import java.math.BigDecimal;

/**
 * Un producto a importar (posiblemente editado por el usuario en la previsualización).
 * El backend recalcula el costo en USD a partir de "precio" + "moneda" usando la cotización actual.
 */
public class ProductoImportar {
    private String categoria;
    private String marca;
    private String modelo;
    private String especificaciones;
    private String moneda;
    private BigDecimal precio;

    public ProductoImportar() {}

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(String especificaciones) {
        this.especificaciones = especificaciones;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
