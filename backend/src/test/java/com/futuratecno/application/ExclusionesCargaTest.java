package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExclusionesCargaTest {
    private static String m(String marca, String modelo) { return ExclusionesCarga.motivo(marca, modelo, null, null, null); }

    @Test
    void descartaAccesoriosChicosLavarropasYReplicas() {
        assertNotNull(m("Apple", "Funda Silicona iPhone 17 Pro MagSafe"));
        assertNotNull(m("Generic", "Enchufe 40W AAA+ Pata Americana"));
        assertNotNull(m("Apple", "Cable USB-A a Lightning 1m"));
        assertNotNull(m("Apple", "MagSafe 3 30W Air"));
        assertNotNull(m("Samsung", "Cargador 25W USB-C"));
        assertNotNull(m("Spigen", "Vidrio Templado Galaxy S25"));
        assertNotNull(m("Apple", "Correa Sport Band 46mm"));
        assertNotNull(m("Samsung", "Lavarropas WA80CG4240BW 8KG"));
        assertNotNull(m("Lavarropas", "Samsung WA80CG4240BW 8KG"));
        assertNotNull(m("Apple", "AirPods Pro 2 Réplica"));
        assertNotNull(m("Rolex", "Submariner 1:1"));
        assertNotNull(m("Accesorios", "Funda Galaxy A55"));
    }

    @Test
    void cargaLoQueSoloMencionaUnAccesorioYLosDeValor() {
        assertNull(m("HP", "Notebook 15 i5 16GB 512GB con cargador"));
        assertNull(m("Apple", "iPhone 17 Pro 256GB compatible MagSafe"));
        assertNull(m("Apple", "Pencil USB-C"));
        assertNull(m("Apple", "AirTag Pack x4"));
        assertNull(m("Sony", "PlayStation 5 DualSense Wireless Controller Black"));
        assertNull(m("Logitech", "Volante G923 Xbox"));
        assertNull(m("Xiaomi", "Smart Band 9"));
        assertNull(m("Corsair", "Fuente RM850x 850W 80 Plus Gold"));
        assertNull(m("JBL", "Xtreme 5 130W Negro"));
        assertNull(m("Fujifilm", "Film Instax Mini 12 Repuesto X20U"));
    }

    @Test
    void electrodomesticosNoSeCargan() {
        assertNotNull(m("Xiaomi", "Smart Air Fryer 6.5L"));
        assertNotNull(m("Philips", "Freidora de aire 4L"));
        assertNotNull(m("Samsung", "Heladera No Frost 382L"));
        assertNotNull(m("BGH", "Microondas 20L Digital"));
        assertNotNull(m("Xiaomi", "Robot Aspirador X20+"));
        assertNotNull(m("Xiaomi", "Mi Robot Vacuum S10"));
        assertNotNull(m("Liliana", "Ventilador de pie 20\""));
        assertNotNull(m("Peabody", "Calefactor infrarrojo 1200W"));
        assertNotNull(ExclusionesCarga.motivo("Atma", "MX-2000", null, null, "Electrodomésticos"));
    }

    @Test
    void cuidadoPersonalDysonYTecladosDeIpadNoSeCargan() {
        assertNotNull(m("Dyson", "Secador de pelo Supersonic"));
        assertNotNull(m("Dyson", "V15 Detect Absolute"));
        assertNotNull(m("Philips", "Afeitadora Serie 5000"));
        assertNotNull(m("Gama", "Planchita de pelo Elegance"));
        assertNotNull(m("Remington", "Cortadora de pelo HC5150"));
        assertNotNull(m("Xiaomi", "Hair Clipper 2"));
        assertNotNull(m("Apple", "Magic Keyboard iPad Air 13\" White"));
        assertNotNull(m("Apple", "Smart Keyboard Folio iPad Pro 11"));
        assertNull(m("Apple", "Magic Keyboard Touch ID Español"));
        assertNull(m("Apple", "iPad Air M3 11 128GB"));
        assertNotNull(m("Samsung", "Secadora de ropa 8kg"));
    }

    @Test
    void loQueSoloSeParecesSeCarga() {
        assertNull(m("Cooler Master", "Ventilador 120mm ARGB"));
        assertNull(m("Lenovo", "IdeaPad Slim 3 con ventilador silencioso"));
        assertNull(ExclusionesCarga.motivo("Lenovo", "IdeaPad 1", null, null, "Notebooks > Consumo"));
    }
}
