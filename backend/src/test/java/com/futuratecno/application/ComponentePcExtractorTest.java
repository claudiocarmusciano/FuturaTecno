package com.futuratecno.application;

import com.futuratecno.application.ComponentePcExtractor.Tipo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Casos sacados del catálogo real de Elit e Invid (2026-09-22). */
class ComponentePcExtractorTest {

    @Test
    void elTipoSaleDelNombreAntesQueDeLaCategoria() {
        // "Coolers > Fans" tiene fuentes y gabinetes de Cooler Master adentro.
        assertEquals(Tipo.FUENTE, ComponentePcExtractor.tipo("Fuente Cooler Master Elite Gold 750W Full Mod", "Coolers", "Fans"));
        assertEquals(Tipo.GABINETE, ComponentePcExtractor.tipo("Gabinete Cooler Master Cosmos Alpha Full Tower", "Coolers", "Fans"));
        assertEquals(Tipo.COOLER, ComponentePcExtractor.tipo("Air Cooler Cooler Master Hyper 212 Spectrum V3", "Coolers", "Fans"));
        // "CPU Cooler" es un cooler, no un procesador.
        assertEquals(Tipo.COOLER, ComponentePcExtractor.tipo("CPU Cooler MSI MAG COREFROZR AA13 Black (5743)", "Microprocesadores", "AMD"));
        // Un ventilador de gabinete no entra al armador.
        assertNull(ComponentePcExtractor.tipo("Fan Cooler Master MF120 Lite ARGB 3-Pack", "Coolers", "Fans"));
    }

    @Test
    void fueraDeLasCategoriasDeComponentesNoEntraNada() {
        assertNull(ComponentePcExtractor.tipo("AIO Lenovo IdeaCentre 24", "Computadoras", "All in One"));
        assertNull(ComponentePcExtractor.tipo("Memoria Kingston 8GB DDR4 Sodimm", "Memorias RAM", "Memoria Sodimm"));
        assertNull(ComponentePcExtractor.tipo("Disco Externo WD 2TB", "Discos Rígidos / SSD", "Disco Rígido Externo"));
        // Una SD categorizada como DDR4 en el catálogo real.
        assertNull(ComponentePcExtractor.tipo("Tarjeta de Memoria BIWIN microSD 128GB", "Memorias RAM", "Memoria DDR4"));
        assertNull(ComponentePcExtractor.tipo("Extreme Portable SSD 1TB", "Discos Rígidos / SSD", "Disco SSD"));
        assertEquals(Tipo.COOLER, ComponentePcExtractor.tipo("Water Cooling Corsair iCUE H100i", "Coolers", "Watercoolers"));
    }

    @Test
    void socketExplicitoPorChipsetYPorGeneracion() {
        assertEquals("AM5", ComponentePcExtractor.socket(Tipo.PROCESADOR, "Proces. AMD Ryzen 7 9700X AM5 (8 Core)", null));
        assertEquals("LGA1700", ComponentePcExtractor.socket(Tipo.PROCESADOR, "Proces. Intel Core I7-14700 Raptorlake R  S1700 (9239)", null));
        assertEquals("LGA1700", ComponentePcExtractor.socket(Tipo.PROCESADOR, "Procesador Core i9-14900 2.0GHz 36MB LGA 1700", null));
        // Sin socket escrito: lo fija la generación o el chipset.
        assertEquals("AM4", ComponentePcExtractor.socket(Tipo.PROCESADOR, "Procesador AMD Ryzen 5 5600X", null));
        assertEquals("LGA1851", ComponentePcExtractor.socket(Tipo.PROCESADOR, "Procesador Intel Core Ultra 7 265K", null));
        assertEquals("AM5", ComponentePcExtractor.socket(Tipo.MOTHER, "Motherboard ASUS TUF GAMING B650M-PLUS WIFI", null));
        assertEquals("LGA1700", ComponentePcExtractor.socket(Tipo.MOTHER, "Mother Gigabyte H610M K DDR4", null));
        // Un código de stock entre paréntesis no es un socket.
        assertNull(ComponentePcExtractor.socket(Tipo.MOTHER, "Mother genérico (1700)", null));
    }

    @Test
    void tipoDeRam() {
        assertEquals("DDR4", ComponentePcExtractor.tipoRam(Tipo.MOTHER, "Mother Gigabyte H610M K DDR4", null, null, "LGA1700"));
        assertEquals("DDR4", ComponentePcExtractor.tipoRam(Tipo.MOTHER, "Motherboard ASUS PRIME B760M-A D4", null, null, "LGA1700"));
        // AM5 es siempre DDR5; LGA1700 salió en las dos versiones y no se adivina.
        assertEquals("DDR5", ComponentePcExtractor.tipoRam(Tipo.MOTHER, "Motherboard MSI B650 Gaming Plus", null, null, "AM5"));
        assertNull(ComponentePcExtractor.tipoRam(Tipo.MOTHER, "Motherboard ASUS PRIME B760M-A AX6 II", null, null, "LGA1700"));
        assertNull(ComponentePcExtractor.tipoRam(Tipo.MOTHER, "Mother B760", "Soporta DDR4 y DDR5 según versión", null, "LGA1700"));
        assertEquals("DDR5", ComponentePcExtractor.tipoRam(Tipo.MEMORIA, "Memoria Kingston Fury 16GB", null, "Memoria DDR5", null));
    }

    @Test
    void formatoDeMotherYGabinete() {
        assertEquals("MATX", ComponentePcExtractor.formato(Tipo.MOTHER, "Motherboard ASUS TUF GAMING B650M-PLUS WIFI", null));
        assertEquals("MATX", ComponentePcExtractor.formato(Tipo.MOTHER, "Mother Gigabyte X870M A ELITE WF7 ICE DDR5 AM5", null));
        assertEquals("ITX", ComponentePcExtractor.formato(Tipo.MOTHER, "Mother ASRock B650I Lightning WiFi", null));
        assertEquals("ATX", ComponentePcExtractor.formato(Tipo.MOTHER, "Motherboard MSI MAG B650 TOMAHAWK ATX", null));
        // El gabinete vale por lo más grande que admite.
        assertEquals("ATX", ComponentePcExtractor.formato(Tipo.GABINETE, "Gabinete Lian Li", "Soporta ATX / Micro-ATX / Mini-ITX"));
        assertEquals("MATX", ComponentePcExtractor.formato(Tipo.GABINETE, "Gabinete Cooler Master Elite 461 Mini Tower", null));
        assertTrue(ComponentePcExtractor.entra("MATX", "ATX"));
        assertFalse(ComponentePcExtractor.entra("ATX", "MATX"));
    }

    @Test
    void watts() {
        assertEquals(750, ComponentePcExtractor.potenciaW("Fuente Cooler Master Elite Gold 750W Full Mod", null));
        assertEquals(250, ComponentePcExtractor.potenciaW("Fuente Teros TE-1330S 250W ATX 3.1", null));
        assertEquals(750, ComponentePcExtractor.fuenteRecomendadaW("Placa MSI NVIDIA GeForce  5070 Ti 16G", null));
        assertEquals(650, ComponentePcExtractor.fuenteRecomendadaW("VGA ASUS RTX5070 12GB", null));
        assertEquals(750, ComponentePcExtractor.fuenteRecomendadaW("VGA Sapphire RX 9070XT Pulse", null));
        assertNull(ComponentePcExtractor.fuenteRecomendadaW("VGA PNY QUADRO RTX A1000 8Gb", null));
    }

    @Test
    void videoIntegradoYCooler() {
        assertFalse(ComponentePcExtractor.videoIntegrado("Proces. AMD Ryzen 7 5700X SIN VIDEO SIN COOLER", null));
        assertTrue(ComponentePcExtractor.videoIntegrado("Procesador AMD Ryzen 5 8600G", null));
        assertTrue(ComponentePcExtractor.videoIntegrado("Procesador AMD Ryzen 5 7600", null));
        assertFalse(ComponentePcExtractor.videoIntegrado("Procesador AMD Ryzen 5 5600X", null));
        assertFalse(ComponentePcExtractor.videoIntegrado("Procesador Intel Core i5-12400F", null));
        assertTrue(ComponentePcExtractor.videoIntegrado("Procesador INTEL Pentium Gold G7400 3.70GHz", null));

        assertTrue(ComponentePcExtractor.incluyeCooler("Proces. AMD Ryzen 5 7600 AM5 CON VIDEO CON COOLER", null));
        assertFalse(ComponentePcExtractor.incluyeCooler("Procesador AMD Ryzen 7 7800X3D", null));
        assertFalse(ComponentePcExtractor.incluyeCooler("Procesador Intel Core i7-14700K", null));
        assertNull(ComponentePcExtractor.incluyeCooler("Procesador Core i5-14400 2.5GHz", null));
    }

    @Test
    void ranurasDeMemoriaYModulosPorKit() {
        assertEquals(2, ComponentePcExtractor.ranurasRam("Motherboard ASUS PRIME A620M-K AM5 DDR5",
                "Tipo de memoria RAM: DDR5. Cantidad de slots de memoria RAM: 2. Versión de PCI express", "MATX"));
        assertEquals(4, ComponentePcExtractor.ranurasRam("Mother MSI B650 Gaming", "4 x DDR5 DIMM", "ATX"));
        // Sin dato: ATX e ITX se deducen; Micro-ATX sale con 2 y con 4, no se adivina.
        assertEquals(4, ComponentePcExtractor.ranurasRam("Motherboard MSI PRO Z790-P WIFI", null, "ATX"));
        assertEquals(2, ComponentePcExtractor.ranurasRam("Mother ASRock B650I Lightning", null, "ITX"));
        assertNull(ComponentePcExtractor.ranurasRam("Motherboard GIGABYTE B550M K", null, "MATX"));

        assertEquals(2, ComponentePcExtractor.modulosPorUnidad("Memoria Kingston Fury Beast 32GB (2x16GB) DDR5"));
        assertEquals(1, ComponentePcExtractor.modulosPorUnidad("Memoria Ram UDIMM ADATA 8GB DDR5 5600MHz"));
    }
}
