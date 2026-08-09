package com.futuratecno.api.dto;

public record OnboardingStatusDTO(boolean whatsappVerificado, boolean emailVerificado,
                                  boolean whatsappAgendado, boolean instagramCompletado,
                                  String whatsappVerificacionCodigo) {
    public boolean pasoUnoCompleto() { return emailVerificado; }
    public boolean completo() { return pasoUnoCompleto() && whatsappVerificado && instagramCompletado; }
}
