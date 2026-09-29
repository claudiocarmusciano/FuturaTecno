package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ImagenRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** La demora del proveedor llega al catálogo público, pero el proveedor no. */
class CatalogoDemoraEntregaTest {

    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final ProductoRepository productos = stub(ProductoRepository.class);
    private final VarianteRepository variantes = stub(VarianteRepository.class);
    private final ImagenRepository imagenes = stub(ImagenRepository.class);
    private final CotizacionService cotizacion = stub(CotizacionService.class);
    private final PrecioService precios = stub(PrecioService.class);
    private final CatalogoService catalogo = new CatalogoService(productos, variantes, imagenes, cotizacion,
            stub(CategoriaService.class), precios);

    private Producto producto(Integer min, Integer max) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Apple Trade");
        proveedor.setCodigo("APT");
        proveedor.setDemoraEntregaMinDias(min);
        proveedor.setDemoraEntregaMaxDias(max);
        Producto p = new Producto();
        p.setId(7L);
        p.setActivo(true);
        p.setMarca("Apple");
        p.setModelo("iPhone 18 Pro 256GB Black");
        p.setProveedor(proveedor);
        when(productos.findById(7L)).thenReturn(Optional.of(p));
        when(variantes.findByProductoIdAndActivo(7L, true)).thenReturn(List.of(new Variante()));
        when(imagenes.findByProductoIdAndActivoOrderByOrden(7L, true)).thenReturn(List.of());
        when(cotizacion.obtenerCotizacionUsdArs()).thenReturn(new BigDecimal("1500"));
        when(precios.precioVentaUsd(any(), any(), any())).thenReturn(new BigDecimal("1500"));
        when(precios.aArs(any(), any())).thenReturn(new BigDecimal("2250000"));
        return p;
    }

    @Test
    void conDemoraElProductoLaInformaSinNombrarAlProveedor() throws Exception {
        producto(15, 20);

        var dto = catalogo.obtenerProducto(7L);

        assertEquals(15, dto.getDemoraEntregaMinDias());
        assertEquals(20, dto.getDemoraEntregaMaxDias());
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(dto);
        // El SKU camuflado lleva el código corto del proveedor a propósito; el nombre, nunca.
        assertFalse(json.contains("Apple Trade") || json.contains("proveedor"), json);
    }

    @Test
    void sinDemoraEsLaEntregaNormal() {
        producto(null, null);

        var dto = catalogo.obtenerProducto(7L);

        assertNull(dto.getDemoraEntregaMinDias());
        assertNull(dto.getDemoraEntregaMaxDias());
    }
}
