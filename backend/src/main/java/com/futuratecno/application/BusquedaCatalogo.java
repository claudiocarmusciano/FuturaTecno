package com.futuratecno.application;

import com.futuratecno.api.dto.CatalogoPaginaDTO;
import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                         int pagina, int porPagina,
                         FiltrosCategoria.Grupo grupo, Map<String, List<String>> atributos) {
        public Filtro(Set<Long> idsCategoria, String marca, String texto, BigDecimal precioMin,
                      BigDecimal precioMax, String orden, int pagina, int porPagina) {
            this(idsCategoria, marca, texto, precioMin, precioMax, orden, pagina, porPagina, null, Map.of());
        }
    }

    private BusquedaCatalogo() {}

    public static CatalogoPaginaDTO buscar(List<ProductoCatalogoDTO> catalogo, Filtro f) {
        String q = f.texto() == null ? "" : f.texto().trim().toLowerCase(Locale.ROOT);
        String marca = f.marca() == null || f.marca().isBlank() ? null : f.marca();

        // Filtros por atributo de la categoría (iPhone, Celulares…): valores normalizados por clave.
        Map<String, Map<String, String>> elegidos = elegidos(f);
        List<ProductoCatalogoDTO> base = new ArrayList<>();   // todo menos los filtros por atributo
        for (ProductoCatalogoDTO p : catalogo) {
            if (f.idsCategoria() != null && !f.idsCategoria().contains(p.getCategoriaId())) continue;
            if (marca != null && !marca.equals(p.getMarca())) continue;
            if (!q.isEmpty() && !textoDe(p).contains(q)) continue;
            BigDecimal precio = precioDesde(p);
            // Sin precio (todas las variantes en 0) queda afuera si hay cualquier límite: el front
            // lo trataba como Infinity, que pasa un mínimo pero no un máximo. Se conserva eso.
            if (f.precioMin() != null && precio != null && precio.compareTo(f.precioMin()) < 0) continue;
            if (f.precioMax() != null && (precio == null || precio.compareTo(f.precioMax()) > 0)) continue;
            base.add(p);
        }
        List<ProductoCatalogoDTO> filtrados = new ArrayList<>();
        for (ProductoCatalogoDTO p : base) if (cumple(p, elegidos, null)) filtrados.add(p);

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
        if (f.grupo() != null) out.setFiltros(opciones(base, f.grupo(), elegidos));
        return out;
    }

    // ------------------------------------------------------------------ Filtros por atributo

    /** Por atributo: clave normalizada del valor elegido → el texto tal como vino (para mostrarlo). */
    private static Map<String, Map<String, String>> elegidos(Filtro f) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        if (f.grupo() == null || f.atributos() == null) return out;
        for (var d : FiltrosCategoria.DEFINICIONES.get(f.grupo())) {
            List<String> valores = f.atributos().get(d.clave());
            if (valores == null) continue;
            Map<String, String> norm = new LinkedHashMap<>();
            for (String v : valores) if (v != null && !v.isBlank()) norm.putIfAbsent(clave(v), v.trim());
            if (!norm.isEmpty()) out.put(d.clave(), norm);
        }
        return out;
    }

    /**
     * ¿El producto pasa los filtros elegidos, salvo {@code ignorar}? Dentro de un atributo alcanza
     * con uno de los valores (256GB o 512GB); entre atributos tienen que cumplirse todos. Sin el
     * dato no pasa: no se puede afirmar que sea lo que se pidió (regla acordada, 2026-09-29).
     */
    private static boolean cumple(ProductoCatalogoDTO p, Map<String, Map<String, String>> elegidos, String ignorar) {
        for (var e : elegidos.entrySet()) {
            if (e.getKey().equals(ignorar)) continue;
            String v = p.getFiltros().get(e.getKey());
            if (v == null || !e.getValue().containsKey(clave(v))) return false;
        }
        return true;
    }

    /**
     * Las opciones de cada filtro con su cantidad. Cada atributo se cuenta aplicando los OTROS
     * filtros y no el propio, así con "256GB" elegido se sigue viendo cuántos hay de 512GB.
     */
    private static List<CatalogoPaginaDTO.FiltroDTO> opciones(List<ProductoCatalogoDTO> base,
                                                           FiltrosCategoria.Grupo grupo, Map<String, Map<String, String>> elegidos) {
        List<CatalogoPaginaDTO.FiltroDTO> out = new ArrayList<>();
        for (var d : FiltrosCategoria.DEFINICIONES.get(grupo)) {
            Map<String, Integer> cantidad = new HashMap<>();
            Map<String, Map<String, Integer>> formas = new HashMap<>();   // "Ice Blue" / "Iceblue": se muestra la más usada
            for (ProductoCatalogoDTO p : base) {
                if (!cumple(p, elegidos, d.clave())) continue;
                String v = p.getFiltros().get(d.clave());
                if (v == null) continue;
                cantidad.merge(clave(v), 1, Integer::sum);
                formas.computeIfAbsent(clave(v), k -> new HashMap<>()).merge(v, 1, Integer::sum);
            }
            Map<String, String> sel = elegidos.getOrDefault(d.clave(), Map.of());
            // Un valor elegido que se quedó sin productos se sigue mostrando, para poder sacarlo.
            sel.keySet().forEach(k -> cantidad.putIfAbsent(k, 0));
            List<CatalogoPaginaDTO.OpcionDTO> ops = new ArrayList<>();
            for (var e : cantidad.entrySet()) {
                String visible = formas.containsKey(e.getKey())
                        ? formas.get(e.getKey()).entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey()
                        : sel.get(e.getKey());
                ops.add(new CatalogoPaginaDTO.OpcionDTO(visible, e.getValue(), sel.containsKey(e.getKey())));
            }
            ops.sort(orden(d.clave()));
            if (!ops.isEmpty()) out.add(new CatalogoPaginaDTO.FiltroDTO(d.clave(), d.nombre(), ops));
        }
        return out;
    }

    private static final List<String> ORDEN_VERSION = List.of("estandar", "e", "mini", "plus", "air", "pro", "promax");

    private static Comparator<CatalogoPaginaDTO.OpcionDTO> orden(String clave) {
        return switch (clave) {
            // Lo más nuevo primero: es lo que más se busca.
            case "generacion" -> Comparator.comparing((CatalogoPaginaDTO.OpcionDTO o) -> numero(o.valor())).reversed();
            case "version" -> Comparator.comparing(o -> {
                int i = ORDEN_VERSION.indexOf(clave(o.valor()));
                return i < 0 ? 99 : i;
            });
            case "capacidad", "ram", "red", "hz", "vram", "mhz", "ddr", "modulos", "almacenamiento" -> Comparator.comparing(o -> gigas(o.valor()));
            // Notebooks: "Integrada" primero y después las dedicadas de la más potente a la menos.
            case "gpu" -> Comparator.comparing((CatalogoPaginaDTO.OpcionDTO o) -> !"Integrada".equals(o.valor()))
                    .thenComparing(o -> -numero(o.valor().replaceAll("(?i)ti|super", "")))
                    .thenComparing(o -> -sufijoGpu(o.valor()));
            // Procesadores agrupados por marca y línea, de menor a mayor gama.
            case "procesador" -> Comparator.comparing((CatalogoPaginaDTO.OpcionDTO o) -> familiaCpu(o.valor()))
                    .thenComparing(o -> numero(o.valor()));
            case "formato" -> Comparator.comparing(o -> {
                int i = ORDEN_FORMATO.indexOf(o.valor());
                return i < 0 ? 99 : i;
            });
            case "potencia" -> Comparator.comparing(o -> numero(o.valor()));
            case "certificacion" -> Comparator.comparing(o -> {
                int i = ORDEN_CERTIFICACION.indexOf(o.valor());
                return i < 0 ? 99 : i;
            });
            case "socket", "chipset" -> Comparator.comparing(CatalogoPaginaDTO.OpcionDTO::valor);
            case "video", "cooler" -> Comparator.comparing(o -> !o.valor().startsWith("Con"));
            case "rgb" -> Comparator.comparing(o -> !"Con RGB".equals(o.valor()));
            case "pulgadas" -> Comparator.comparing(o -> Double.parseDouble(o.valor().replaceAll("[^0-9.]", "")));
            case "resolucion" -> Comparator.comparing(o -> {
                int i = ORDEN_RESOLUCION.indexOf(o.valor());
                return i < 0 ? 99 : i;
            });
            case "pantalla" -> Comparator.comparing(o -> !"Plana".equals(o.valor()));
            // Placas: por marca de chip y lo más nuevo primero (RTX 5070 antes que RTX 3060).
            case "serie", "chip" -> Comparator.comparing((CatalogoPaginaDTO.OpcionDTO o) -> familiaGpu(o.valor()))
                    .thenComparing(o -> -numero(o.valor().replaceAll("(?i)ti|super|xtx|xt|gre", "")))
                    .thenComparing(o -> -sufijoGpu(o.valor()))   // RTX 5070 Ti antes que RTX 5070
                    .thenComparing(CatalogoPaginaDTO.OpcionDTO::valor);
            // Colores por cantidad; "Color a consultar" siempre al final.
            default -> Comparator.comparing((CatalogoPaginaDTO.OpcionDTO o) -> FiltrosCategoria.COLOR_A_CONSULTAR.equals(o.valor()))
                    .thenComparing(Comparator.comparingInt(CatalogoPaginaDTO.OpcionDTO::cantidad).reversed())
                    .thenComparing(CatalogoPaginaDTO.OpcionDTO::valor);
        };
    }

    private static final List<String> ORDEN_RESOLUCION = List.of(
            "HD", "Full HD", "UltraWide Full HD", "2K (QHD)", "UltraWide QHD", "Dual QHD", "4K", "8K");

    private static final List<String> ORDEN_FORMATO = List.of("PC", "Notebook (SODIMM)", "Mini-ITX", "Micro-ATX", "ATX", "E-ATX");

    private static final List<String> ORDEN_CERTIFICACION = List.of(
            "Titanium", "Platinum", "Gold", "Silver", "Bronze", "80 Plus", "Sin certificación");

    private static int familiaGpu(String v) {
        String s = v.toUpperCase(Locale.ROOT);
        if (s.startsWith("PROFESIONAL") || s.startsWith("RTX PRO") || s.matches("RTX A\\d+.*")) return 5;
        if (s.startsWith("RTX")) return 0;
        if (s.startsWith("RX")) return 1;
        if (s.startsWith("ARC")) return 2;
        if (s.startsWith("GTX")) return 3;
        return 4;   // GT
    }

    private static final List<String> FAMILIAS_CPU = List.of(
            "Intel Core Ultra", "Intel Core i", "Intel Core", "Intel N", "AMD Ryzen AI", "AMD Ryzen", "AMD Athlon", "Snapdragon");

    private static int familiaCpu(String v) {
        for (int i = 0; i < FAMILIAS_CPU.size(); i++) if (v.startsWith(FAMILIAS_CPU.get(i))) return i;
        return 99;
    }

    /** Cuánto suma el sufijo del chip: XTX > XT / Ti SUPER > Ti / SUPER / GRE > nada. */
    private static int sufijoGpu(String v) {
        String s = v.toUpperCase(Locale.ROOT);
        if (s.endsWith("XTX")) return 3;
        if (s.endsWith(" XT") || s.endsWith("TI SUPER")) return 2;
        if (s.endsWith(" TI") || s.endsWith("SUPER") || s.endsWith("GRE")) return 1;
        return 0;
    }

        private static int numero(String s) {
        String d = s.replaceAll("\\D", "");
        return d.isEmpty() ? 0 : Integer.parseInt(d);
    }

    /** "512GB" → 512, "1TB" → 1024, "5G" → 5. */
    private static int gigas(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)?").matcher(s);
        double n = m.find() ? Double.parseDouble(m.group()) : 0;   // "1.92TB": el decimal cuenta
        return (int) Math.round(s.toUpperCase(Locale.ROOT).endsWith("TB") ? n * 1024 : n);
    }

    /** Clave de comparación: minúsculas, sin tildes ni espacios ("Ice Blue" = "Iceblue", "Estándar" = "estandar"). */
    static String clave(String v) {
        String sinTildes = java.text.Normalizer.normalize(v, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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
