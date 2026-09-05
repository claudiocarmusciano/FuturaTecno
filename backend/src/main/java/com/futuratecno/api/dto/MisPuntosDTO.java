package com.futuratecno.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Saldo y movimientos del programa de beneficios del usuario autenticado. */
public class MisPuntosDTO {
    private int puntosDisponibles;
    private LocalDateTime proximoVencimiento;
    private List<Movimiento> movimientos = new ArrayList<>();

    public int getPuntosDisponibles() { return puntosDisponibles; }
    public void setPuntosDisponibles(int puntosDisponibles) { this.puntosDisponibles = puntosDisponibles; }
    public LocalDateTime getProximoVencimiento() { return proximoVencimiento; }
    public void setProximoVencimiento(LocalDateTime proximoVencimiento) { this.proximoVencimiento = proximoVencimiento; }
    public List<Movimiento> getMovimientos() { return movimientos; }
    public void setMovimientos(List<Movimiento> movimientos) { this.movimientos = movimientos; }

    public static class Movimiento {
        private String tipo;
        private int puntos;
        private String detalle;
        private LocalDateTime fecha;
        private LocalDateTime venceEn;
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public int getPuntos() { return puntos; }
        public void setPuntos(int puntos) { this.puntos = puntos; }
        public String getDetalle() { return detalle; }
        public void setDetalle(String detalle) { this.detalle = detalle; }
        public LocalDateTime getFecha() { return fecha; }
        public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
        public LocalDateTime getVenceEn() { return venceEn; }
        public void setVenceEn(LocalDateTime venceEn) { this.venceEn = venceEn; }
    }
}
