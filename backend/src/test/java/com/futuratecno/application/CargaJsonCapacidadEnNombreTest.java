package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargaJsonCapacidadEnNombreTest {

    @Test
    void agregaAlNombreLaRamYElDiscoQueSoloEstanEnLaFicha() {
        assertEquals("16GB 1TB", CargaJsonService.capacidadesFaltantes("250 G10 CORE i7-1355U",
                Map.of("ram", "16GB", "almacenamiento", "1TB SSD", "procesador", "Intel Core i7-1355U")));
        assertEquals("512GB", CargaJsonService.capacidadesFaltantes("ThinkBook 16 G8 16GB", Map.of("ram", "16 GB", "almacenamiento", "512GB")));
    }

    @Test
    void noRepiteLoQueElNombreYaDice() {
        assertEquals("", CargaJsonService.capacidadesFaltantes("MacBook Air 13 M5 16GB 512GB", Map.of("ram", "16GB", "almacenamiento", "512GB")));
        // La forma de los listados: "16/512".
        assertEquals("", CargaJsonService.capacidadesFaltantes("Galaxy Tab S10 FE 8/128", Map.of("ram", "8GB", "almacenamiento", "128GB")));
        assertEquals("", CargaJsonService.capacidadesFaltantes("Vivobook 15", Map.of("procesador", "Core 7")));
    }

    @Test
    void elTamanioDePantallaNoCuentaComoRam() {
        assertEquals("16GB", CargaJsonService.capacidadesFaltantes("MacBook Pro 16 1TB", Map.of("ram", "16GB", "almacenamiento", "1TB")));
        // "128GB" no dice "8GB": el número tiene que estar solo.
        assertEquals("8GB", CargaJsonService.capacidadesFaltantes("Galaxy Tab A11 128GB", Map.of("ram", "8GB", "almacenamiento", "128GB")));
    }

    @Test
    void laPlacaDeVideoDedicadaTambienVaAlNombre() {
        assertEquals("RTX 5070 Ti", CargaJsonService.gpu("NVIDIA GeForce RTX 5070 Ti 12GB"));
        assertEquals("RTX 5080", CargaJsonService.gpu("RTX 5080 16GB"));
        assertEquals("RX 7600S", CargaJsonService.gpu("AMD Radeon RX 7600S"));
        // Integradas no: no distinguen y alargan el nombre.
        assertEquals(null, CargaJsonService.gpu("Intel Iris Xe Graphics"));
        assertEquals("RTX 5080", CargaJsonService.gpuFaltante("Raider 16 Max 32GB 1TB", Map.of("gpu", "RTX 5080 16GB")));
        assertEquals("", CargaJsonService.gpuFaltante("Raider 16 Max RTX 5080", Map.of("gpu", "NVIDIA RTX 5080")));
        // Una 5070 Ti en el nombre no dice 5070 ni al revés.
        assertEquals("RTX 5070", CargaJsonService.gpuFaltante("Katana 15 RTX 5070 Ti", Map.of("gpu", "RTX 5070")));
    }

    @Test
    void elNombreSinLaPlacaEsLoQueVieneAntes() {
        assertEquals("LOQ Essential Core i7-13650HX 16GB 512GB", CargaJsonService.antesDeLaPlaca("LOQ Essential Core i7-13650HX 16GB 512GB RTX 5060"));
        assertEquals("Raider 16 Max 32GB 1TB", CargaJsonService.antesDeLaPlaca("Raider 16 Max 32GB 1TB - RTX 5070 Ti 12GB 240Hz"));
        assertEquals(null, CargaJsonService.antesDeLaPlaca("Vivobook 15 16GB 512GB"));
    }

    @Test
    void laFichaGuardadaTieneQueTraerLaMismaRamYElMismoDisco() {
        String ficha = "Intel Core i7-1355U · 8GB · 256GB · 15.6”";
        assertTrue(CargaJsonService.fichaConCapacidades(ficha, Map.of("ram", "8GB", "almacenamiento", "256GB")));
        assertFalse(CargaJsonService.fichaConCapacidades(ficha, Map.of("ram", "16GB", "almacenamiento", "1TB")));
        // "128GB" no es "8GB" aunque lo contenga.
        assertFalse(CargaJsonService.fichaConCapacidades("Octa-core · 128GB", Map.of("ram", "8GB")));
        // Y la misma placa, si la carga la dice.
        String gamer = "Core Ultra 9 · 32GB · 1TB · NVIDIA RTX 5070 Ti 12GB";
        assertTrue(CargaJsonService.fichaConCapacidades(gamer, Map.of("ram", "32GB", "almacenamiento", "1TB", "gpu", "RTX 5070 Ti")));
        assertFalse(CargaJsonService.fichaConCapacidades(gamer, Map.of("ram", "32GB", "almacenamiento", "1TB", "gpu", "RTX 5080")));
        assertFalse(CargaJsonService.fichaConCapacidades(gamer, Map.of("ram", "32GB", "almacenamiento", "1TB", "gpu", "RTX 5070")));
        // Sin capacidades para comparar no se afirma nada.
        assertFalse(CargaJsonService.fichaConCapacidades(ficha, Map.of("procesador", "i7")));
    }
}
