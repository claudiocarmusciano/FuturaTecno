package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvidStockTest {

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
}
