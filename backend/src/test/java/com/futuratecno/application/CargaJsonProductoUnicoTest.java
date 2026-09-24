package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Recargar el listado de ayer no debe duplicar el producto porque la IA redactó el modelo distinto.
 * Duplicarlo significaba ofrecer el mismo artículo dos veces a precios distintos y volver a cargarle
 * la foto a mano. Desde la V42 lo decide la identidad y no el nombre: estos tests cubren la vía
 * con mocks; el comportamiento contra PostgreSQL está en CargaJsonIdentidadPostgresTest.
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
            mock(CategoriaClasificadorService.class), mock(CategoriaService.class), mock(ImageUrlValidatorService.class),
            new IdentidadProductoService(), mock(org.springframework.jdbc.core.JdbcTemplate.class));

    private Producto existente(String modelo) {
        Producto p = new Producto();
        p.setId(10L);
        p.setMarca("Apple");
        p.setModelo(modelo);
        return p;
    }

    private void cargar(String modeloQueEntra, Map<String, Object> especificaciones) {
        when(proveedores.findById(2L)).thenReturn(Optional.of(new Proveedor()));
        when(productos.saveAndFlush(any())).thenAnswer(inv -> { Producto p = inv.getArgument(0); if (p.getId() == null) p.setId(99L); return p; });
        var art = new ArticuloJsonDTO();
        art.setMarca("Apple");
        art.setModelo(modeloQueEntra);
        art.setPrecioUsd(new BigDecimal("1508.17"));
        art.setEspecificaciones(especificaciones);
        service.cargar(2L, List.of(art));
    }

    private void candidatos(Producto... ps) {
        when(productos.candidatosDeIdentidad(eq(2L), any(), any(), anyString(), anyString(), anyString())).thenReturn(List.of(ps));
    }

    /** El caso real: mismo teléfono, otra redacción. Tiene que actualizar, no crear. */
    @Test void reconoceElProductoAunqueCambieLaRedaccionDelModelo() {
        var yaCargado = existente("iPhone 17 Pro 256GB eSIM");
        candidatos(yaCargado);

        cargar("iPhone 17 Pro 256 GB (eSIM)", null);

        verify(productos).saveAndFlush(argThat(p -> Long.valueOf(10L).equals(p.getId())));
        assertEquals("iPhone 17 Pro 256GB eSIM", yaCargado.getModelo(),
                "el nombre publicado no debe cambiar en cada carga por cómo lo redactó la IA");
        assertNotNull(yaCargado.getIdentidadClave(), "el producto viejo adopta su identidad");
    }

    /**
     * El nombre exacto trae candidatos pero no decide: si los atributos difieren (acá la SIM, que
     * viene en las especificaciones), es otro artículo y el existente no se pisa.
     */
    @Test void unNombreExactoIgualNoAlcanzaSiLosAtributosDifieren() {
        var conSimFisica = existente("iPhone 17 Pro 256GB");
        Variante v = new Variante();
        v.setProducto(conSimFisica);
        v.setActivo(true);
        v.setEspecificaciones("SIM física");
        candidatos(conSimFisica);
        when(variantes.findByProductoIdIn(any())).thenReturn(List.of(v));

        cargar("iPhone 17 Pro 256GB", Map.of("otros", "eSIM"));

        verify(productos, never()).saveAndFlush(argThat(p -> Long.valueOf(10L).equals(p.getId())));
        verify(productos).saveAndFlush(argThat(p -> p.getId() == null || p.getId() == 99L));
    }

    /** Un artículo que de verdad es nuevo se sigue creando. */
    @Test void creaElProductoCuandoNoHayNadaParecido() {
        candidatos();

        cargar("iPhone 17 Pro 1TB eSIM", null);

        verify(productos).saveAndFlush(argThat(p -> "iPhone 17 Pro 1TB eSIM".equals(p.getModelo())));
    }

    /** Aflojar la clave no puede hacer que una capacidad caiga sobre otra. */
    @Test void laClaveSueltaNoConfundeCapacidades() {
        assertNotEquals(ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB eSIM"),
                        ImagenManualService.clave("Apple", "iPhone 17 Pro 512GB eSIM"));
        assertNotEquals(ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB eSIM"),
                        ImagenManualService.clave("Apple", "iPhone 17 Pro 256GB SIM"));
    }

    /**
     * La vista previa del borrador: el mismo teléfono escrito de dos formas tiene la misma
     * identidad (es duplicado), la misma redacción con otra RAM no (no es duplicado), y lo
     * incompleto se marca para revisión. No toca la base.
     */
    @Test void laVistaPreviaDelBorradorDistingueDuplicadosDeVariantes() {
        var a = new ArticuloJsonDTO(); a.setMarca("Motorola"); a.setModelo("G04 4G 64GB / 4GB RAM");
        a.setEspecificaciones(Map.of("almacenamiento", "64GB", "ram", "4GB"));
        var b = new ArticuloJsonDTO(); b.setMarca("Motorola"); b.setModelo("Motorola G04 4G 64GB");
        b.setEspecificaciones(Map.of("almacenamiento", "64GB", "ram", "4GB"));
        var c = new ArticuloJsonDTO(); c.setMarca("Motorola"); c.setModelo("Motorola G04 4G 64GB");
        c.setEspecificaciones(Map.of("almacenamiento", "64GB", "ram", "8GB"));
        var d = new ArticuloJsonDTO(); d.setMarca("Motorola"); d.setModelo("G04 4G 64GB");

        var filas = service.identidades(List.of(a, b, c, d));

        assertEquals(filas.get(0).get("identidad"), filas.get(1).get("identidad"));
        assertNotEquals(filas.get(1).get("identidad"), filas.get(2).get("identidad"));
        assertEquals("revision", filas.get(3).get("estado"));
        verifyNoInteractions(productos);
    }
}
