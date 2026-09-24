package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** La descripción escrita a mano sobrevive a un reimport, igual que la imagen manual. */
class CargaJsonDescripcionManualTest {

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    @Test void reutilizaLaDescripcionRecordadaYPriorizaSobreElJson() {
        comprobar(Optional.of("Pantalla 6,8\" · 12 GB RAM"), "Pantalla 6,8\" · 12 GB RAM");
    }

    @Test void usaLaDelJsonCuandoNoHayNadaRecordado() {
        comprobar(Optional.empty(), "Snapdragon");
    }

    @Test void normalizacionConservaVariantes() {
        assertEquals("nano 64gb", DescripcionManualService.normalizar(" NANO  64GB "));
        assertNotEquals(DescripcionManualService.normalizar("Nano 64GB"),
                DescripcionManualService.normalizar("Nano 128GB"));
    }

    @Test void reconoceLasFuentesDeMayorista() {
        assertTrue(DescripcionManualService.esDeMayorista("ELIT"));
        assertTrue(DescripcionManualService.esDeMayorista(" invid "));
        assertFalse(DescripcionManualService.esDeMayorista("JSON"));
        assertFalse(DescripcionManualService.esDeMayorista(null));
    }

    private void comprobar(Optional<String> recordada, String esperada) {
        var productos = mock(ProductoRepository.class);
        var variantes = mock(VarianteRepository.class);
        var proveedores = mock(ProveedorRepository.class);
        var descripciones = mock(DescripcionManualService.class);

        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.candidatosDeIdentidad(eq(2L), any(), any(), anyString(), anyString(), anyString())).thenReturn(List.of());
        when(productos.saveAndFlush(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); p.setId(10L); return p; });
        when(descripciones.buscar(anyList())).thenReturn(recordada);
        when(variantes.findByProductoIdAndActivo(anyLong(), anyBoolean())).thenReturn(List.of());

        var service = new CargaJsonService(mock(ImagenManualService.class), descripciones,
                mock(AtributosManualService.class), mock(MargenManualService.class), productos, variantes, proveedores, mock(ImagenRepository.class),
                mock(CategoriaClasificadorService.class), mock(CategoriaService.class), mock(ImageUrlValidatorService.class),
                new IdentidadProductoService(), mock(org.springframework.jdbc.core.JdbcTemplate.class));

        var art = new ArticuloJsonDTO();
        art.setMarca("DJI");
        art.setModelo("Nano 64GB");
        art.setPrecioUsd(BigDecimal.TEN);
        art.setEspecificaciones(Map.of("procesador", "Snapdragon"));

        service.cargar(2L, List.of(art));

        verify(variantes).save(argThat(v -> esperada.equals(v.getEspecificaciones())));
    }
}
