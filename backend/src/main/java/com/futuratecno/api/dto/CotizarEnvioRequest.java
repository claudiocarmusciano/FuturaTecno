package com.futuratecno.api.dto;

import java.util.List;

/** Lo que manda el checkout para cotizar el envío: CP de destino + el carrito (variante y cantidad). */
public class CotizarEnvioRequest {
    private String cpDestino;
    private List<ItemPedidoRequest> items;

    public CotizarEnvioRequest() {}

    public String getCpDestino() { return cpDestino; }
    public void setCpDestino(String cpDestino) { this.cpDestino = cpDestino; }

    public List<ItemPedidoRequest> getItems() { return items; }
    public void setItems(List<ItemPedidoRequest> items) { this.items = items; }
}
