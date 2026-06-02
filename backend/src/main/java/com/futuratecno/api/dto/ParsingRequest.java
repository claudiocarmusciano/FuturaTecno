package com.futuratecno.api.dto;

public class ParsingRequest {
    private String texto;
    private Long proveedorId;

    public ParsingRequest() {}

    public ParsingRequest(String texto, Long proveedorId) {
        this.texto = texto;
        this.proveedorId = proveedorId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }
}
