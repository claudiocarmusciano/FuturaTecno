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
import java.util.ArrayList;

/** Cozzo: el mismo artículo con dos precios vale el más alto, en la misma carga y entre listas del día. */
class CargaJsonCozzoTest {

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    @Test void enLaMismaCargaQuedaElMasCaro() {
        var guardadas = cargar("100", LocalDateTime.now().minusDays(1), "106", "109");
        assertEquals(1, guardadas.size());
        assertEquals(new BigDecimal("109.00"), guardadas.getFirst().getCostoUsd());
    }

    @Test void siOtraListaDeHoyLoTrajoMasCaroSeMantiene() {
        var guardadas = cargar("120", LocalDateTime.now(), "109");
        assertEquals(new BigDecimal("120"), guardadas.getFirst().getCostoUsd());
    }

    @Test void conUnaListaDeAyerSeActualizaAunqueBaje() {
        var guardadas = cargar("120", LocalDateTime.now().minusDays(1), "109");
        assertEquals(new BigDecimal("109.00"), guardadas.getFirst().getCostoUsd());
    }

    @Test void debajoDe50NoSeCarga() {
        assertTrue(cargar("100", null, "45").isEmpty());
    }

    private List<Variante> cargar(String costoExistente, LocalDateTime visto, String... precios) {
        var productos = mock(ProductoRepository.class);
        var variantes = mock(VarianteRepository.class);
        var proveedores = mock(ProveedorRepository.class);

        Producto producto = new Producto();
        producto.setId(10L);
        producto.setMarca("Motorola");
        producto.setModelo("G06 4G 64GB");
        producto.setVistoEnSyncAt(visto);
        var r = new IdentidadProductoService().resolver("Motorola", "G06 4G 64GB", Map.of("ram", "4GB", "almacenamiento", "64GB"), "Celulares");
        producto.setIdentidadClave(r.clave());
        producto.setIdentidadVersion(r.version());
        producto.setIdentidadFamilia(r.familia());
        producto.setIdentidadAtributos(CargaJsonService.escribirAtributos(r.atributos()));
        Proveedor cozzo = new Proveedor();
        cozzo.setNombre("Dropshiping Diego Cozzo");

        when(proveedores.findById(2L)).thenReturn(Optional.of(cozzo));
        when(productos.candidatosDeIdentidad(eq(2L), any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(producto));
        when(productos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        List<Variante> existentes = new ArrayList<>();
        if (costoExistente != null) {
            Variante v = new Variante();
            v.setId(5L);
            v.setCostoUsd(new BigDecimal(costoExistente));
            v.setActivo(true);
            v.setEspecificaciones("Ram: 4GB · Almacenamiento: 64GB");
            existentes.add(v);
        }
        when(variantes.findByProductoIdAndActivo(10L, true)).thenReturn(existentes);

        var service = new CargaJsonService(mock(ImagenManualService.class),
                mock(DescripcionManualService.class), mock(AtributosManualService.class), mock(MargenManualService.class),
                productos, variantes, proveedores, mock(ImagenRepository.class),
                mock(CategoriaClasificadorService.class), mock(CategoriaService.class), mock(ImageUrlValidatorService.class),
                new IdentidadProductoService(), mock(org.springframework.jdbc.core.JdbcTemplate.class));

        List<ArticuloJsonDTO> lista = new ArrayList<>();
        for (String p : precios) {
            var art = new ArticuloJsonDTO();
            art.setMarca("Motorola");
            art.setModelo("G06 4G 64GB");
            art.setCategoria("Celulares");
            art.setPrecioUsd(new BigDecimal(p));
            art.setEspecificaciones(Map.of("ram", "4GB", "almacenamiento", "64GB"));
            lista.add(art);
        }
        service.cargar(2L, lista);

        var capturadas = org.mockito.ArgumentCaptor.forClass(Variante.class);
        verify(variantes, atLeast(0)).save(capturadas.capture());
        return capturadas.getAllValues();
    }
}
