package com.futuratecno.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigControllerTest {

    @Test
    void publicaLosIdsDeAnaliticaSoloSiTienenElFormatoEsperado() {
        var ok = new ConfigController("cid", " G-AB12CD34EF ", "1234567890123456").config();
        assertEquals("G-AB12CD34EF", ok.get("ga4MeasurementId"));
        assertEquals("1234567890123456", ok.get("metaPixelId"));

        // Un valor mal cargado no llega a la URL del script: queda sin medir.
        var mal = new ConfigController("cid", "G-X\"><script>", "12345abc").config();
        assertEquals("", mal.get("ga4MeasurementId"));
        assertEquals("", mal.get("metaPixelId"));

        var vacio = new ConfigController(null, null, null).config();
        assertEquals("", vacio.get("ga4MeasurementId"));
        assertEquals("", vacio.get("googleClientId"));
    }
}
