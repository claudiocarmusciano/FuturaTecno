package com.futuratecno.api.dto;

import java.math.BigDecimal;

/** Variante en formato editable para el admin (precio en su moneda de origen). */
public class VarianteEditDTO {
    private Long id;
    private String especificaciones;
    private String moneda;          // "USD" o "ARS"
    private BigDecimal precio;      // precio en la moneda de origen
    private Integer stock;

    public VarianteEditDTO() {}

    public VarianteEditDTO(Long id, String especificaciones, String moneda, BigDecimal precio, Integer stock) {
        this.id = id;
        this.especificaciones = especificaciones;
        this.moneda = moneda;
        this.precio = precio;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(String especificaciones) { this.especificaciones = especificaciones; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
