package com.futuratecno.api.dto;

import java.math.BigDecimal;

/**
 * Un componente elegible en "Armá tu PC": una variante con precio y los datos de compatibilidad
 * que se pudieron leer de su ficha. Cualquiera de esos datos puede venir en null = "no se sabe";
 * el frontend lo muestra con aviso en vez de ocultarlo.
 */
public record ComponentePcDTO(
        Long productoId,
        Long varianteId,
        String tipo,
        String marca,
        String modelo,
        String sku,
        String especificaciones,
        String imagenUrl,
        BigDecimal precioUsd,
        BigDecimal precioArs,
        String socket,
        String tipoRam,
        String formato,
        Integer ranurasRam,
        Integer modulos,
        Integer potenciaW,
        Integer fuenteRecomendadaW,
        Boolean videoIntegrado,
        Boolean incluyeCooler,
        Integer gama) {
}
