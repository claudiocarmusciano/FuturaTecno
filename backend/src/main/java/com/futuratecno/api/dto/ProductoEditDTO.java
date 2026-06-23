package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Producto en formato editable para el admin (incluye variantes con su precio de origen). */
public class ProductoEditDTO {
    private Long id;
    private String categoria;
    private String marca;
    private String modelo;
    private String proveedor;   // solo lectura (informativo)
    private String imagenUrl;
    private List<VarianteEditDTO> variantes;

    // Datos para que el editor calcule el precio de venta en vivo (solo lectura).
    private BigDecimal margenPorcentaje;
    private BigDecimal fletePorcentaje;
    private BigDecimal cotizacion;

    public ProductoEditDTO() {}

    public BigDecimal getMargenPorcentaje() { return margenPorcentaje; }
    public void setMargenPorcentaje(BigDecimal margenPorcentaje) { this.margenPorcentaje = margenPorcentaje; }

    public BigDecimal getFletePorcentaje() { return fletePorcentaje; }
    public void setFletePorcentaje(BigDecimal fletePorcentaje) { this.fletePorcentaje = fletePorcentaje; }

    public BigDecimal getCotizacion() { return cotizacion; }
    public void setCotizacion(BigDecimal cotizacion) { this.cotizacion = cotizacion; }

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

    public List<VarianteEditDTO> getVariantes() { return variantes; }
    public void setVariantes(List<VarianteEditDTO> variantes) { this.variantes = variantes; }
}
