package com.futuratecno.application;

import java.time.Duration;

/**
 * Invid cortó por rate limit (50 consultas/hora) y dijo cuánto esperar.
 *
 * <p>Existe como excepción propia y no como un `IllegalStateException` con el texto adentro porque
 * el scheduler necesita el número, no el mensaje: reintentar leyendo minutos de un string sería
 * frágil ante cualquier cambio de redacción. Sigue siendo un `IllegalStateException` para que
 * `CargaJsonController` y el panel la traten igual que antes — el admin ve el mismo error.
 */
public class InvidRateLimitException extends IllegalStateException {

    /** Lo que pidió esperar el header Retry-After. Null si no lo mandó o no era numérico. */
    private final transient Duration espera;

    public InvidRateLimitException(String mensaje, Duration espera) {
        super(mensaje);
        this.espera = espera;
    }

    public Duration getEspera() {
        return espera;
    }
}
