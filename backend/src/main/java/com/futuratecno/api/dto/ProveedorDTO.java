package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProveedorDTO {
    private Long id;
    private String nombre;
    private BigDecimal margenPorcentaje;
    private BigDecimal fletePorcentaje;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProveedorDTO() {}

    public ProveedorDTO(Long id, String nombre, BigDecimal margenPorcentaje, BigDecimal fletePorcentaje, Boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nombre = nombre;
        this.margenPorcentaje = margenPorcentaje;
        this.fletePorcentaje = fletePorcentaje;
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
