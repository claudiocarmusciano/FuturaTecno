package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiEsperaTest {

    @Test
    void leeLaEsperaDelLimitePorMinuto() {
        String cuerpo = "{\"error\":{\"code\":429,\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\": \"23s\"}]}}";
        assertEquals(24, GenerarListadoService.esperaGemini(cuerpo));
        assertEquals(8, GenerarListadoService.esperaGemini("{\"retryDelay\":\"7.5s\"}"));
    }

    @Test
    void sinEsperaNoReintenta() {
        assertEquals(-1, GenerarListadoService.esperaGemini("{\"error\":{\"code\":429}}"));
        assertEquals(-1, GenerarListadoService.esperaGemini(null));
    }
}
