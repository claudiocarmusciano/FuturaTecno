package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class ParsingResponse {
    private List<ProductoParseado> productos;
    private int total;
    private int procesados;
    private String mensaje;
    private BigDecimal cotizacionUsdArs;

    public ParsingResponse() {}

    public ParsingResponse(List<ProductoParseado> productos, int total, int procesados, String mensaje) {
        this.productos = productos;
        this.total = total;
        this.procesados = procesados;
        this.mensaje = mensaje;
    }

    public List<ProductoParseado> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoParseado> productos) {
        this.productos = productos;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getProcesados() {
        return procesados;
    }

    public void setProcesados(int procesados) {
        this.procesados = procesados;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public BigDecimal getCotizacionUsdArs() {
        return cotizacionUsdArs;
    }

    public void setCotizacionUsdArs(BigDecimal cotizacionUsdArs) {
        this.cotizacionUsdArs = cotizacionUsdArs;
    }
}
