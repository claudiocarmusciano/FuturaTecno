package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** La pista de categoría de una carga ("Celulares") no puede sacar a Apple de su árbol. */
class CategoriaClasificadorAppleTest {

    private final CategoriaService categorias = mock(CategoriaService.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final ClasificadorPorNombre porNombre = mock(ClasificadorPorNombre.class, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    private final CategoriaClasificadorService servicio = new CategoriaClasificadorService(new RestTemplate(), categorias, porNombre, new JevClient());

    private static Producto producto(String marca, String modelo) {
        Producto p = new Producto();
        p.setMarca(marca);
        p.setModelo(modelo);
        return p;
    }

    @Test
    void appleVaASuArbolAunqueLaCargaSugieraCelulares() {
        Producto ipad = producto("Apple", "iPad Pro M4 13 2TB Nano Texture");
        when(porNombre.clasificar(ipad)).thenReturn("Apple > iPad");
        when(categorias.idPorPath("Apple > iPad")).thenReturn(96L);
        when(categorias.idPorPath("Celulares")).thenReturn(107L);

        assertEquals(96L, servicio.clasificar(ipad, "Celulares"));
    }

    @Test
    void laNintendoSwitchNoTerminaEnSwitchesDeRedAunqueLaPistaLoDiga() {
        Producto sw = producto("Nintendo", "Switch 2 + Mario (1 día de uso)");
        when(porNombre.clasificar(sw)).thenReturn("Consolas > Nintendo Switch");
        when(categorias.idPorPath("Consolas > Nintendo Switch")).thenReturn(210L);
        when(categorias.idPorPath("Switches No Administrables")).thenReturn(300L);

        assertEquals(210L, servicio.clasificar(sw, "Switches No Administrables"));
    }

    @Test
    void lasTarjetasDeMemoriaVanAAlmacenamientoAunqueLaPistaDigaDdr4() {
        when(categorias.idPorPath("Almacenamiento > Tarjetas de memoria")).thenReturn(400L);
        when(categorias.idPorPath("Memoria DDR4")).thenReturn(500L);
        assertEquals(400L, servicio.clasificar(producto("KINGSTON", "Tarjeta de Memoria KINGSTON Canvas Select Plus MicroSDXC 256GB"), "Memoria DDR4"));
        assertEquals(400L, servicio.clasificar(producto("Hiksemi", "MicroSD HIKSEMI 64Gb NEO c/Adap"), "Memoria DDR4"));
    }

    @Test
    void unParlanteOUnAdaptadorQueMencionanMicroSdNoSonTarjetas() {
        org.junit.jupiter.api.Assertions.assertFalse(CategoriaClasificadorService.esTarjetaDeMemoria("Parlante Kelyx Bluetooth 1 altavoz 5W MicroSD / USB"));
        org.junit.jupiter.api.Assertions.assertFalse(CategoriaClasificadorService.esTarjetaDeMemoria("UA430 Adaptador TP-Link USB-A a SD / Micro SD"));
        org.junit.jupiter.api.Assertions.assertFalse(CategoriaClasificadorService.esTarjetaDeMemoria("Memoria Ram UDIMM KINGSTON Fury Beast 16GB DDR4"));
        org.junit.jupiter.api.Assertions.assertTrue(CategoriaClasificadorService.esTarjetaDeMemoria("Tarjeta de Memoria Sandisk MicroSDXC 128GB for Nintendo Switch"));
    }

    @Test
    void otraMarcaSigueUsandoLaPista() {
        Producto galaxy = producto("Samsung", "Galaxy S26 Ultra 12/256GB");
        when(categorias.idPorPath("Celulares")).thenReturn(107L);

        // Un celular de verdad sigue yendo a Celulares por la pista del mayorista.
        // (Antes este test exigía que NO se consultara al clasificador por nombre;
        // ahora se lo consulta siempre, para poder rescatar las tablets — ver el
        // test de abajo. Lo que importa es el resultado, no cuántos colaboradores
        // se tocan para llegar a él.)
        assertEquals(107L, servicio.clasificar(galaxy, "Celulares"));
    }

    /**
     * Regresión del 2026-09-26: Kadabra manda "Celulares" para las tablets, y 11 Galaxy Tab
     * y Redmi Pad quedaron listadas como celulares en el catálogo. Si el nombre dice que es
     * una tablet, gana sobre la pista del mayorista.
     */
    @Test
    void unaTabletNoQuedaEnCelularesAunqueLaCargaLoSugiera() {
        Producto tab = producto("Samsung", "Galaxy Tab S10 FE WiFi 12GB 256GB");
        when(porNombre.clasificar(tab)).thenReturn("Tablets");
        when(categorias.idPorPath("Tablets")).thenReturn(93L);
        when(categorias.idPorPath("Celulares")).thenReturn(107L);

        assertEquals(93L, servicio.clasificar(tab, "Celulares"));
    }

    @Test
    void unaRedmiPadTampocoQuedaEnCelulares() {
        Producto pad = producto("Xiaomi", "Redmi Pad 2 11\" WiFi 8GB 256GB");
        when(porNombre.clasificar(pad)).thenReturn("Tablets");
        when(categorias.idPorPath("Tablets")).thenReturn(93L);
        when(categorias.idPorPath("Celulares")).thenReturn(107L);

        assertEquals(93L, servicio.clasificar(pad, "Celulares"));
    }
}
