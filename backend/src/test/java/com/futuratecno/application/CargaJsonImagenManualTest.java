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
        var productos = mock(ProductoRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "DJI", "Nano 64GB")).thenReturn(Optional.empty());
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(memoria.buscar("DJI", "Nano 64GB")).thenReturn(manual);
        var service = new CargaJsonService(memoria, mock(DescripcionManualService.class),
                mock(AtributosManualService.class), productos, mock(VarianteRepository.class), proveedores,
                mock(ImagenRepository.class), mock(CategoriaClasificadorService.class), mock(CategoriaService.class));
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
                mock(CategoriaClasificadorService.class), mock(CategoriaService.class));
        var art = new ArticuloJsonDTO();
        art.setMarca("DJI"); art.setModelo("Nano 64GB"); art.setPrecioUsd(BigDecimal.TEN); art.setImagenes(urls);
        service.cargar(2L, List.of(art));
        verify(productos).save(argThat(p -> esperado.equals(p.getImagenUrl())));
        if (manual.isPresent() || !urls.isEmpty()) {
            verify(imagenes).save(argThat(img -> esperado.equals(img.getUrl())));
        } else verify(imagenes, never()).deleteAll(any(Iterable.class));
    }
}
