package com.futuratecno.api.dto;

public record OnboardingStatusDTO(boolean whatsappVerificado, boolean emailVerificado,
                                  boolean whatsappAgendado, boolean instagramCompletado) {
    public boolean pasoUnoCompleto() { return whatsappVerificado && emailVerificado; }
    public boolean completo() { return pasoUnoCompleto() && whatsappAgendado && instagramCompletado; }
}
