package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Producto en formato editable para el admin (incluye variantes con su precio de origen). */
public class ProductoEditDTO {
    private Long id;
    private Long categoriaId;
    private String categoria;   // solo lectura (informativo): nombre de la subcategoría resuelta
    private String marca;
    private String modelo;
    private String proveedor;   // solo lectura (informativo)
    private String imagenUrl;
    private List<VarianteEditDTO> variantes;

    // Peso/dimensiones propios del producto (override del default de categoría). Null = no cargado.
    private Integer pesoGramos;
    private Integer altoCm;
    private Integer anchoCm;
    private Integer largoCm;

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

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

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

    public Integer getPesoGramos() { return pesoGramos; }
    public void setPesoGramos(Integer pesoGramos) { this.pesoGramos = pesoGramos; }

    public Integer getAltoCm() { return altoCm; }
    public void setAltoCm(Integer altoCm) { this.altoCm = altoCm; }

    public Integer getAnchoCm() { return anchoCm; }
    public void setAnchoCm(Integer anchoCm) { this.anchoCm = anchoCm; }

    public Integer getLargoCm() { return largoCm; }
    public void setLargoCm(Integer largoCm) { this.largoCm = largoCm; }
}
