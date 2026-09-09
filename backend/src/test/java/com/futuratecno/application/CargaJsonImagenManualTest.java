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
    @Test void normalizacionConservaVariantes() {
        assertEquals("mic mini white", ImagenManualService.normalizar(" MIC  MINI White "));
        assertNotEquals(ImagenManualService.normalizar("Mic Mini White"), ImagenManualService.normalizar("Mic Mini Black"));
        assertNotEquals(ImagenManualService.normalizar("Nano 64GB"), ImagenManualService.normalizar("Nano 128GB"));
        assertNotEquals(ImagenManualService.normalizar("Mini 5 Pro"), ImagenManualService.normalizar("Mini 5 Pro Plus"));
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
        var service = new CargaJsonService(memoria, productos, variantes, proveedores, imagenes,
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
