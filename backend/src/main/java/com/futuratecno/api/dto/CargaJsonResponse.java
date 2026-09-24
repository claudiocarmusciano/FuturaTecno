package com.futuratecno.api.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de la carga por JSON: contadores + detalle por artículo. */
public class CargaJsonResponse {
    private int creados;
    private int actualizados;
    private int omitidos;
    private int sinCategoria;   // cuántos quedaron sin categoría (para asignar a mano)
    private int revision;       // cuántos quedaron para revisar: ni se crearon ni se actualizaron (V42)
    private String mensaje;
    private List<Item> items = new ArrayList<>();

    public static class Item {
        private String producto;    // "ASUS Vivobook Go 15 ..."
        private String estado;      // creado | actualizado | omitido | revision
        private String categoria;   // path asignado, o null si quedó sin clasificar
        private String motivo;      // si se omitió o quedó en revisión, por qué
        // Agregados con la identidad (V42). Opcionales: un consumidor viejo puede ignorarlos.
        private Long productoId;           // producto creado o actualizado
        private String identidad;          // clave de identidad resuelta (null si quedó en revisión)
        private List<Long> candidatos;     // productos existentes relacionados con la decisión
        private String aviso;              // se cargó, pero hay algo para mirar (p. ej. copias dadas de baja)

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
        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }
        public String getIdentidad() { return identidad; }
        public void setIdentidad(String identidad) { this.identidad = identidad; }
        public List<Long> getCandidatos() { return candidatos; }
        public void setCandidatos(List<Long> candidatos) { this.candidatos = candidatos; }
        public String getAviso() { return aviso; }
        public void setAviso(String aviso) { this.aviso = aviso; }
    }

    public int getCreados() { return creados; }
    public void setCreados(int creados) { this.creados = creados; }
    public int getActualizados() { return actualizados; }
    public void setActualizados(int actualizados) { this.actualizados = actualizados; }
    public int getOmitidos() { return omitidos; }
    public void setOmitidos(int omitidos) { this.omitidos = omitidos; }
    public int getRevision() { return revision; }
    public void setRevision(int revision) { this.revision = revision; }
    public int getSinCategoria() { return sinCategoria; }
    public void setSinCategoria(int sinCategoria) { this.sinCategoria = sinCategoria; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
