package com.futuratecno.application;

import com.futuratecno.application.ImagenManualService.ParienteConFoto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ImagenFamiliaTest {

    private static String familia(String marca, String modelo) {
        return ImagenManualService.claveFamilia(marca, modelo);
    }

    @Test
    void capacidadRamTecladoYConectividadNoCambianLaFamilia() {
        assertEquals(familia("Apple", "iMac M4 24 16GB 256GB 10C-10C"), familia("Apple", "iMac M4 24 32GB 1TB 10C-10C"));
        assertEquals(familia("Apple", "MacBook Pro M5 Pro 14 48GB 1TB 18C-20C"),
                familia("Apple", "MacBook Pro M5 Pro 14 24GB 1TB 15C-16C Teclado Español"));
        assertEquals(familia("Apple", "iPad Air M4 11 128GB"), familia("Apple", "iPad Air M4 11 128GB +Cell"));
        assertEquals(familia("Samsung", "Galaxy S26 Ultra 12/256GB Negro"), familia("Samsung", "Galaxy S26 Ultra 12/512GB Blanco"));
        assertEquals(familia("Xiaomi", "Redmi Pad 2 11\" LTE 4GB 128GB"), familia("Xiaomi", "Redmi Pad 2 11\" WiFi 8GB 256GB"));
        // La marca repetida en el modelo no cambia nada.
        assertEquals(familia("Apple", "iPhone 18 Pro Max"), familia("Apple", "Apple iPhone 18 Pro Max 1TB Negro"));
    }

    @Test
    void loInternoQueNoSeVeTampocoCambiaLaFamilia() {
        assertEquals(familia("LENOVO", "LOQ 15ARP10E RYZEN 7 7735HS 16GB 1TB RTX 4050 6GB 144HZ"),
                familia("LENOVO", "LOQ 15ARP10E RYZEN 7 170 16GB, 512GB RTX 4050 6GB 15.6” 144HZ"));
        assertEquals(familia("HP", "250 G10 CORE i7-1355U 8GB 256GB"), familia("HP", "250 G10 CORE i5-1334U 16GB 512GB"));
        assertEquals(familia("MSI", "Vector 16 HX AI CORE ULTRA 9 275HX 32GB 1TB RTX 5080 16GB QHD 240HZ"),
                familia("MSI", "Vector 16 HX AI CORE ULTRA 7 255HX 16GB 1TB RTX 5070 Ti 12GB 165HZ"));
        assertEquals(familia("ASUS", "Zenbook A14 SNAPDRAGON X PLUS 16GB 512GB 14\" OLED"), familia("ASUS", "Zenbook A14 SNAPDRAGON X2 ELITE 32GB 1TB 14\""));
        assertEquals(familia("Apple", "MacBook Pro M5 Pro 14 24GB 1TB"), familia("Apple", "MacBook Pro M5 14\" 16GB 1TB"));
        assertEquals(familia("Dell", "15 CORE i7-1355U 16GB 512GB FHD TOUCH"), familia("Dell", "15 CORE i5-1334U 8GB 512GB FHD"));
        // Opciones comerciales del proveedor.
        assertEquals(familia("Apple", "MacBook Air M5 13\" 24GB 512GB"), familia("Apple", "MacBook Air M5 13 24GB 512GB ABM OPT"));
        assertEquals(familia("Apple", "iPhone 17 Pro 256GB"), familia("Apple", "iPhone 17 Pro 256GB eSIM Sellado"));
    }

    @Test
    void loQueCambiaElAparatoSiCambiaLaFamilia() {
        assertNotEquals(familia("Apple", "iPhone 17 Pro 256GB"), familia("Apple", "iPhone 17 Pro Max 256GB"));
        assertNotEquals(familia("Apple", "MacBook Air 13 M5 16GB 512GB"), familia("Apple", "MacBook Air 15 M5 16GB 512GB"));
        assertNotEquals(familia("Apple", "iPad Pro M5 11 256GB"), familia("Apple", "iPad Pro M5 13 256GB"));
        assertNotEquals(familia("Motorola", "Moto G06 4/128GB"), familia("Motorola", "Moto G06s 4/128GB"));
        assertNotEquals(familia("Apple", "MacBook Pro M5 14 16GB 1TB"), familia("Apple", "MacBook Pro M5 16 16GB 1TB"));
        assertNotEquals(familia("LENOVO", "Legion 5 CORE i9-14900HX 16GB 1TB"), familia("LENOVO", "Legion Pro 5i CORE ULTRA 9 275HX 32GB 1TB"));
        // Touch ID es una tecla que se ve: un Magic Keyboard con y sin Touch ID son distintos.
        assertNotEquals(familia("Apple", "Magic Keyboard Touch ID"), familia("Apple", "Magic Keyboard"));
        // Lo que queda sin nada identificable no forma familia: juntaría artículos distintos.
        assertEquals("", familia("Kingston", "1TB"));
    }

    private static ParienteConFoto pariente(long id, String url, boolean activo, String... colores) {
        return new ParienteConFoto(id, url, activo, Set.of(colores));
    }

    @Test
    void conColorSoloSirveUnParienteDeEseMismoColor() {
        var parientes = List.of(pariente(1, "plata.jpg", true, "plata"), pariente(2, "naranja-viejo.jpg", false, "naranja"),
                pariente(3, "naranja.jpg", true, "naranja"));
        assertEquals(Optional.of("naranja.jpg"), ImagenManualService.elegirPariente(Set.of("naranja"), parientes));
        assertEquals(Optional.empty(), ImagenManualService.elegirPariente(Set.of("azul"), parientes));
    }

    @Test
    void sinColorPrefiereGrisOPlataDespuesNegroDespuesSinColor() {
        var parientes = List.of(pariente(1, "azul.jpg", true, "azul"), pariente(2, "sin-color.jpg", true),
                pariente(3, "negro.jpg", true, "negro"), pariente(4, "plata.jpg", false, "plata"));
        assertEquals(Optional.of("plata.jpg"), ImagenManualService.elegirPariente(Set.of(), parientes));
        assertEquals(Optional.of("negro.jpg"), ImagenManualService.elegirPariente(Set.of(), parientes.subList(0, 3)));
        assertEquals(Optional.of("sin-color.jpg"), ImagenManualService.elegirPariente(Set.of(), parientes.subList(0, 2)));
        assertEquals(Optional.of("azul.jpg"), ImagenManualService.elegirPariente(Set.of(), parientes.subList(0, 1)));
        // Varios colores = la ficha lista los disponibles, no cuál es este: se trata como sin color.
        assertEquals(Optional.of("plata.jpg"), ImagenManualService.elegirPariente(Set.of("azul", "negro"), parientes));
    }

    @Test
    void laGarantiaElProcesadorSinCoreYElTamanoRepetidoNoSeparanFamilias() {
        // Caso real del 2026-10-09 (Kadabra): tres redacciones del mismo ThinkPad E16.
        String kadabra = familia("Lenovo", "ThinkPad E16 Ultra 5 225U 64GB 1TB 16' IPS Win11 Pro 3 Años On Site");
        assertEquals(kadabra, familia("Lenovo", "THINKPAD E16 ULTRA 5 225U 64GB, 1TB"));
        assertEquals(kadabra, familia("Lenovo", "ThinkPad E16 Ultra 5 64GB 2TB"));
        assertEquals(kadabra, familia("Lenovo", "ThinkPad E16 Ultra 5 225U 32GB 2TB 16' IPS Win11 Pro 3 Años On Site"));
        assertEquals(familia("HP", "ProBook 450 G10 i5 16GB 512GB 12 meses de garantia"), familia("HP", "ProBook 450 G10 i5 8GB 256GB"));

        // Lo que tiene que seguir separado.
        assertNotEquals(familia("Apple", "MacBook Air M5 13\" 16GB 512GB"), familia("Apple", "MacBook Air M5 15\" 16GB 512GB"));
        assertNotEquals(familia("Samsung", "Galaxy S25 Ultra 12/256GB"), familia("Samsung", "Galaxy S25 12/256GB"));
        assertNotEquals(familia("Xiaomi", "Redmi Note 14 Pro 5G 8/256GB"), familia("Xiaomi", "Redmi Note 14 5G 8/256GB"));
        assertNotEquals(familia("Lenovo", "ThinkPad E14 Ultra 5 225U 16GB 512GB 14'"), familia("Lenovo", "ThinkPad E16 Ultra 5 225U 16GB 512GB 16'"));
    }
}
