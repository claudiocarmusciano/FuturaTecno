package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Depuración del catálogo: qué se propone sacar y, sobre todo, qué se guarda antes de sacarlo.
 * Un producto dado de baja tiene que poder volver sin recargar a mano su imagen ni su categoría.
 */
class ProductoAdminDepurarTest {

    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final ImagenManualService imagenes = stub(ImagenManualService.class);
    private final DescripcionManualService descripciones = stub(DescripcionManualService.class);
    private final AtributosManualService atributos = stub(AtributosManualService.class);
    private final ProductoRepository productos = stub(ProductoRepository.class);
    private final VarianteRepository variantes = stub(VarianteRepository.class);
    private final CategoriaService categorias = stub(CategoriaService.class);

    private final ProductoAdminService service = new ProductoAdminService(
            imagenes, descripciones, atributos, stub(MargenManualService.class), productos, variantes,
            stub(IcecatService.class), stub(GoogleImageService.class), stub(AnthropicImageService.class),
            stub(DuckDuckGoImageService.class), stub(ImageUrlValidatorService.class),
            stub(CotizacionService.class), stub(CategoriaClasificadorService.class),
            categorias, stub(CategoriaRepository.class));

    private Producto producto(long id, String fuente, LocalDateTime actualizado) {
        Producto p = new Producto();
        p.setId(id);
        p.setMarca("Apple");
        p.setModelo("iPhone 15 128GB");
        p.setFuente(fuente);
        p.setImagenUrl("https://cdn.test/iphone.jpg");
        p.setCategoriaId(95L);
        p.setUpdatedAt(actualizado);
        return p;
    }

    @Test void proponeSoloLoQueNoSeActualizaHaceMasDeLosDiasPedidos() {
        Producto viejo = producto(1L, "INVID", LocalDateTime.now().minusDays(10));
        Producto fresco = producto(2L, "JSON", LocalDateTime.now().minusHours(2));
        when(productos.findByActivo(true)).thenReturn(List.of(viejo, fresco));
        when(variantes.findByProductoIdInAndActivo(anyList(), eq(true))).thenReturn(List.of());

        var vencidos = service.listarVencidos(3);

        assertEquals(1, vencidos.size());
        assertEquals(1L, vencidos.getFirst().getId());
    }

    /** La variante actualizada hoy mantiene vivo al producto aunque su fila no se haya tocado. */
    @Test void laFechaDeLaVarianteCuentaComoActualizacion() {
        Producto p = producto(1L, "ELIT", LocalDateTime.now().minusDays(10));
        Variante v = new Variante();
        v.setId(9L);
        v.setProducto(p);
        v.setUpdatedAt(LocalDateTime.now().minusHours(1));
        when(productos.findByActivo(true)).thenReturn(List.of(p));
        when(variantes.findByProductoIdInAndActivo(anyList(), eq(true))).thenReturn(List.of(v));

        assertTrue(service.listarVencidos(3).isEmpty());
    }

    /**
     * El caso que motivó la V40: Invid informó 1.188 actualizados y solo 70 movieron updated_at,
     * porque el resto vino con precio y stock idénticos. Siguen en el feed y no deben proponerse.
     */
    @Test void noProponeUnProductoQueElMayoristaSigueTeniendo() {
        Producto p = producto(1L, "INVID", LocalDateTime.now().minusDays(10));
        p.setVistoEnSyncAt(LocalDateTime.now().minusHours(2));
        when(productos.findByActivo(true)).thenReturn(List.of(p));
        when(variantes.findByProductoIdInAndActivo(anyList(), eq(true))).thenReturn(List.of());

        assertTrue(service.listarVencidos(3).isEmpty(),
                "el precio no cambió hace 10 días, pero el mayorista lo devolvió hace 2 horas");
    }

    /** Si dejó de aparecer en el feed, sí se propone aunque su fila se haya tocado por otra cosa. */
    @Test void proponeElQueDesaparecioDelFeed() {
        Producto p = producto(1L, "ELIT", LocalDateTime.now().minusHours(1));
        p.setVistoEnSyncAt(LocalDateTime.now().minusDays(10));
        when(productos.findByActivo(true)).thenReturn(List.of(p));
        when(variantes.findByProductoIdInAndActivo(anyList(), eq(true))).thenReturn(List.of());

        assertEquals(1, service.listarVencidos(3).size());
    }

    /** Sin feed (carga por JSON) se usa la última actualización, que ahí sí es la señal honesta. */
    @Test void sinVistoEnSyncCaeEnLaUltimaActualizacion() {
        Producto viejo = producto(1L, "JSON", LocalDateTime.now().minusDays(10));
        Producto fresco = producto(2L, "JSON", LocalDateTime.now().minusHours(2));
        when(productos.findByActivo(true)).thenReturn(List.of(viejo, fresco));
        when(variantes.findByProductoIdInAndActivo(anyList(), eq(true))).thenReturn(List.of());

        var vencidos = service.listarVencidos(3);
        assertEquals(1, vencidos.size());
        assertEquals(1L, vencidos.getFirst().getId());
    }

    @Test void guardaImagenYCategoriaAntesDeDarDeBaja() {
        Producto p = producto(1L, "JSON", LocalDateTime.now().minusDays(10));
        when(productos.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(productos.desactivarPorIds(List.of(1L))).thenReturn(1);
        when(variantes.findByProductoIdAndActivo(1L, true)).thenReturn(List.of());

        assertEquals(1, service.eliminarMasivamente(List.of(1L)));

        verify(atributos).recordar(p);
        verify(imagenes).guardarAutomatica("Apple", "iPhone 15 128GB", "https://cdn.test/iphone.jpg");
        verify(productos).desactivarPorIds(List.of(1L));
    }

    /** La imagen entra como automática: nunca puede pisar una que el admin eligió a mano (V32). */
    @Test void noPisaLaImagenElegidaAMano() {
        Producto p = producto(1L, "JSON", LocalDateTime.now().minusDays(10));
        when(productos.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(variantes.findByProductoIdAndActivo(1L, true)).thenReturn(List.of());

        service.eliminarMasivamente(List.of(1L));

        verify(imagenes, never()).guardar(anyString(), anyString(), anyString());
    }

    @Test void recuerdaLaDescripcionDeUnaCargaPropia() {
        Producto p = producto(1L, "JSON", LocalDateTime.now().minusDays(10));
        Variante v = new Variante();
        v.setEspecificaciones("128GB · eSIM");
        when(productos.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(variantes.findByProductoIdAndActivo(1L, true)).thenReturn(List.of(v));

        service.eliminarMasivamente(List.of(1L));

        verify(descripciones).guardar("Apple", "iPhone 15 128GB", "128GB · eSIM");
    }

    /** Elit e Invid reescriben su ficha en cada sync: recordarla taparía la propia (V33). */
    @Test void noRecuerdaLaDescripcionDeUnMayorista() {
        Producto p = producto(1L, "ELIT", LocalDateTime.now().minusDays(10));
        Variante v = new Variante();
        v.setEspecificaciones("Ficha de Elit");
        when(productos.findAllById(List.of(1L))).thenReturn(List.of(p));
        when(variantes.findByProductoIdAndActivo(1L, true)).thenReturn(List.of(v));

        service.eliminarMasivamente(List.of(1L));

        verify(descripciones, never()).guardar(anyString(), anyString(), anyString());
        verify(atributos).recordar(p);   // el peso y la categoría sí, que ningún mayorista los trae
    }
}
