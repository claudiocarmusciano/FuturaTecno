package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrecioServiceTest {

    @Test
    void recuperaElPrecioBaseLuegoDeComisionInmediataEIva() {
        PrecioService service = new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"), new BigDecimal("7"));

        assertEquals(new BigDecimal("899382.18"),
                service.precioMercadoPagoInmediato(new BigDecimal("830931.10")));
    }

    @Test
    void aplicaSietePorCientoDeDescuentoAlContado() {
        PrecioService service = new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"), new BigDecimal("7"));

        assertEquals(new BigDecimal("930.00"), service.precioContadoEfectivo(new BigDecimal("1000.00")));
    }
}
