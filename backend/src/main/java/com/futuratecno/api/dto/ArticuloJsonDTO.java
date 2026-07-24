package com.futuratecno.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Un artículo del JSON que se carga manualmente por la sección "Cargar artículos por JSON".
 * Acepta el formato con claves en snake_case (modelo_exacto, descripcion_corta, precio_usd).
 * Campos opcionales: si falta marca, modelo o precio, el artículo se omite.
 */
public class ArticuloJsonDTO {
    private String listado;
    private String marca;
    private String modelo;                       // modelo "limpio" si se provee; si no, se deriva de modelo_exacto
    @JsonProperty("modelo_exacto")
    private String modeloExacto;
    @JsonProperty("descripcion_corta")
    private String descripcionCorta;
    private Map<String, Object> especificaciones;
    @JsonProperty("precio_usd")
    private BigDecimal precioUsd;
    private List<String> imagenes;
    private String fuente;
    private String notas;
    private String categoria;                    // opcional: hint de categoría (path "Notebooks > Consumo" o nombre de hoja)

    public String getListado() { return listado; }
    public void setListado(String listado) { this.listado = listado; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getModeloExacto() { return modeloExacto; }
    public void setModeloExacto(String modeloExacto) { this.modeloExacto = modeloExacto; }

    public String getDescripcionCorta() { return descripcionCorta; }
    public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }

    public Map<String, Object> getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(Map<String, Object> especificaciones) { this.especificaciones = especificaciones; }

    public BigDecimal getPrecioUsd() { return precioUsd; }
    public void setPrecioUsd(BigDecimal precioUsd) { this.precioUsd = precioUsd; }

    public List<String> getImagenes() { return imagenes; }
    public void setImagenes(List<String> imagenes) { this.imagenes = imagenes; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
