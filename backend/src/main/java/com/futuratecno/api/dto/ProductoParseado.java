package com.futuratecno.api.dto;

import java.math.BigDecimal;

public class ProductoParseado {
    private String categoria;
    private String marca;
    private String modelo;
    private String especificaciones;
    private BigDecimal precioUsd;
    private BigDecimal precioArs;
    private String monedaOrigen;
    private String estado;
    private String mensaje;

    public ProductoParseado() {}

    public ProductoParseado(String marca, String modelo, String especificaciones, BigDecimal precioUsd, BigDecimal precioArs, String monedaOrigen, String estado, String mensaje) {
        this.marca = marca;
        this.modelo = modelo;
        this.especificaciones = especificaciones;
        this.precioUsd = precioUsd;
        this.precioArs = precioArs;
        this.monedaOrigen = monedaOrigen;
        this.estado = estado;
        this.mensaje = mensaje;
    }

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

    public BigDecimal getPrecioUsd() {
        return precioUsd;
    }

    public void setPrecioUsd(BigDecimal precioUsd) {
        this.precioUsd = precioUsd;
    }

    public BigDecimal getPrecioArs() {
        return precioArs;
    }

    public void setPrecioArs(BigDecimal precioArs) {
        this.precioArs = precioArs;
    }

    public String getMonedaOrigen() {
        return monedaOrigen;
    }

    public void setMonedaOrigen(String monedaOrigen) {
        this.monedaOrigen = monedaOrigen;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
