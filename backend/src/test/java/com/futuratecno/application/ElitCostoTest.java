package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ElitCostoTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void sumaIvaEImpuestoInternoComoElPvpDeElit() throws Exception {
        // Monitor ASUS VY229HF-J real: Elit informa pvp_usd 94,71.
        assertThat(ElitImportService.costoUsdDe(json.readTree(
                "{\"precio\":72.02,\"iva\":21,\"impuesto_interno\":10.5}"))).isEqualByComparingTo(new BigDecimal("94.71"));
    }

    @Test
    void sinImpuestoInternoEsPrecioMasIva() throws Exception {
        assertThat(ElitImportService.costoUsdDe(json.readTree(
                "{\"precio\":856.22,\"iva\":10.5,\"impuesto_interno\":0}"))).isEqualByComparingTo(new BigDecimal("946.12"));
        assertThat(ElitImportService.costoUsdDe(json.readTree(
                "{\"precio\":856.22,\"iva\":10.5}"))).isEqualByComparingTo(new BigDecimal("946.12"));
    }
}
