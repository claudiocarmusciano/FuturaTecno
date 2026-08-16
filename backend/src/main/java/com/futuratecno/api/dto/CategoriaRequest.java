package com.futuratecno.api.dto;

/** Alta o edición de una categoría. padreId null = categoría de primer nivel. */
public class CategoriaRequest {
    private String nombre;
    private Long padreId;
    private Integer pesoGramosDefault;
    private Integer altoCmDefault;
    private Integer anchoCmDefault;
    private Integer largoCmDefault;

    public CategoriaRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Long getPadreId() { return padreId; }
    public void setPadreId(Long padreId) { this.padreId = padreId; }

    public Integer getPesoGramosDefault() { return pesoGramosDefault; }
    public void setPesoGramosDefault(Integer pesoGramosDefault) { this.pesoGramosDefault = pesoGramosDefault; }
    public Integer getAltoCmDefault() { return altoCmDefault; }
    public void setAltoCmDefault(Integer altoCmDefault) { this.altoCmDefault = altoCmDefault; }
    public Integer getAnchoCmDefault() { return anchoCmDefault; }
    public void setAnchoCmDefault(Integer anchoCmDefault) { this.anchoCmDefault = anchoCmDefault; }
    public Integer getLargoCmDefault() { return largoCmDefault; }
    public void setLargoCmDefault(Integer largoCmDefault) { this.largoCmDefault = largoCmDefault; }
}
