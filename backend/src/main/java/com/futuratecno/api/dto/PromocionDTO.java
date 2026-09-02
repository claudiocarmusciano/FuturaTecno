package com.futuratecno.api.dto;

import java.time.LocalDateTime;

public record PromocionDTO(Long id, String titulo, String texto, String enlace, Integer orden,
                           LocalDateTime fechaInicio, LocalDateTime fechaFin, boolean activo,
                           String imagenEscritorioUrl, String imagenMovilUrl) {}
