package com.futuratecno.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Un renglón de un pedido, con los datos del artículo COPIADOS al momento de confirmar.
 *
 * <p>Nombre, especificaciones, SKU, imagen y precios son snapshots, no lecturas del producto vivo:
 * el catálogo cambia (el mayorista pisa costos y stock a diario, el admin corrige nombres o
 * categorías) y el pedido tiene que seguir mostrando lo que el cliente aceptó. {@link #producto} y
 * {@link #variante} quedan solo para trazabilidad interna.
 */
@Entity
@Table(name = "pedido_items")
public class PedidoItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id")
    private Variante variante;

    @Column(name = "producto_nombre", nullable = false, length = 300)
    private String productoNombre;

    @Column(length = 500)
    private String especificaciones;

    @Column(length = 120)
    private String sku;

    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_usd", nullable = false)
    private BigDecimal precioUnitarioUsd;

    @Column(name = "precio_unitario_ars", nullable = false)
    private BigDecimal precioUnitarioArs;

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Variante getVariante() {
        return variante;
    }

    public void setVariante(Variante variante) {
        this.variante = variante;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public String getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(String especificaciones) {
        this.especificaciones = especificaciones;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitarioUsd() {
        return precioUnitarioUsd;
    }

    public void setPrecioUnitarioUsd(BigDecimal precioUnitarioUsd) {
        this.precioUnitarioUsd = precioUnitarioUsd;
    }

    public BigDecimal getPrecioUnitarioArs() {
        return precioUnitarioArs;
    }

    public void setPrecioUnitarioArs(BigDecimal precioUnitarioArs) {
        this.precioUnitarioArs = precioUnitarioArs;
    }

    /** Subtotal en USD del renglón (precio unitario × cantidad). */
    public BigDecimal subtotalUsd() {
        return precioUnitarioUsd.multiply(BigDecimal.valueOf(cantidad));
    }

    /** Subtotal en ARS del renglón (precio unitario × cantidad). */
    public BigDecimal subtotalArs() {
        return precioUnitarioArs.multiply(BigDecimal.valueOf(cantidad));
    }
}
