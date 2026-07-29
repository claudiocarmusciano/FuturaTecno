package com.futuratecno.api.dto;

import java.math.BigDecimal;

/** Renglón de un pedido, con los datos congelados al confirmar. */
public class PedidoItemDTO {
    private Long id;
    private Long productoId;
    private Long varianteId;
    private String productoNombre;
    private String especificaciones;
    private String sku;
    private String imagenUrl;
    private Integer cantidad;
    private BigDecimal precioUnitarioUsd;
    private BigDecimal precioUnitarioArs;
    private BigDecimal subtotalUsd;
    private BigDecimal subtotalArs;

    public PedidoItemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Long getVarianteId() { return varianteId; }
    public void setVarianteId(Long varianteId) { this.varianteId = varianteId; }

    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

    public String getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(String especificaciones) { this.especificaciones = especificaciones; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitarioUsd() { return precioUnitarioUsd; }
    public void setPrecioUnitarioUsd(BigDecimal precioUnitarioUsd) { this.precioUnitarioUsd = precioUnitarioUsd; }

    public BigDecimal getPrecioUnitarioArs() { return precioUnitarioArs; }
    public void setPrecioUnitarioArs(BigDecimal precioUnitarioArs) { this.precioUnitarioArs = precioUnitarioArs; }

    public BigDecimal getSubtotalUsd() { return subtotalUsd; }
    public void setSubtotalUsd(BigDecimal subtotalUsd) { this.subtotalUsd = subtotalUsd; }

    public BigDecimal getSubtotalArs() { return subtotalArs; }
    public void setSubtotalArs(BigDecimal subtotalArs) { this.subtotalArs = subtotalArs; }
}
