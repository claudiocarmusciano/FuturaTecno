package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Recargar el listado de ayer no debe duplicar el producto porque la IA redactó el modelo distinto.
 * Duplicarlo significaba ofrecer el mismo artículo dos veces a precios distintos y volver a cargarle
 * la foto a mano.
 */
class CargaJsonProductoUnicoTest {

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final VarianteRepository variantes = mock(VarianteRepository.class);
    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);

    private final CargaJsonService service = new CargaJsonService(mock(ImagenManualService.class),
            mock(DescripcionManualService.class), mock(AtributosManualService.class), mock(MargenManualService.class),
            productos, variantes, proveedores, mock(ImagenRepository.class),
            mock(CategoriaClasificadorService.class), mock(CategoriaService.class), mock(ImageUrlValidatorService.class));

    private Producto existente(String modelo) {
        Producto p = new Producto();
        p.setId(10L);
        p.setMarca("Apple");
        p.setModelo(modelo);
        return p;
    }

    private void cargar(String modeloQueEntra) {
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); if (p.getId() == null) p.setId(99L); return p; });
        var art = new ArticuloJsonDTO();
        art.setMarca("Apple");
        art.setModelo(modeloQueEntra);
        art.setPrecioUsd(new BigDecimal("1508.17"));
        service.cargar(2L, List.of(art));
    }

    /** El caso real: mismo teléfono, otra redacción. Tiene que actualizar, no crear. */
    @Test void reconoceElProductoAunqueCambieLaRedaccionDelModelo() {
        var yaCargado = existente("iPhone 17 Pro 256GB eSIM");
        when(productos.findByProveedorIdAndMarcaAndModelo(eq(2L), anyString(), anyString())).thenReturn(Optional.empty());
        when(productos.idPorClaveSuelta(2L, "appleiphone17pro256gbesim")).thenReturn(Optional.of(10L));
        when(productos.findById(10L)).thenReturn(Optional.of(yaCargado));

        cargar("iPhone 17 Pro 256 GB (eSIM)");

        verify(productos).save(argThat(p -> Long.valueOf(10L).equals(p.getId())));
        assertEquals("iPhone 17 Pro 256GB eSIM", yaCargado.getModelo(),
                "el nombre publicado no debe cambiar en cada carga por cómo lo redactó la IA");
    }

    /** Si el match exacto funciona, no hace falta la consulta suelta. */
    @Test void noConsultaLaClaveSueltaCuandoElExactoAlcanza() {
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "Apple", "iPhone 17 Pro 256GB eSIM"))
                .thenReturn(Optional.of(existente("iPhone 17 Pro 256GB eSIM")));

        cargar("iPhone 17 Pro 256GB eSIM");

        verify(productos, never()).idPorClaveSuelta(anyLong(), anyString());
    }

    /** Un artículo que de verdad es nuevo se sigue creando. */
    @Test void creaElProductoCuandoNoHayNadaParecido() {
        when(productos.findByProveedorIdAndMarcaAndModelo(eq(2L), anyString(), anyString())).thenReturn(Optional.empty());
        when(productos.idPorClaveSuelta(anyLong(), anyString())).thenReturn(Optional.empty());

        cargar("iPhone 17 Pro 1TB eSIM");

        verify(productos).save(argThat(p -> "iPhone 17 Pro 1TB eSIM".equals(p.getModelo())));
    }

    /** Aflojar la clave no puede hacer que una capacidad caiga sobre otra. */
    @Test void laClaveSueltaNoConfundeCapacidades() {
        assertNotEquals(ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB eSIM"),
                        ImagenManualService.clave("Apple", "iPhone 17 Pro 512GB eSIM"));
        assertNotEquals(ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB eSIM"),
                        ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB SIM"));
    }
}
