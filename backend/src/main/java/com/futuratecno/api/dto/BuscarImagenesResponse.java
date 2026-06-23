package com.futuratecno.api.dto;

public class BuscarImagenesResponse {
    private int procesados;
    private int encontradas;
    private int noEncontradas;
    private int desdeIcecat;
    private int desdeGoogle;
    private String mensaje;

    public BuscarImagenesResponse() {}

    public BuscarImagenesResponse(int procesados, int encontradas, int noEncontradas, String mensaje) {
        this.procesados = procesados;
        this.encontradas = encontradas;
        this.noEncontradas = noEncontradas;
        this.mensaje = mensaje;
    }

    public int getDesdeIcecat() { return desdeIcecat; }
    public void setDesdeIcecat(int desdeIcecat) { this.desdeIcecat = desdeIcecat; }

    public int getDesdeGoogle() { return desdeGoogle; }
    public void setDesdeGoogle(int desdeGoogle) { this.desdeGoogle = desdeGoogle; }

    private int desdeDuckDuckGo;
    public int getDesdeDuckDuckGo() { return desdeDuckDuckGo; }
    public void setDesdeDuckDuckGo(int desdeDuckDuckGo) { this.desdeDuckDuckGo = desdeDuckDuckGo; }

    private int desdeAnthropic;
    public int getDesdeAnthropic() { return desdeAnthropic; }
    public void setDesdeAnthropic(int desdeAnthropic) { this.desdeAnthropic = desdeAnthropic; }

    public int getProcesados() { return procesados; }
    public void setProcesados(int procesados) { this.procesados = procesados; }

    public int getEncontradas() { return encontradas; }
    public void setEncontradas(int encontradas) { this.encontradas = encontradas; }

    public int getNoEncontradas() { return noEncontradas; }
    public void setNoEncontradas(int noEncontradas) { this.noEncontradas = noEncontradas; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
