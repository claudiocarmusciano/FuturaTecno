package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Qué reprocesa el botón "Clasificar categorías faltantes". */
class ProductoAdminClasificarTest {

    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final ProductoRepository productos = stub(ProductoRepository.class);
    private final CategoriaService categorias = stub(CategoriaService.class);
    private final CategoriaClasificadorService clasificador = stub(CategoriaClasificadorService.class);

    private final ProductoAdminService service = new ProductoAdminService(
            stub(ImagenManualService.class), stub(DescripcionManualService.class), stub(AtributosManualService.class),
            stub(MargenManualService.class), productos, stub(VarianteRepository.class),
            stub(IcecatService.class), stub(GoogleImageService.class), stub(AnthropicImageService.class),
            stub(DuckDuckGoImageService.class), stub(ImageUrlValidatorService.class),
            stub(CotizacionService.class), stub(PrecioService.class), clasificador,
            categorias, stub(CategoriaRepository.class), stub(IdentidadTransicionService.class));

    private static Producto producto(String marca, String modelo) {
        Producto p = new Producto();
        p.setActivo(true);
        p.setMarca(marca);
        p.setModelo(modelo);
        p.setCategoriaId(300L);
        return p;
    }

    @Test
    void unaTarjetaParaNintendoSwitchNoSeReprocesaComoConsola() {
        Producto tarjeta = producto("SANDISK", "Tarjeta de Memoria Sandisk MicroSDXC 128GB for Nintendo Switch Mario Bros Edition");
        when(productos.findAll()).thenReturn(List.of(tarjeta));
        when(categorias.estaBajo(300L, "Almacenamiento")).thenReturn(true);

        assertEquals(0, service.clasificarCategoriasFaltantes().getProcesados());
        verify(clasificador, never()).clasificar(any(), any());
    }

    @Test
    void unaConsolaSwitchFueraDeConsolasSiSeReprocesa() {
        Producto consola = producto("Nintendo", "Switch 2 + Mario Kart World");
        when(productos.findAll()).thenReturn(List.of(consola));

        assertEquals(1, service.clasificarCategoriasFaltantes().getProcesados());
    }
}
