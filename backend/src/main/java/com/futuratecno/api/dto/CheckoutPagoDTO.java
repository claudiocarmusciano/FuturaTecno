package com.futuratecno.api.dto;

import java.math.BigDecimal;

/** URL segura de Checkout Pro creada para un pedido propio. */
public class CheckoutPagoDTO {
    private String checkoutUrl;
    private String preferenceId;
    private BigDecimal montoArs;
    private boolean prueba;

    public CheckoutPagoDTO() {}

    public CheckoutPagoDTO(String checkoutUrl, String preferenceId, BigDecimal montoArs, boolean prueba) {
        this.checkoutUrl = checkoutUrl;
        this.preferenceId = preferenceId;
        this.montoArs = montoArs;
        this.prueba = prueba;
    }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    public String getPreferenceId() { return preferenceId; }
    public void setPreferenceId(String preferenceId) { this.preferenceId = preferenceId; }
    public BigDecimal getMontoArs() { return montoArs; }
    public void setMontoArs(BigDecimal montoArs) { this.montoArs = montoArs; }
    public boolean isPrueba() { return prueba; }
    public void setPrueba(boolean prueba) { this.prueba = prueba; }
}
