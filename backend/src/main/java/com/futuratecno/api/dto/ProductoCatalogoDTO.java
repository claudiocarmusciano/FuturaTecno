package com.futuratecno.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProductoCatalogoDTO {
    private Long id;
    private String categoria;
    private String marca;
    private String modelo;
    private String imagenUrl;
    private LocalDateTime ultimaActualizacion;
    private List<VarianteCatalogoDTO> variantes;

    public ProductoCatalogoDTO() {}

    public ProductoCatalogoDTO(Long id, String categoria, String marca, String modelo, String imagenUrl, List<VarianteCatalogoDTO> variantes) {
        this.id = id;
        this.categoria = categoria;
        this.marca = marca;
        this.modelo = modelo;
        this.imagenUrl = imagenUrl;
        this.variantes = variantes;
    }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public List<VarianteCatalogoDTO> getVariantes() { return variantes; }
    public void setVariantes(List<VarianteCatalogoDTO> variantes) { this.variantes = variantes; }
}
