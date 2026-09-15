package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CargaJsonImagenManualTest {
    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    /** Validador que da por buena (o por muerta) toda URL del JSON. */
    private static ImageUrlValidatorService validador(boolean viva) {
        var v = mock(ImageUrlValidatorService.class);
        when(v.esImagenDirecta(anyString())).thenReturn(viva);
        return v;
    }
    @Test void reutilizaManualEnProductoNuevoYPriorizaSobreJson() {
        comprobar(true, Optional.of("https://manual/image.jpg"), List.of("https://json/image.jpg"), "https://manual/image.jpg");
    }
    @Test void reutilizaManualConJsonVacio() {
        comprobar(true, Optional.of("https://manual/image.jpg"), List.of(), "https://manual/image.jpg");
    }
    @Test void conservaImagenExistenteConJsonVacio() {
        comprobar(false, Optional.empty(), List.of(), "https://existing/image.jpg");
    }
    @Test void admiteJsonCuandoNoHayEleccionManual() {
        comprobar(true, Optional.empty(), List.of("https://json/image.jpg"), "https://json/image.jpg");
    }
    /**
     * La imagen que llega en el borrador queda recordada, así el próximo listado con este
     * marca+modelo la encuentra en la memoria en vez de pagar otra búsqueda.
     */
    @Test void recuerdaLaImagenDelBorradorParaNoVolverAPagarLaBusqueda() {
        var memoria = mock(ImagenManualService.class);
        cargarCon(memoria, Optional.empty(), List.of("https://json/image.jpg"));
        verify(memoria).guardarAutomatica("DJI", "Nano 64GB", "https://json/image.jpg");
    }
    /** Si ya vino de la memoria, reescribirla sería un INSERT por artículo que no cambia nada. */
    @Test void noReescribeLaQueYaEstabaRecordada() {
        var memoria = mock(ImagenManualService.class);
        cargarCon(memoria, Optional.of("https://manual/image.jpg"), List.of());
        verify(memoria, never()).guardarAutomatica(anyString(), anyString(), anyString());
    }
    /**
     * Una URL del JSON que no responde no se publica: el producto queda sin foto y aparece en
     * Admin → Imágenes. Antes alcanzaba con que empezara por "http".
     */
    @Test void descartaLaImagenDelJsonSiLaUrlEstaMuerta() {
        var productos = mock(ProductoRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        var memoria = mock(ImagenManualService.class);
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "DJI", "Nano 64GB")).thenReturn(Optional.empty());
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(memoria.buscar("DJI", "Nano 64GB")).thenReturn(Optional.empty());
        var service = new CargaJsonService(memoria, mock(DescripcionManualService.class),
                mock(AtributosManualService.class), productos, mock(VarianteRepository.class), proveedores,
                mock(ImagenRepository.class), mock(CategoriaClasificadorService.class), mock(CategoriaService.class),
                validador(false));
        var art = new ArticuloJsonDTO();
        art.setMarca("DJI"); art.setModelo("Nano 64GB"); art.setPrecioUsd(BigDecimal.TEN);
        art.setImagenes(List.of("https://json/rota.jpg"));

        var res = service.cargar(2L, List.of(art));

        verify(productos).save(argThat(p -> p.getImagenUrl() == null));
        assertEquals("La imagen del JSON no responde. Quedó sin foto: cargala desde Admin → Imágenes.",
                res.getItems().getFirst().getMotivo());
    }

    /** Lo peor de una URL muerta era que se recordaba y se reusaba para siempre. */
    @Test void unaUrlMuertaNoEnsuciaLaMemoria() {
        var memoria = mock(ImagenManualService.class);
        cargarCon(memoria, Optional.empty(), List.of("https://json/rota.jpg"), false);
        verify(memoria, never()).guardarAutomatica(anyString(), anyString(), anyString());
    }

    /** Si la base ya tenía imagen, no se gasta una petición en verificar la del JSON. */
    @Test void noVerificaLaUrlSiLaBaseYaTieneImagen() {
        var validador = mock(ImageUrlValidatorService.class);
        var productos = mock(ProductoRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        var memoria = mock(ImagenManualService.class);
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "DJI", "Nano 64GB")).thenReturn(Optional.empty());
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(memoria.buscar("DJI", "Nano 64GB")).thenReturn(Optional.of("https://manual/image.jpg"));
        var service = new CargaJsonService(memoria, mock(DescripcionManualService.class),
                mock(AtributosManualService.class), productos, mock(VarianteRepository.class), proveedores,
                mock(ImagenRepository.class), mock(CategoriaClasificadorService.class), mock(CategoriaService.class),
                validador);
        var art = new ArticuloJsonDTO();
        art.setMarca("DJI"); art.setModelo("Nano 64GB"); art.setPrecioUsd(BigDecimal.TEN);
        art.setImagenes(List.of("https://json/image.jpg"));

        service.cargar(2L, List.of(art));

        verifyNoInteractions(validador);
    }

    /**
     * La clave suelta absorbe cómo redactó la IA el modelo esta vez, sin perder lo que distingue.
     * Tiene que dar exactamente lo mismo que la columna generada productos.clave_suelta (V37).
     */
    @Test void laClaveSueltaIgnoraLaRedaccionPeroNoLaCapacidad() {
        String esperada = "appleiphone17pro256gbesim";
        assertEquals(esperada, ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB eSIM"));
        assertEquals(esperada, ImagenManualService.clave("Apple", "iPhone 17 Pro 256 GB (eSIM)"));
        assertEquals(esperada, ImagenManualService.clave("apple", "iphone 17 pro 256gb e-SIM"));
        assertEquals(esperada, ImagenManualService.clave("Apple", "  iPhone 17 Pro 256GB eSIM  "));

        assertNotEquals(esperada, ImagenManualService.clave("Apple", "iPhone 17 Pro 512GB eSIM"));
        assertNotEquals(esperada, ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB SIM"));
        assertNotEquals(ImagenManualService.clave("DJI", "Mic Mini White"),
                        ImagenManualService.clave("DJI", "Mic Mini Black"));
        assertNotEquals(ImagenManualService.clave("DJI", "Mini 5 Pro"),
                        ImagenManualService.clave("DJI", "Mini 5 Pro Plus"));
    }

    /** La IA repite la marca en el modelo unas veces sí y otras no: las dos formas son la misma. */
    @Test void laMarcaRepetidaEnElModeloNoCambiaLaClave() {
        assertEquals(ImagenManualService.clave("Apple", "Apple Pencil Pro"),
                     ImagenManualService.clave("Apple", "Pencil Pro"));
    }

    @Test void normalizacionConservaVariantes() {
        assertEquals("mic mini white", ImagenManualService.normalizar(" MIC  MINI White "));
        assertNotEquals(ImagenManualService.normalizar("Mic Mini White"), ImagenManualService.normalizar("Mic Mini Black"));
        assertNotEquals(ImagenManualService.normalizar("Nano 64GB"), ImagenManualService.normalizar("Nano 128GB"));
        assertNotEquals(ImagenManualService.normalizar("Mini 5 Pro"), ImagenManualService.normalizar("Mini 5 Pro Plus"));
    }
    /** Carga un artículo con la memoria dada, para poder verificar qué se le pide y qué se le guarda. */
    private void cargarCon(ImagenManualService memoria, Optional<String> manual, List<String> urls) {
        cargarCon(memoria, manual, urls, true);
    }

    private void cargarCon(ImagenManualService memoria, Optional<String> manual, List<String> urls, boolean urlViva) {
        var productos = mock(ProductoRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "DJI", "Nano 64GB")).thenReturn(Optional.empty());
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(memoria.buscar("DJI", "Nano 64GB")).thenReturn(manual);
        var service = new CargaJsonService(memoria, mock(DescripcionManualService.class),
                mock(AtributosManualService.class), productos, mock(VarianteRepository.class), proveedores,
                mock(ImagenRepository.class), mock(CategoriaClasificadorService.class), mock(CategoriaService.class),
                validador(urlViva));
        var art = new ArticuloJsonDTO();
        art.setMarca("DJI"); art.setModelo("Nano 64GB"); art.setPrecioUsd(BigDecimal.TEN); art.setImagenes(urls);
        service.cargar(2L, List.of(art));
    }

    private void comprobar(boolean nuevo, Optional<String> manual, List<String> urls, String esperado) {
        var productos = mock(ProductoRepository.class);
        var variantes = mock(VarianteRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        var imagenes = mock(ImagenRepository.class);
        var memoria = mock(ImagenManualService.class);
        var producto = new Producto();
        producto.setId(10L);
        producto.setImagenUrl("https://existing/image.jpg");
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "DJI", "Nano 64GB"))
                .thenReturn(nuevo ? Optional.empty() : Optional.of(producto));
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(memoria.buscar("DJI", "Nano 64GB")).thenReturn(manual);
        var service = new CargaJsonService(memoria, mock(DescripcionManualService.class),
                mock(AtributosManualService.class), productos, variantes, proveedores, imagenes,
                mock(CategoriaClasificadorService.class), mock(CategoriaService.class), validador(true));
        var art = new ArticuloJsonDTO();
        art.setMarca("DJI"); art.setModelo("Nano 64GB"); art.setPrecioUsd(BigDecimal.TEN); art.setImagenes(urls);
        service.cargar(2L, List.of(art));
        verify(productos).save(argThat(p -> esperado.equals(p.getImagenUrl())));
        if (manual.isPresent() || !urls.isEmpty()) {
            verify(imagenes).save(argThat(img -> esperado.equals(img.getUrl())));
        } else verify(imagenes, never()).deleteAll(any(Iterable.class));
    }
}
