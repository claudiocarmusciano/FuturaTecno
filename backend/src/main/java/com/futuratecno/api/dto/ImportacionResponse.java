package com.futuratecno.api.dto;

public class ImportacionResponse {
    private int creados;
    private int actualizados;
    private int omitidos;
    private String mensaje;

    public ImportacionResponse() {}

    public ImportacionResponse(int creados, int actualizados, int omitidos, String mensaje) {
        this.creados = creados;
        this.actualizados = actualizados;
        this.omitidos = omitidos;
        this.mensaje = mensaje;
    }

    public int getCreados() {
        return creados;
    }

    public void setCreados(int creados) {
        this.creados = creados;
    }

    public int getActualizados() {
        return actualizados;
    }

    public void setActualizados(int actualizados) {
        this.actualizados = actualizados;
    }

    public int getOmitidos() {
        return omitidos;
    }

    public void setOmitidos(int omitidos) {
        this.omitidos = omitidos;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
