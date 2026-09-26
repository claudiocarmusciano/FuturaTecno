package com.futuratecno.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Lo que la home muestra del catálogo: cuántos productos hay, cuántos por categoría raíz (para
 * los chips), 8 destacados al azar y un candidato por categoría raíz para el hero. Antes la home
 * bajaba el catálogo entero para elegir estos pocos.
 */
public class PortadaCatalogoDTO {
    private int totalCatalogo;
    private Map<Long, Integer> cantidadPorCategoriaRaiz;
    private List<ProductoCatalogoDTO> destacados;
    private Map<Long, ProductoCatalogoDTO> heroPorCategoriaRaiz;

    public int getTotalCatalogo() { return totalCatalogo; }
    public void setTotalCatalogo(int totalCatalogo) { this.totalCatalogo = totalCatalogo; }
    public Map<Long, Integer> getCantidadPorCategoriaRaiz() { return cantidadPorCategoriaRaiz; }
    public void setCantidadPorCategoriaRaiz(Map<Long, Integer> m) { this.cantidadPorCategoriaRaiz = m; }
    public List<ProductoCatalogoDTO> getDestacados() { return destacados; }
    public void setDestacados(List<ProductoCatalogoDTO> destacados) { this.destacados = destacados; }
    public Map<Long, ProductoCatalogoDTO> getHeroPorCategoriaRaiz() { return heroPorCategoriaRaiz; }
    public void setHeroPorCategoriaRaiz(Map<Long, ProductoCatalogoDTO> m) { this.heroPorCategoriaRaiz = m; }
}
