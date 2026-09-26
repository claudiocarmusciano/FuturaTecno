package com.futuratecno.application;

import com.futuratecno.api.dto.CatalogoPaginaDTO;
import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Filtra, ordena y pagina el catálogo ya armado. Va en memoria y no en SQL a propósito: el filtro
 * y el orden por precio usan el precio de VENTA, que se calcula solo en {@link PrecioService}
 * (costo × flete × margen, con overrides por producto). Llevarlo a una consulta obligaría a
 * duplicar la fórmula, y un cliente podría filtrar por un precio y ver otro.
 */
public final class BusquedaCatalogo {

    public static final int POR_PAGINA_DEFAULT = 24;
    public static final int POR_PAGINA_MAXIMO = 96;

    /** Parámetros tal como llegan del catálogo público (?cat=&marca=&q=&min=&max=&orden=&page=&size=). */
    public record Filtro(Set<Long> idsCategoria, String marca, String texto,
                         BigDecimal precioMin, BigDecimal precioMax, String orden,
                         int pagina, int porPagina) {}

    private BusquedaCatalogo() {}

    public static CatalogoPaginaDTO buscar(List<ProductoCatalogoDTO> catalogo, Filtro f) {
        String q = f.texto() == null ? "" : f.texto().trim().toLowerCase(Locale.ROOT);
        String marca = f.marca() == null || f.marca().isBlank() ? null : f.marca();

        List<ProductoCatalogoDTO> filtrados = new ArrayList<>();
        for (ProductoCatalogoDTO p : catalogo) {
            if (f.idsCategoria() != null && !f.idsCategoria().contains(p.getCategoriaId())) continue;
            if (marca != null && !marca.equals(p.getMarca())) continue;
            if (!q.isEmpty() && !textoDe(p).contains(q)) continue;
            BigDecimal precio = precioDesde(p);
            // Sin precio (todas las variantes en 0) queda afuera si hay cualquier límite: el front
            // lo trataba como Infinity, que pasa un mínimo pero no un máximo. Se conserva eso.
            if (f.precioMin() != null && precio != null && precio.compareTo(f.precioMin()) < 0) continue;
            if (f.precioMax() != null && (precio == null || precio.compareTo(f.precioMax()) > 0)) continue;
            filtrados.add(p);
        }

        Comparator<ProductoCatalogoDTO> porPrecio = Comparator.comparing(
                BusquedaCatalogo::precioDesde, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("precio-desc".equals(f.orden())) {
            filtrados.sort(Comparator.comparing(BusquedaCatalogo::precioDesde,
                    Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder())));   // sin precio, al final siempre
        } else if (!"relevancia".equals(f.orden())) {
            filtrados.sort(porPrecio);   // default: del más barato al más caro
        }

        int porPagina = f.porPagina() <= 0 ? POR_PAGINA_DEFAULT : Math.min(f.porPagina(), POR_PAGINA_MAXIMO);
        int totalPaginas = Math.max(1, (filtrados.size() + porPagina - 1) / porPagina);
        // Una página fuera de rango devuelve la última, no una vacía: pasa al volver de un
        // producto cuando el catálogo cambió y hay menos páginas que antes.
        int pagina = Math.min(Math.max(1, f.pagina()), totalPaginas);
        int desde = (pagina - 1) * porPagina;
        List<ProductoCatalogoDTO> items = filtrados.subList(desde, Math.min(desde + porPagina, filtrados.size()));

        CatalogoPaginaDTO out = new CatalogoPaginaDTO();
        out.setItems(new ArrayList<>(items));
        out.setTotal(filtrados.size());
        out.setPagina(pagina);
        out.setPorPagina(porPagina);
        out.setTotalPaginas(totalPaginas);
        facetas(catalogo, out);
        return out;
    }

    /**
     * Lo que el catálogo necesita del listado COMPLETO, no del filtrado: el árbol lateral solo
     * muestra categorías con productos, el menú de marcas las lista todas, y el rango de precios
     * guía los campos mín/máx. Antes lo calculaba el navegador con los ~4.000 productos a mano.
     */
    private static void facetas(List<ProductoCatalogoDTO> catalogo, CatalogoPaginaDTO out) {
        out.setTotalCatalogo(catalogo.size());
        out.setMarcas(new ArrayList<>(catalogo.stream().map(ProductoCatalogoDTO::getMarca)
                .filter(m -> m != null && !m.isBlank()).collect(Collectors.toCollection(TreeSet::new))));
        out.setCategoriaIds(catalogo.stream().map(ProductoCatalogoDTO::getCategoriaId)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList()));
        List<BigDecimal> precios = catalogo.stream().map(BusquedaCatalogo::precioDesde)
                .filter(Objects::nonNull).toList();
        if (!precios.isEmpty()) {
            out.setPrecioMinUsd(precios.stream().min(Comparator.naturalOrder()).get().setScale(0, RoundingMode.FLOOR));
            out.setPrecioMaxUsd(precios.stream().max(Comparator.naturalOrder()).get().setScale(0, RoundingMode.CEILING));
        }
    }

    /** Precio "desde": el menor precio de venta USD entre las variantes con precio; null si ninguna tiene. */
    public static BigDecimal precioDesde(ProductoCatalogoDTO p) {
        if (p.getVariantes() == null) return null;
        return p.getVariantes().stream().map(VarianteCatalogoDTO::getPrecioUsd)
                .filter(v -> v != null && v.signum() > 0)
                .min(Comparator.naturalOrder()).orElse(null);
    }

    /** Lo que mira el buscador: subcategoría, marca, modelo y las especificaciones de cada variante. */
    private static String textoDe(ProductoCatalogoDTO p) {
        List<String> partes = new ArrayList<>();
        partes.add(p.getCategoria());
        partes.add(p.getMarca());
        partes.add(p.getModelo());
        if (p.getVariantes() != null) p.getVariantes().forEach(v -> partes.add(v.getEspecificaciones()));
        return partes.stream().filter(Objects::nonNull).collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT);
    }
}
