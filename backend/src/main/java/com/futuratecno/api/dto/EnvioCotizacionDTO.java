package com.futuratecno.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de cotizar un envío. Si {@code disponible} es false, {@code mensaje} explica por qué
 * (servicio no configurado, productos sin peso/dimensiones, API caída) y el checkout sigue sin
 * costo de envío — la cotización es un extra, nunca un bloqueo.
 */
public class EnvioCotizacionDTO {
    private boolean disponible;
    private String mensaje;
    private List<OpcionEnvio> opciones = new ArrayList<>();

    public static EnvioCotizacionDTO noDisponible(String mensaje) {
        EnvioCotizacionDTO dto = new EnvioCotizacionDTO();
        dto.disponible = false;
        dto.mensaje = mensaje;
        return dto;
    }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public List<OpcionEnvio> getOpciones() { return opciones; }
    public void setOpciones(List<OpcionEnvio> opciones) { this.opciones = opciones; }

    /** Una modalidad cotizada: el código tal como lo da Andreani ("estándar", "sucursal", ...). */
    public static class OpcionEnvio {
        private String codigo;
        private BigDecimal totalArs;

        public OpcionEnvio() {}

        public OpcionEnvio(String codigo, BigDecimal totalArs) {
            this.codigo = codigo;
            this.totalArs = totalArs;
        }

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }

        public BigDecimal getTotalArs() { return totalArs; }
        public void setTotalArs(BigDecimal totalArs) { this.totalArs = totalArs; }
    }
}
