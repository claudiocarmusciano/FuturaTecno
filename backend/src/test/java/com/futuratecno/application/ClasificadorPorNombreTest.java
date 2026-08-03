package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Casos tomados del catálogo real de producción (2026-07-31). Los que llevan comentario son los
 * que efectivamente se clasificaban mal antes y motivaron una regla: sirven de red de contención
 * si alguien reordena las reglas, porque el orden ES la prioridad.
 */
class ClasificadorPorNombreTest {

    private final ClasificadorPorNombre clasificador = new ClasificadorPorNombre();

    private String clasificar(String marca, String modelo) {
        Producto p = new Producto();
        p.setMarca(marca);
        p.setModelo(modelo);
        return clasificador.clasificar(p);
    }

    @Test
    @DisplayName("Las especificaciones del nombre no deben mandar sobre el tipo de producto")
    void especificacionesNoPisanElTipo() {
        // El caso que rompía todo: un procesador que viene con cooler no es un cooler.
        assertEquals("Microprocesadores > AMD",
                clasificar("AMD", "Procesador AMD Ryzen 5 5600X con Cooler Wraith"));
        // Una notebook con memoria SODIMM no es una memoria.
        assertEquals("Notebooks > Consumo",
                clasificar("LENOVO", "Notebook Lenovo ThinkBook 16, CORE 5, 16GB SODIMM"));
        // Una PC armada que menciona su SSD no es un disco.
        assertEquals("Computadoras > PC",
                clasificar("KELYX", "PC Kelyx Ryzen 5, 16GB, SSD 480GB"));
    }

    @Test
    @DisplayName("Lo específico gana sobre lo genérico según el orden de las reglas")
    void especificoAntesQueGenerico() {
        assertEquals("Coolers > Watercoolers",
                clasificar("COOLER MASTER", "Water Cooler CM Masterliquid 360 Atmos"));
        assertEquals("Coolers > Fans", clasificar("NOCTUA", "Cooler Noctua NH-D15 chromax"));
        assertEquals("Periféricos > Mousepads", clasificar("XTECH", "Mousepad Xtech Colonist"));
        assertEquals("Periféricos > Mouse", clasificar("LOGITECH", "Mouse c/Cable Logitech M100 Negro"));
    }

    @Test
    @DisplayName("La subcategoría sale de las especificaciones, no del encabezado")
    void subcategoriaPorEspecificaciones() {
        assertEquals("Mothers > Plataforma AMD", clasificar("MSI", "Motherboard MSI PRO B840M-B AM5 DDR5"));
        assertEquals("Mothers > Plataforma Intel", clasificar("MSI", "Motherboard MSI PRO H810M-E LGA 1851"));
        assertEquals("Memorias RAM > Memoria DDR5", clasificar("KINGSTON", "Memoria Ram Kingston Fury 16GB DDR5"));
        assertEquals("Memorias RAM > Memoria Sodimm", clasificar("LEXAR", "Memoria Sodimm Lexar 16GB DDR4"));
        assertEquals("Discos Rígidos / SSD > Disco SSD M2",
                clasificar("SANDISK", "Disco Interno SSD Sandisk Extreme 1TB M.2"));
        assertEquals("Conectividad > Switches Administrables",
                clasificar("TP-LINK", "Switch TP-Link 48 puertos Administrable L2"));
    }

    @Test
    @DisplayName("Los consumibles de HP tienen hoja propia, separada del resto")
    void consumiblesHpVanASuPropiaHoja() {
        assertEquals("Consumibles > Consumibles HP", clasificar("HP", "Cartucho de Tinta HP 664 Negro"));
        assertEquals("Consumibles > Cartuchos", clasificar("BROTHER", "Cartucho de Tinta Brother LC3619"));
        assertEquals("Consumibles > Tintas", clasificar("EPSON", "Botella de Tinta Epson 504 Negra"));
    }

    @Test
    @DisplayName("Tolera el typo 'Acces Point' que manda el mayorista")
    void toleraTypoDelMayorista() {
        assertEquals("Conectividad > Access Point y Extensores de Rango",
                clasificar("CUDY", "Acces Point Cudy AC1200 Gigabit"));
    }

    @Test
    @DisplayName("Tablets sin la palabra 'tablet': Samsung/Lenovo/Xiaomi dicen 'Tab' o 'Pad'")
    void tabletsSinLaPalabraCompleta() {
        assertEquals("Tablets", clasificar("Samsung", "Galaxy Tab S10+ SM-X820"));
        assertEquals("Tablets", clasificar("Lenovo", "Idea Tab Plus TB361FU"));
        assertEquals("Tablets", clasificar("Xiaomi", "Redmi Pad Pro 12.1"));
        assertEquals("Tablets", clasificar("Xiaomi", "Pad 7 11.2"));
        assertEquals("Tablets", clasificar("Toy Story", "Kids Tablet 7"));
    }

    @Test
    @DisplayName("\\bpad\\b no le pisa la regla a 'gamepad'/'mousepad': van pegados, sin espacio")
    void padNoPisaPalabrasCompuestas() {
        assertEquals("Accesorios", clasificar("Redragon", "Gamepad Redragon Jupiter"));
        assertEquals("Periféricos > Mousepads", clasificar("Xtech", "Mousepad Xtech Colonist"));
    }

    @Test
    @DisplayName("Consolas: PlayStation 5 por defecto si no dice '4', PS4 explícito, y Xbox")
    void consolas() {
        assertEquals("Consolas > Playstation 5", clasificar("Sony", "PlayStation 5 Standard Bundle Astro"));
        assertEquals("Consolas > Playstation 5", clasificar("Sony", "PlayStation 5 Slim Digital Bundle A"));
        assertEquals("Consolas > Playstation 4", clasificar("Sony", "PlayStation 4 Slim 1TB Negro"));
        assertEquals("Consolas > X-Box", clasificar("Microsoft", "Xbox Series X 1TB Black"));
    }

    @Test
    @DisplayName("Ante la duda devuelve null: mal categorizado es peor que sin categorizar")
    void anteLaDudaDevuelveNull() {
        assertNull(clasificar("ACME", "Dispositivo Acme XYZ-1000"));
        assertNull(clasificar(null, null));
        assertNull(clasificar("ACME", ""));
    }
}
