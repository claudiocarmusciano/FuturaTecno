package com.futuratecno.api.dto;

import java.util.List;

public class CategoriaTreeDTO {
    private Long id;
    private String nombre;
    private List<CategoriaTreeDTO> hijos;

    public CategoriaTreeDTO() {}

    public CategoriaTreeDTO(Long id, String nombre, List<CategoriaTreeDTO> hijos) {
        this.id = id;
        this.nombre = nombre;
        this.hijos = hijos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<CategoriaTreeDTO> getHijos() { return hijos; }
    public void setHijos(List<CategoriaTreeDTO> hijos) { this.hijos = hijos; }
}
