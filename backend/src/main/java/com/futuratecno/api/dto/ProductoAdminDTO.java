package com.futuratecno.api.dto;

import java.time.LocalDateTime;

public class ProductoAdminDTO {
    private Long id;
    private String categoria;
    private String marca;
    private String modelo;
    private String proveedor;
    private String imagenUrl;
    private String especificaciones;   // de la primera variante (para búsqueda de imagen)
    private LocalDateTime ultimaActualizacion;

    public ProductoAdminDTO() {}

    public ProductoAdminDTO(Long id, String categoria, String marca, String modelo, String proveedor, String imagenUrl) {
        this.id = id;
        this.categoria = categoria;
        this.marca = marca;
        this.modelo = modelo;
        this.proveedor = proveedor;
        this.imagenUrl = imagenUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(String especificaciones) { this.especificaciones = especificaciones; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}
