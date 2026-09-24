package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Un producto cargado por JSON tiene una sola variante. Antes se la buscaba por el texto exacto
 * de las especificaciones, que redacta la IA: cuando cambiaba la redacción nacía una segunda
 * variante con otro precio y el catálogo ofrecía el mismo teléfono dos veces (ver V35).
 */
class CargaJsonVarianteUnicaTest {

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private Variante variante(long id, String especificaciones, String costo, LocalDateTime actualizada) {
        Variante v = new Variante();
        v.setId(id);
        v.setEspecificaciones(especificaciones);
        v.setCostoUsd(new BigDecimal(costo));
        v.setUpdatedAt(actualizada);
        v.setActivo(true);
        return v;
    }

    /** El caso real: "256GB · Versión eSIM" y "256GB · eSIM" son el mismo teléfono. */
    @Test void reusaLaVarianteExistenteAunqueCambieLaRedaccionDeLasSpecs() {
        var guardadas = cargar(List.of(variante(77L, "256GB · Versión eSIM", "1446.59",
                LocalDateTime.of(2026, 9, 12, 2, 49))), "eSIM");

        assertEquals(1, guardadas.size(), "no debe nacer una variante nueva");
        Variante v = guardadas.getFirst();
        assertEquals(77L, v.getId(), "tiene que actualizar la que ya existía");
        assertEquals(new BigDecimal("1508.17"), v.getCostoUsd());
        assertEquals("256GB · eSIM", v.getEspecificaciones(), "la descripción se actualiza igual");
    }

    /** Si quedaron varias activas, gana la tocada más recientemente: la del precio vigente. */
    @Test void anteVariasActivasActualizaLaMasReciente() {
        var guardadas = cargar(List.of(
                variante(77L, "256GB · Versión eSIM", "1446.59", LocalDateTime.of(2026, 9, 1, 0, 0)),
                variante(88L, "256GB · eSIM", "1508.17", LocalDateTime.of(2026, 9, 12, 2, 49))), "eSIM");

        assertEquals(88L, guardadas.getFirst().getId());
    }

    @Test void creaLaVarianteCuandoElProductoTodaviaNoTiene() {
        var guardadas = cargar(List.of(), "eSIM");

        assertEquals(1, guardadas.size());
        assertNull(guardadas.getFirst().getId());
        assertEquals(new BigDecimal("1508.17"), guardadas.getFirst().getCostoUsd());
    }

    /** Carga un iPhone a US$ 1508,17 sobre un producto que ya existe con las variantes dadas. */
    private List<Variante> cargar(List<Variante> existentes, String specs) {
        var productos = mock(ProductoRepository.class);
        var variantes = mock(VarianteRepository.class);
        var proveedores = mock(ProveedorRepository.class);

        Producto producto = new Producto();
        producto.setId(10L);
        producto.setMarca("Apple");
        producto.setModelo("iPhone 17 Pro 256GB eSIM");

        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.candidatosDeIdentidad(eq(2L), any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(producto));
        when(productos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(variantes.findByProductoIdAndActivo(10L, true)).thenReturn(existentes);

        var service = new CargaJsonService(mock(ImagenManualService.class),
                mock(DescripcionManualService.class), mock(AtributosManualService.class), mock(MargenManualService.class),
                productos, variantes, proveedores, mock(ImagenRepository.class),
                mock(CategoriaClasificadorService.class), mock(CategoriaService.class), mock(ImageUrlValidatorService.class),
                new IdentidadProductoService(), mock(org.springframework.jdbc.core.JdbcTemplate.class));

        var art = new ArticuloJsonDTO();
        art.setMarca("Apple");
        art.setModelo("iPhone 17 Pro 256GB eSIM");
        art.setPrecioUsd(new BigDecimal("1508.17"));
        art.setEspecificaciones(Map.of("almacenamiento", "256GB", "otros", specs));

        service.cargar(2L, List.of(art));

        var capturadas = org.mockito.ArgumentCaptor.forClass(Variante.class);
        verify(variantes, atLeastOnce()).save(capturadas.capture());
        return capturadas.getAllValues();
    }
}
