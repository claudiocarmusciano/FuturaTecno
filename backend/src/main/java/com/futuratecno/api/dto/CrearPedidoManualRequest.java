package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orden de venta cargada por el admin (Admin → Pedidos → Nueva orden). A diferencia del carrito,
 * acá el precio SÍ puede venir en el request: lo manda un admin autenticado, no un cliente, y
 * cada precio cambiado queda registrado junto al del catálogo.
 */
public class CrearPedidoManualRequest {

    public static class Item {
        private Long varianteId;
        private Integer cantidad;
        /** Null = precio del catálogo del día. */
        private BigDecimal precioUnitarioUsd;

        public Long getVarianteId() { return varianteId; }
        public void setVarianteId(Long varianteId) { this.varianteId = varianteId; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public BigDecimal getPrecioUnitarioUsd() { return precioUnitarioUsd; }
        public void setPrecioUnitarioUsd(BigDecimal precioUnitarioUsd) { this.precioUnitarioUsd = precioUnitarioUsd; }
    }

    /** Si coincide con una cuenta activa, la orden queda en esa cuenta (y suma puntos). */
    private String email;
    private String nombre;
    private String telefono;
    private String medioPago;
    private String notas;
    private String cpDestino;
    private String modoEnvio;
    /** Costo de envío acordado a mano. Null con modalidad y CP = se cotiza con Andreani. */
    private BigDecimal costoEnvioArs;
    private List<Item> items;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getCpDestino() { return cpDestino; }
    public void setCpDestino(String cpDestino) { this.cpDestino = cpDestino; }
    public String getModoEnvio() { return modoEnvio; }
    public void setModoEnvio(String modoEnvio) { this.modoEnvio = modoEnvio; }
    public BigDecimal getCostoEnvioArs() { return costoEnvioArs; }
    public void setCostoEnvioArs(BigDecimal costoEnvioArs) { this.costoEnvioArs = costoEnvioArs; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
