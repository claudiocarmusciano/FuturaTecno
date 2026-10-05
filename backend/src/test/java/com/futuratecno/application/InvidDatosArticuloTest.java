package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvidDatosArticuloTest {

    private final ObjectMapper json = new ObjectMapper();

    private Integer stock(String art) throws Exception {
        return InvidImportService.stock(json.readTree(art));
    }

    @Test
    void traduceElEstadoQueMandaInvid() throws Exception {
        assertThat(stock("{\"STOCK_STATUS\":\"Disponible\"}")).isEqualTo(10);
        assertThat(stock("{\"STOCK_STATUS\":\"Menos de 10 unidades\"}")).isEqualTo(5);
        assertThat(stock("{\"STOCK_STATUS\":\"Stock bajo\"}")).isEqualTo(2);
        assertThat(stock("{\"STOCK_STATUS\":\"Sin stock\"}")).isZero();
        assertThat(stock("{\"STOCK_STATUS\":\"No disponible\"}")).isZero();
    }

    @Test
    void laCantidadRealGanaYLoDesconocidoQuedaNull() throws Exception {
        assertThat(stock("{\"STOCK\":7,\"STOCK_STATUS\":\"Disponible\"}")).isEqualTo(7);
        assertThat(stock("{\"STOCK\":\"12\"}")).isEqualTo(12);
        assertThat(stock("{}")).isNull();
        assertThat(stock("{\"STOCK_STATUS\":\"Consultar\"}")).isNull();
    }

    @Test
    void medidasEnKgYCmRedondeadasHaciaArriba() throws Exception {
        int[] m = InvidImportService.medidas(json.readTree(
                "{\"WEIGHT\":\"2.29\",\"HEIGHT\":\"22.000\",\"WIDTH\":\"8.000\",\"LENGTH\":\"53.500\",\"DIMENSIONS_UNIT\":\"Cm\",\"WEIGHT_UNIT\":\"Kg\"}"));
        assertThat(m).containsExactly(2290, 22, 8, 54);
        assertThat(InvidImportService.medidas(json.readTree(
                "{\"WEIGHT\":0.09,\"HEIGHT\":110,\"WIDTH\":75,\"LENGTH\":190,\"DIMENSIONS_UNIT\":\"Mm\",\"WEIGHT_UNIT\":\"Kg\"}")))
                .containsExactly(90, 11, 8, 19);
    }

    @Test
    void sinDatoONoCreibleNoDevuelveMedidas() throws Exception {
        assertThat(InvidImportService.medidas(json.readTree("{\"WEIGHT\":\"1\",\"HEIGHT\":\"10\",\"WIDTH\":\"10\"}"))).isNull();
        assertThat(InvidImportService.medidas(json.readTree("{\"WEIGHT\":\"0\",\"HEIGHT\":\"10\",\"WIDTH\":\"10\",\"LENGTH\":\"10\"}"))).isNull();
        assertThat(InvidImportService.medidas(json.readTree("{\"WEIGHT\":\"1\",\"HEIGHT\":\"900\",\"WIDTH\":\"10\",\"LENGTH\":\"10\"}"))).isNull();
    }
}
