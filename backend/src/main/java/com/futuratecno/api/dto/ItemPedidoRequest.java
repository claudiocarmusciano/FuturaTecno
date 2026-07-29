package com.futuratecno.api.dto;

/**
 * Un renglón del pedido tal como lo manda el carrito del frontend.
 * Solo viajan variante y cantidad: el precio lo calcula el backend, nunca se toma del cliente.
 */
public class ItemPedidoRequest {
    private Long varianteId;
    private Integer cantidad;

    public ItemPedidoRequest() {}

    public Long getVarianteId() { return varianteId; }
    public void setVarianteId(Long varianteId) { this.varianteId = varianteId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
