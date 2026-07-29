package com.futuratecno.domain;

/**
 * Estados de un pedido. Se persisten como texto (@Enumerated(STRING)) y están replicados en el
 * CHECK de la tabla pedidos (V13): agregar un estado acá exige una migración que amplíe el CHECK.
 */
public enum EstadoPedido {
    /** Recién creado por el cliente. Vale hasta el corte de las 06:30 AR. */
    PENDIENTE,
    /** El negocio se contactó y confirmó precio y disponibilidad. */
    CONFIRMADO,
    /** Entregado y cerrado. */
    ENTREGADO,
    /** Cancelado a mano (por el cliente o por el negocio). */
    CANCELADO,
    /** Pasó el corte sin cerrarse: los precios y el stock ya no son los que vio el cliente. */
    VENCIDO
}
