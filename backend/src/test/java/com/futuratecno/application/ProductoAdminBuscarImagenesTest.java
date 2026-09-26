package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Los dos botones de Admin → Imágenes: la base (todos, gratis) e internet (de a 10, sigue donde quedó). */
class ProductoAdminBuscarImagenesTest {

    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final ImagenManualService imagenes = stub(ImagenManualService.class);
    private final ProductoRepository productos = stub(ProductoRepository.class);
    private final DuckDuckGoImageService duck = stub(DuckDuckGoImageService.class);
    private final AnthropicImageService anthropic = stub(AnthropicImageService.class);
    private final ImageUrlValidatorService validador = stub(ImageUrlValidatorService.class);

    private final ProductoAdminService service = new ProductoAdminService(
            imagenes, stub(DescripcionManualService.class), stub(AtributosManualService.class), stub(MargenManualService.class),
            productos, stub(VarianteRepository.class),
            stub(IcecatService.class), stub(GoogleImageService.class), anthropic,
            duck, validador,
            stub(CotizacionService.class), stub(PrecioService.class), stub(CategoriaClasificadorService.class),
            stub(CategoriaService.class), stub(CategoriaRepository.class), stub(IdentidadTransicionService.class));

    private final List<Producto> catalogo = new ArrayList<>();

    ProductoAdminBuscarImagenesTest() {
        LongStream.rangeClosed(1, 25).forEach(id -> {
            Producto p = new Producto();
            p.setId(id);
            p.setMarca("HP");
            p.setModelo("Modelo " + id);
            p.setActivo(true);
            catalogo.add(p);
        });
        when(productos.findByActivo(true)).thenAnswer(i -> List.copyOf(catalogo));
        when(imagenes.buscar(anyString(), anyString())).thenReturn(Optional.empty());
        when(imagenes.buscarPorFamilia(anyString(), anyString(), any())).thenReturn(Optional.empty());
        when(duck.buscarImagenes(anyString())).thenReturn(List.of());
        when(validador.verificar(anyString())).thenReturn(ImageUrlValidatorService.Verificacion.IMAGEN);
    }

    @Test
    void internetBuscaDeADiezYElClicSiguienteSigueConLosDiezSiguientes() {
        service.buscarImagenesEnInternet();
        assertEquals(10, catalogo.stream().filter(p -> p.getImagenBusquedaAt() != null).count());
        assertTrue(catalogo.subList(0, 10).stream().allMatch(p -> p.getImagenBusquedaAt() != null));

        var r = service.buscarImagenesEnInternet();
        assertEquals(10, r.getProcesados());
        assertTrue(catalogo.subList(10, 20).stream().allMatch(p -> p.getImagenBusquedaAt() != null));
        assertTrue(catalogo.subList(20, 25).stream().allMatch(p -> p.getImagenBusquedaAt() == null));
        assertTrue(r.getMensaje().contains("5 todavía no se buscaron"), r.getMensaje());
    }

    @Test
    void laBaseRecorreTodosDeUnaYNoSaleAInternet() {
        when(imagenes.buscarPorFamilia(eq("HP"), eq("Modelo 7"), any())).thenReturn(Optional.of("https://cdn.test/7.jpg"));
        when(imagenes.buscar("HP", "Modelo 22")).thenReturn(Optional.of("https://cdn.test/22.jpg"));

        var r = service.buscarImagenesEnBase();

        assertEquals(25, r.getProcesados());
        assertEquals(2, r.getEncontradas());
        assertEquals("https://cdn.test/7.jpg", catalogo.get(6).getImagenUrl());
        assertEquals("https://cdn.test/22.jpg", catalogo.get(21).getImagenUrl());
        verifyNoInteractions(duck, anthropic);
        // No marca intentos: la cola de internet no se mueve por buscar en la base.
        assertTrue(catalogo.stream().allMatch(p -> p.getImagenBusquedaAt() == null));
    }

    @Test
    void unaFotoDeLaBaseQueDa404NoSeUsa() {
        when(imagenes.buscarPorFamilia(eq("HP"), eq("Modelo 3"), any())).thenReturn(Optional.of("https://cdn.test/muerta.jpg"));
        when(validador.verificar("https://cdn.test/muerta.jpg")).thenReturn(ImageUrlValidatorService.Verificacion.MUERTA);

        var r = service.buscarImagenesEnBase();

        assertEquals(0, r.getEncontradas());
        assertNull(catalogo.get(2).getImagenUrl());
    }
}
