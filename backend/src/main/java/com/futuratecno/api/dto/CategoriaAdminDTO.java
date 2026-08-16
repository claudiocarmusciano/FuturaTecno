package com.futuratecno.api.dto;

import java.util.List;

/**
 * Nodo del árbol para la pantalla de categorías del admin. A diferencia de CategoriaTreeDTO
 * (que es el del catálogo público), trae el conteo de productos: es lo que necesita el admin
 * para saber si puede borrar una categoría o cuántos artículos tendría que reasignar antes.
 */
public class CategoriaAdminDTO {
    private Long id;
    private String nombre;
    private Long padreId;
    private long cantidadProductos;
    private List<CategoriaAdminDTO> hijos;
    private Integer pesoGramosDefault;
    private Integer altoCmDefault;
    private Integer anchoCmDefault;
    private Integer largoCmDefault;

    public CategoriaAdminDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Long getPadreId() { return padreId; }
    public void setPadreId(Long padreId) { this.padreId = padreId; }

    public long getCantidadProductos() { return cantidadProductos; }
    public void setCantidadProductos(long cantidadProductos) { this.cantidadProductos = cantidadProductos; }

    public List<CategoriaAdminDTO> getHijos() { return hijos; }
    public void setHijos(List<CategoriaAdminDTO> hijos) { this.hijos = hijos; }

    public Integer getPesoGramosDefault() { return pesoGramosDefault; }
    public void setPesoGramosDefault(Integer pesoGramosDefault) { this.pesoGramosDefault = pesoGramosDefault; }
    public Integer getAltoCmDefault() { return altoCmDefault; }
    public void setAltoCmDefault(Integer altoCmDefault) { this.altoCmDefault = altoCmDefault; }
    public Integer getAnchoCmDefault() { return anchoCmDefault; }
    public void setAnchoCmDefault(Integer anchoCmDefault) { this.anchoCmDefault = anchoCmDefault; }
    public Integer getLargoCmDefault() { return largoCmDefault; }
    public void setLargoCmDefault(Integer largoCmDefault) { this.largoCmDefault = largoCmDefault; }
}
