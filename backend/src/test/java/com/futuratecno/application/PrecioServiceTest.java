package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrecioServiceTest {

    @Test
    void recuperaElPrecioBaseLuegoDeComisionInmediataEIva() {
        PrecioService service = new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"));

        assertEquals(new BigDecimal("899382.18"),
                service.precioMercadoPagoInmediato(new BigDecimal("830931.10")));
    }
}
