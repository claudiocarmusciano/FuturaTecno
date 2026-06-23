package com.futuratecno.api.dto;

import java.math.BigDecimal;

public class VarianteCatalogoDTO {
    private Long id;
    private String especificaciones;
    private BigDecimal precioUsd;
    private BigDecimal precioArs;

    public VarianteCatalogoDTO() {}

    public VarianteCatalogoDTO(Long id, String especificaciones, BigDecimal precioUsd, BigDecimal precioArs) {
        this.id = id;
        this.especificaciones = especificaciones;
        this.precioUsd = precioUsd;
        this.precioArs = precioArs;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(String especificaciones) { this.especificaciones = especificaciones; }

    public BigDecimal getPrecioUsd() { return precioUsd; }
    public void setPrecioUsd(BigDecimal precioUsd) { this.precioUsd = precioUsd; }

    public BigDecimal getPrecioArs() { return precioArs; }
    public void setPrecioArs(BigDecimal precioArs) { this.precioArs = precioArs; }
}
