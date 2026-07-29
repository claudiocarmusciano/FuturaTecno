package com.futuratecno.api.dto;

/** Alta o edición de una categoría. padreId null = categoría de primer nivel. */
public class CategoriaRequest {
    private String nombre;
    private Long padreId;

    public CategoriaRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Long getPadreId() { return padreId; }
    public void setPadreId(Long padreId) { this.padreId = padreId; }
}
