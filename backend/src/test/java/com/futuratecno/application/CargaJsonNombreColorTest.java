package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Al crear un producto, el color de la ficha se agrega al nombre solo si el nombre no lo dice. */
class CargaJsonNombreColorTest {

    private final IdentidadProductoService identidad = new IdentidadProductoService();

    private String nombre(String modelo, String color) {
        var r = identidad.resolver("Motorola", modelo,
                Map.of("ram", "8GB", "almacenamiento", "256GB", "color", color), "Celulares");
        return CargaJsonService.nombreAlCrear(modelo, r, color);
    }

    @Test
    void noRepiteUnColorQueElNombreYaDice() {
        assertEquals("Edge 60 Pro 5G 8GB 256GB Walnut", nombre("Edge 60 Pro 5G 8GB 256GB Walnut", "Walnut"));
        assertEquals("Hot 60 Pro 8GB 256GB Jungle Breath", nombre("Hot 60 Pro 8GB 256GB Jungle Breath", "Jungle Breath"));
        assertEquals("Galaxy S26 FE 8GB 256GB Blueberry", nombre("Galaxy S26 FE 8GB 256GB Blueberry", "blueberry"));
    }

    @Test
    void agregaElColorSiElNombreNoLoDice() {
        assertEquals("Edge 60 Pro 5G 8GB 256GB Walnut", nombre("Edge 60 Pro 5G 8GB 256GB", "Walnut"));
    }

    @Test
    void yaDiceComparaPalabrasCompletas() {
        assertTrue(CargaJsonService.yaDice("iPhone 18 Pro 256GB Glacier", "glacier"));
        assertFalse(CargaJsonService.yaDice("Galaxy Blueberry2", "Blueberry"));
        assertFalse(CargaJsonService.yaDice("Note 15 Pro 256GB", "Pro Max"));
    }
}
