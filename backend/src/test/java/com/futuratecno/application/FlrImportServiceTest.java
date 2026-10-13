package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlrImportServiceTest {

    @Test
    void tomaElPrecioDeUnaUnidadYLeeLasComillas() {
        List<List<String>> filas = FlrImportService.parsearCsv(
                "CELULAR SAMSUNG S25 ULTRA 12/256GB BLACK,ANDROID,,805,Disponible,800,795,790\n"
                + "\"APPLE MACBOOK AIR 13\"\" M1 8/256GB\",APPLE PRODUCTS,,Sin precio,Disponible,,,\r\n");
        assertEquals(2, filas.size());
        assertEquals("805", filas.get(0).get(3));
        assertEquals("APPLE MACBOOK AIR 13\" M1 8/256GB", filas.get(1).get(0));
        assertEquals(8, filas.get(1).size());
    }

    @Test
    void precios() {
        assertEquals(new BigDecimal("805"), FlrImportService.precio("805"));
        assertEquals(new BigDecimal("805"), FlrImportService.precio("$805"));
        assertEquals(new BigDecimal("1030"), FlrImportService.precio("1.030"));
        assertEquals(new BigDecimal("99.5"), FlrImportService.precio("99,5"));
        assertNull(FlrImportService.precio("Sin precio"));
        assertNull(FlrImportService.precio(""));
    }

    @Test
    void marcaYModelo() {
        ArticuloJsonDTO s = FlrImportService.articulo("CELULAR SAMSUNG S25 ULTRA 12/256GB BLACK", "ANDROID", new BigDecimal("805"));
        assertEquals("Samsung", s.getMarca());
        assertEquals("S25 Ultra 12/256GB Black", s.getModelo());

        ArticuloJsonDTO i = FlrImportService.articulo("IPHONE 17 PRO 256GB ORANGE", "IPHONE", new BigDecimal("1170"));
        assertEquals("Apple", i.getMarca());
        assertEquals("iPhone 17 Pro 256GB Orange", i.getModelo());

        ArticuloJsonDTO a = FlrImportService.articulo("APPLE PENCIL USB-C", "APPLE PRODUCTS", new BigDecimal("100"));
        assertEquals("Apple", a.getMarca());
        assertEquals("Pencil USB-C", a.getModelo());

        ArticuloJsonDTO h = FlrImportService.articulo("PARLANTE SPEAKER HARMAN KARDON GO + PLAY 3 BT WHITE", "AUDIO", new BigDecimal("210"));
        assertEquals("Harman Kardon", h.getMarca());
        assertEquals("Parlante Go + Play 3 BT White", h.getModelo());

        ArticuloJsonDTO c = FlrImportService.articulo("CONSOLA PORTÁTIL R35S 64GB", "CONSOLAS", new BigDecimal("50"));
        assertEquals("R35S", c.getMarca());
        assertEquals("Consola Portátil 64GB", c.getModelo());

        ArticuloJsonDTO sb = FlrImportService.articulo("HOME THEATER - SOUNDBAR SAMSUNG HW-C400 USB/BT/NFC/40W/BLACK", "AUDIO", new BigDecimal("150"));
        assertEquals("Samsung", sb.getMarca());
        assertEquals("Soundbar HW-C400 USB/BT/NFC/40W/BLACK", sb.getModelo());
    }
}
