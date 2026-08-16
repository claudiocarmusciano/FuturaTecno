package com.futuratecno.api.dto;

public record OnboardingStatusDTO(boolean whatsappVerificado, boolean emailVerificado,
                                  boolean whatsappAgendado, boolean instagramCompletado,
                                  boolean instagramVerificado, String whatsappVerificacionCodigo,
                                  String codigoSorteo, String instagramUsuario) {
    public boolean pasoUnoCompleto() { return emailVerificado; }
    public boolean completo() { return pasoUnoCompleto() && whatsappVerificado && instagramVerificado; }
}
