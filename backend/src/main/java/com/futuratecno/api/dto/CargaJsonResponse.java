package com.futuratecno.api.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de la carga por JSON: contadores + detalle por artículo. */
public class CargaJsonResponse {
    private int creados;
    private int actualizados;
    private int omitidos;
    private int sinCategoria;   // cuántos quedaron sin categoría (para asignar a mano)
    private String mensaje;
    private List<Item> items = new ArrayList<>();

    public static class Item {
        private String producto;    // "ASUS Vivobook Go 15 ..."
        private String estado;      // creado | actualizado | omitido
        private String categoria;   // path asignado, o null si quedó sin clasificar
        private String motivo;      // si se omitió, por qué

        public Item() {}
        public Item(String producto, String estado, String categoria, String motivo) {
            this.producto = producto; this.estado = estado; this.categoria = categoria; this.motivo = motivo;
        }
        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }

    public int getCreados() { return creados; }
    public void setCreados(int creados) { this.creados = creados; }
    public int getActualizados() { return actualizados; }
    public void setActualizados(int actualizados) { this.actualizados = actualizados; }
    public int getOmitidos() { return omitidos; }
    public void setOmitidos(int omitidos) { this.omitidos = omitidos; }
    public int getSinCategoria() { return sinCategoria; }
    public void setSinCategoria(int sinCategoria) { this.sinCategoria = sinCategoria; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
