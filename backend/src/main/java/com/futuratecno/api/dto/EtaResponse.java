package com.futuratecno.api.dto;

import java.time.LocalDate;

public class EtaResponse {
    private LocalDate fechaEntrega;
    private int diasHabiles;
    private int horaCorte;
    private boolean antesDeCorte;

    public EtaResponse() {}

    public EtaResponse(LocalDate fechaEntrega, int diasHabiles, int horaCorte, boolean antesDeCorte) {
        this.fechaEntrega = fechaEntrega;
        this.diasHabiles = diasHabiles;
        this.horaCorte = horaCorte;
        this.antesDeCorte = antesDeCorte;
    }

    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public int getDiasHabiles() { return diasHabiles; }
    public void setDiasHabiles(int diasHabiles) { this.diasHabiles = diasHabiles; }

    public int getHoraCorte() { return horaCorte; }
    public void setHoraCorte(int horaCorte) { this.horaCorte = horaCorte; }

    public boolean isAntesDeCorte() { return antesDeCorte; }
    public void setAntesDeCorte(boolean antesDeCorte) { this.antesDeCorte = antesDeCorte; }
}
