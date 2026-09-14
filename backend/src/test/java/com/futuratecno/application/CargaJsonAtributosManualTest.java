package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.application.AtributosManualService.Atributos;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Categoría y medidas recordadas por marca+modelo: un artículo ya curado no debe volver a nacer
 * sin categoría (y por lo tanto sin peso, sin poder cotizar envío) al cargarlo con otro proveedor.
 */
class CargaJsonAtributosManualTest {

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    @Test void rellenaCategoriaYMedidasEnUnProductoNuevo() {
        Producto guardado = cargar(new Producto(), new Atributos(7L, 450, 20, 15, 3));
        assertEquals(7L, guardado.getCategoriaId());
        assertEquals(450, guardado.getPesoGramos());
        assertEquals(20, guardado.getAltoCm());
        assertEquals(15, guardado.getAnchoCm());
        assertEquals(3, guardado.getLargoCm());
    }

    @Test void noPisaLoQueElProductoYaTiene() {
        Producto existente = new Producto();
        existente.setCategoriaId(99L);
        existente.setPesoGramos(1000);
        Producto guardado = cargar(existente, new Atributos(7L, 450, 20, 15, 3));
        assertEquals(99L, guardado.getCategoriaId(), "la categoría propia manda sobre la recordada");
        assertEquals(1000, guardado.getPesoGramos(), "el peso propio manda sobre el recordado");
        assertEquals(20, guardado.getAltoCm(), "pero los huecos sí se completan");
    }

    @Test void sinMemoriaNoInventaNada() {
        Producto guardado = cargar(new Producto(), null);
        assertNull(guardado.getPesoGramos());
        assertNull(guardado.getAltoCm());
    }

    @Test void laMemoriaEvitaVolverAClasificar() {
        var clasificador = mock(CategoriaClasificadorService.class);
        cargar(new Producto(), new Atributos(7L, null, null, null, null), clasificador);
        verify(clasificador, never()).clasificar(any(), anyString());
    }

    @Test void siLaMemoriaNoTraeCategoriaSeClasifica() {
        var clasificador = mock(CategoriaClasificadorService.class);
        cargar(new Producto(), new Atributos(null, 450, null, null, null), clasificador);
        verify(clasificador).clasificar(any(), any());
    }

    private Producto cargar(Producto inicial, Atributos recordados) {
        return cargar(inicial, recordados, mock(CategoriaClasificadorService.class));
    }

    private Producto cargar(Producto inicial, Atributos recordados, CategoriaClasificadorService clasificador) {
        var productos = mock(ProductoRepository.class);
        var variantes = mock(VarianteRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        // El servicio real, con un buscar() fijo: así se ejercita aplicar() de verdad.
        var atributos = mock(AtributosManualService.class);
        when(atributos.buscar(anyString(), anyString()))
                .thenReturn(Optional.ofNullable(recordados));
        doCallRealMethod().when(atributos).aplicar(any());

        // Una fila existente ya trae marca y modelo de la base: el servicio solo los asigna cuando
        // el producto es nuevo. Sin esto, aplicar() buscaría la memoria con la marca vacía.
        inicial.setMarca("DJI");
        inicial.setModelo("Nano 64GB");

        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.findByProveedorIdAndMarcaAndModelo(2L, "DJI", "Nano 64GB"))
                .thenReturn(inicial.getCategoriaId() == null && inicial.getPesoGramos() == null
                        ? Optional.empty() : Optional.of(inicial));
        when(productos.save(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(variantes.findByProductoIdAndActivo(anyLong(), anyBoolean())).thenReturn(List.of());

        var service = new CargaJsonService(mock(ImagenManualService.class),
                mock(DescripcionManualService.class), atributos,
                productos, variantes, proveedores, mock(ImagenRepository.class),
                clasificador, mock(CategoriaService.class));

        var art = new ArticuloJsonDTO();
        art.setMarca("DJI");
        art.setModelo("Nano 64GB");
        art.setPrecioUsd(BigDecimal.TEN);
        service.cargar(2L, List.of(art));

        var captor = org.mockito.ArgumentCaptor.forClass(Producto.class);
        verify(productos).save(captor.capture());
        return captor.getValue();
    }
}
