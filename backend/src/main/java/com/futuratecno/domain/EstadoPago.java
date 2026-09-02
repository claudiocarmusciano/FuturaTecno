package com.futuratecno.domain;

/** Estado financiero del pedido, separado de su avance operativo. */
public enum EstadoPago {
    SIN_INICIAR,
    PENDIENTE,
    EN_PROCESO,
    APROBADO,
    RECHAZADO,
    CANCELADO,
    REEMBOLSADO,
    CONTRACARGO
}
