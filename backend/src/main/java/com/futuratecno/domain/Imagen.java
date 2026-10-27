package com.futuratecno.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "imagenes")
public class Imagen extends BaseEntity {
    /** Largo máximo de una URL de foto (V47): las de Apple pasan los 500. */
    public static final int URL_MAX = 2000;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = Imagen.URL_MAX)
    private String url;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    /** Cargada a mano en Admin → Imágenes: la sync de Elit y la carga JSON no la borran (V46). */
    @Column(nullable = false)
    private Boolean manual = false;

    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
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
