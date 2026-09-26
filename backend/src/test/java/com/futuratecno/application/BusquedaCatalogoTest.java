package com.futuratecno.application;

import com.futuratecno.api.dto.CatalogoPaginaDTO;
import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BusquedaCatalogoTest {

    private static ProductoCatalogoDTO producto(long id, long categoriaId, String marca, String modelo,
                                                String specs, String... precios) {
        List<VarianteCatalogoDTO> variantes = new ArrayList<>();
        for (String p : precios) variantes.add(new VarianteCatalogoDTO(id * 10, specs, new BigDecimal(p), null));
        ProductoCatalogoDTO dto = new ProductoCatalogoDTO(id, "Sub " + categoriaId, marca, modelo, null, variantes);
        dto.setCategoriaId(categoriaId);
        return dto;
    }

    private static BusquedaCatalogo.Filtro filtro(Set<Long> cats, String marca, String q, String min,
                                                  String max, String orden, int pagina, int porPagina) {
        return new BusquedaCatalogo.Filtro(cats, marca, q, min == null ? null : new BigDecimal(min),
                max == null ? null : new BigDecimal(max), orden, pagina, porPagina);
    }

    private static List<Long> ids(CatalogoPaginaDTO r) {
        return r.getItems().stream().map(ProductoCatalogoDTO::getId).toList();
    }

    private final List<ProductoCatalogoDTO> catalogo = List.of(
            producto(1, 10, "Apple", "iPhone 16 128GB", "Negro", "900"),
            producto(2, 10, "Samsung", "Galaxy S25", "12GB RAM", "700", "650"),
            producto(3, 20, "Logitech", "Mouse G203", "RGB", "25"),
            producto(4, 30, "Samsung", "Monitor Odyssey", "27 pulgadas", "300"),
            producto(5, 30, "LG", "Monitor sin precio", "", "0"));

    @Test
    void ordenaPorPrecioDesdeYElSinPrecioVaSiempreAlFinal() {
        var asc = BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, null, null, 1, 24));
        assertEquals(List.of(3L, 4L, 2L, 1L, 5L), ids(asc));   // el Galaxy cuenta por su variante de 650
        var desc = BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, null, "precio-desc", 1, 24));
        assertEquals(List.of(1L, 2L, 4L, 3L, 5L), ids(desc));
        var relevancia = BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, null, "relevancia", 1, 24));
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(relevancia));
    }

    @Test
    void filtraPorRamaMarcaTextoYPrecioComoLoHaciaElNavegador() {
        assertEquals(List.of(2L, 1L), ids(BusquedaCatalogo.buscar(catalogo, filtro(Set.of(10L), null, null, null, null, null, 1, 24))));
        assertEquals(List.of(4L, 2L), ids(BusquedaCatalogo.buscar(catalogo, filtro(null, "Samsung", null, null, null, null, 1, 24))));
        // El texto busca también en las especificaciones y sin distinguir mayúsculas.
        assertEquals(List.of(2L), ids(BusquedaCatalogo.buscar(catalogo, filtro(null, null, "  12gb ram ", null, null, null, 1, 24))));
        // Sin precio: pasa un mínimo (el front lo trataba como Infinity) pero no un máximo.
        assertEquals(List.of(4L, 2L, 1L, 5L), ids(BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, "50", null, null, 1, 24))));
        assertEquals(List.of(3L, 4L, 2L), ids(BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, "650", null, 1, 24))));
    }

    @Test
    void paginaYAjustaUnaPaginaFueraDeRangoALaUltima() {
        var p2 = BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, null, null, 2, 2));
        assertEquals(List.of(2L, 1L), ids(p2));
        assertEquals(5, p2.getTotal());
        assertEquals(3, p2.getTotalPaginas());

        var fuera = BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, null, null, 99, 2));
        assertEquals(3, fuera.getPagina());
        assertEquals(List.of(5L), ids(fuera));

        var gigante = BusquedaCatalogo.buscar(catalogo, filtro(null, null, null, null, null, null, 1, 10_000));
        assertEquals(BusquedaCatalogo.POR_PAGINA_MAXIMO, gigante.getPorPagina());
    }

    @Test
    void lasFacetasSalenDelCatalogoCompletoNoDelFiltrado() {
        var r = BusquedaCatalogo.buscar(catalogo, filtro(Set.of(20L), null, null, null, null, null, 1, 24));
        assertEquals(1, r.getTotal());
        assertEquals(5, r.getTotalCatalogo());
        assertEquals(List.of("Apple", "LG", "Logitech", "Samsung"), r.getMarcas());
        assertEquals(List.of(10L, 20L, 30L), r.getCategoriaIds());
        assertEquals(new BigDecimal("25"), r.getPrecioMinUsd());
        assertEquals(new BigDecimal("900"), r.getPrecioMaxUsd());
    }

    @Test
    void catalogoVacioDevuelveUnaPaginaVaciaSinRango() {
        var r = BusquedaCatalogo.buscar(List.of(), filtro(null, null, null, null, null, null, 3, 24));
        assertEquals(0, r.getTotal());
        assertEquals(1, r.getPagina());
        assertNull(r.getPrecioMinUsd());
    }
}
