package com.futuratecno.api.dto;

public class ClasificarCategoriasResponse {
    private int procesados;
    private int clasificados;
    private int sinClasificar;
    private String mensaje;

    public ClasificarCategoriasResponse() {}

    public ClasificarCategoriasResponse(int procesados, int clasificados, int sinClasificar, String mensaje) {
        this.procesados = procesados;
        this.clasificados = clasificados;
        this.sinClasificar = sinClasificar;
        this.mensaje = mensaje;
    }

    public int getProcesados() { return procesados; }
    public void setProcesados(int procesados) { this.procesados = procesados; }

    public int getClasificados() { return clasificados; }
    public void setClasificados(int clasificados) { this.clasificados = clasificados; }

    public int getSinClasificar() { return sinClasificar; }
    public void setSinClasificar(int sinClasificar) { this.sinClasificar = sinClasificar; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
