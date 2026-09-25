package com.futuratecno.api.dto;

import java.util.List;

/** Pedido de la carga por JSON: bajo qué proveedor y la lista de artículos. */
public class CargaJsonRequest {
    private Long proveedorId;
    private List<Long> reemplazarProductoIds;

    public List<Long> getReemplazarProductoIds() { return reemplazarProductoIds; }
    public void setReemplazarProductoIds(List<Long> ids) { this.reemplazarProductoIds = ids; }
    private List<ArticuloJsonDTO> articulos;

    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }

    public List<ArticuloJsonDTO> getArticulos() { return articulos; }
    public void setArticulos(List<ArticuloJsonDTO> articulos) { this.articulos = articulos; }
}
