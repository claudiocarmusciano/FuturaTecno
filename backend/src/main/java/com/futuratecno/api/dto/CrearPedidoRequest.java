package com.futuratecno.api.dto;

import java.util.List;

/** Lo que manda el checkout para crear un pedido. El usuario sale del JWT, no del body. */
public class CrearPedidoRequest {
    private List<ItemPedidoRequest> items;
    private String nombreContacto;
    private String telefonoContacto;
    private String notas;

    // Tildar el checkbox del checkout ("confirmar implica un compromiso de compra"). El backend
    // lo valida igual que el precio: no alcanza con que el frontend deshabilite el botón, porque
    // nada impide llamar al endpoint directo.
    private Boolean aceptaCompromiso;

    public CrearPedidoRequest() {}

    public List<ItemPedidoRequest> getItems() { return items; }
    public void setItems(List<ItemPedidoRequest> items) { this.items = items; }

    public Boolean getAceptaCompromiso() { return aceptaCompromiso; }
    public void setAceptaCompromiso(Boolean aceptaCompromiso) { this.aceptaCompromiso = aceptaCompromiso; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
