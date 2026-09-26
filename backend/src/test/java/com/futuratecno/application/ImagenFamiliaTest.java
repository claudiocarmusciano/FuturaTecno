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
    void loQueCambiaElAparatoSiCambiaLaFamilia() {
        assertNotEquals(familia("Apple", "iPhone 17 Pro 256GB"), familia("Apple", "iPhone 17 Pro Max 256GB"));
        assertNotEquals(familia("Apple", "MacBook Air 13 M5 16GB 512GB"), familia("Apple", "MacBook Air 15 M5 16GB 512GB"));
        assertNotEquals(familia("Apple", "iPad Pro M5 11 256GB"), familia("Apple", "iPad Pro M5 13 256GB"));
        assertNotEquals(familia("Motorola", "Moto G06 4/128GB"), familia("Motorola", "Moto G06s 4/128GB"));
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
}
