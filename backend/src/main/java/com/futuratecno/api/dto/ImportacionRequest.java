package com.futuratecno.api.dto;

import java.util.List;

public class ImportacionRequest {
    private Long proveedorId;
    private List<ProductoImportar> productos;

    public ImportacionRequest() {}

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public List<ProductoImportar> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoImportar> productos) {
        this.productos = productos;
    }
}
