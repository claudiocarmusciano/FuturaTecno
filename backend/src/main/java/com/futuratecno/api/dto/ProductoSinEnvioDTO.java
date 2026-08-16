package com.futuratecno.api.dto;

/** Producto activo al que no se le puede cotizar Andreani por faltar medidas resolubles. */
public record ProductoSinEnvioDTO(Long productoId, String producto, String categoria,
                                  Long categoriaId, String faltan) {
}
