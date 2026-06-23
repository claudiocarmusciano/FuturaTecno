package com.futuratecno.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "imagenes")
public class Imagen extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id", nullable = false)
    private Variante variante;

    @Column(nullable = false)
    private String url;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    public Variante getVariante() {
        return variante;
    }

    public void setVariante(Variante variante) {
        this.variante = variante;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
