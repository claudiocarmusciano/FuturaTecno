package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Importa el catálogo de Invid a FuturaTecno usando {@link InvidApiClient}.
 * Cada artículo -> un Producto + una Variante (1:1), deduplicando por el ID de Invid (codigo_externo,
 * fuente "INVID"). Precios en ARS (FINAL_PRICE = precio + IVA + imp. internos) convertidos a USD con la
 * cotización del dólar oficial. El margen y flete los pone el proveedor "Invid" en FuturaTecno.
 */
@Service
public class InvidImportService {
    private static final Logger logger = LoggerFactory.getLogger(InvidImportService.class);
    private static final String NOMBRE_PROVEEDOR = "Invid";
    private static final String FUENTE = "INVID";

    private final InvidApiClient invidApiClient;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final CotizacionService cotizacionService;

    public InvidImportService(InvidApiClient invidApiClient,
                              ProductoRepository productoRepository,
                              VarianteRepository varianteRepository,
                              ProveedorRepository proveedorRepository,
                              CotizacionService cotizacionService) {
        this.invidApiClient = invidApiClient;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.proveedorRepository = proveedorRepository;
        this.cotizacionService = cotizacionService;
    }

    public boolean estaConfigurado() {
        return invidApiClient.estaConfigurado();
    }

    public Map<String, Object> filtros() {
        List<JsonNode> arts = invidApiClient.obtenerArticulos();
        TreeSet<String> categorias = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> marcas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (JsonNode a : arts) {
            String c = txt(a, "CATEGORY");
            String m = txt(a, "BRAND");
            if (c != null) categorias.add(c);
            if (m != null) marcas.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("categorias", new ArrayList<>(categorias));
        out.put("marcas", new ArrayList<>(marcas));
        return out;
    }

    public Map<String, Object> previsualizar(String categoria, String marca, BigDecimal precioMinUsd) {
        List<JsonNode> arts = invidApiClient.obtenerArticulos();
        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
        int total = 0;
        List<String> muestra = new ArrayList<>();
        for (JsonNode a : arts) {
            if (!coincide(a, categoria, marca)) continue;
            BigDecimal precio = parsePrecio(txt(a, "FINAL_PRICE"));
            if (precio.signum() <= 0) continue;
            if (precioMinUsd != null && costoUsdInvid(a, precio, cotizacion).compareTo(precioMinUsd) < 0) continue;
            total++;
            if (muestra.size() < 5) muestra.add(txt(a, "TITLE"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("muestra", muestra);
        return out;
    }

    @Transactional
    public Map<String, Object> importar(String categoria, String marca, boolean soloConStock, BigDecimal precioMinUsd) {
        return procesar(categoria, marca, soloConStock, precioMinUsd, false);
    }

    /** Sincroniza: solo actualiza precio/stock/imagen de los productos de Invid ya importados (no crea nuevos). */
    @Transactional
    public Map<String, Object> sincronizar() {
        return procesar(null, null, false, null, true);
    }

    private Map<String, Object> procesar(String categoria, String marca, boolean soloConStock,
                                         BigDecimal precioMinUsd, boolean soloExistentes) {
        if (!estaConfigurado()) {
            throw new IllegalStateException("La API de Invid no está configurada (faltan INVID_BASE_URL / INVID_USERNAME / INVID_PASSWORD).");
        }
        List<JsonNode> arts = invidApiClient.obtenerArticulos();
        Proveedor proveedor = obtenerOcrearProveedor();
        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();

        int creados = 0, actualizados = 0, salteadosSinStock = 0, salteadosSinPrecio = 0, salteadosPorPrecio = 0;

        for (JsonNode art : arts) {
            if (!coincide(art, categoria, marca)) continue;

            BigDecimal precioOrigen = parsePrecio(txt(art, "FINAL_PRICE"));
            if (precioOrigen.signum() <= 0) { salteadosSinPrecio++; continue; }

            Integer stock = art.path("STOCK").isNumber() ? art.path("STOCK").asInt() : null;
            if (soloConStock && stock != null && stock <= 0) { salteadosSinStock++; continue; }

            if (precioMinUsd != null && costoUsdInvid(art, precioOrigen, cotizacion).compareTo(precioMinUsd) < 0) {
                salteadosPorPrecio++;
                continue;
            }

            String estado = upsert(proveedor, art, precioOrigen, stock, cotizacion, soloExistentes);
            if ("creado".equals(estado)) creados++;
            else if ("actualizado".equals(estado)) actualizados++;
        }

        String mensaje = soloExistentes
                ? String.format("Sincronización de Invid: %d productos actualizados.", actualizados)
                : String.format("Importación de Invid completada: %d nuevos, %d actualizados, %d sin stock, %d sin precio, %d por debajo del precio mínimo.",
                        creados, actualizados, salteadosSinStock, salteadosSinPrecio, salteadosPorPrecio);
        logger.info(mensaje);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("creados", creados);
        out.put("actualizados", actualizados);
        out.put("salteadosSinStock", salteadosSinStock);
        out.put("salteadosSinPrecio", salteadosSinPrecio);
        out.put("salteadosPorPrecio", salteadosPorPrecio);
        out.put("mensaje", mensaje);
        return out;
    }

    /** Costo en USD del artículo de Invid (convierte ARS con la cotización; si ya es USD lo deja). */
    private BigDecimal costoUsdInvid(JsonNode art, BigDecimal precioOrigen, BigDecimal cotizacion) {
        String moneda = txt(art, "CURRENCY");
        boolean esUsd = moneda != null && moneda.toUpperCase().contains("USD");
        return esUsd ? precioOrigen.setScale(2, RoundingMode.HALF_UP)
                     : precioOrigen.divide(cotizacion, 2, RoundingMode.HALF_UP);
    }

    private String upsert(Proveedor proveedor, JsonNode art, BigDecimal precioArs, Integer stock, BigDecimal cotizacion, boolean soloExistentes) {
        String codigoExterno = txt(art, "ID");
        Producto existente = productoRepository
                .findByProveedorIdAndCodigoExterno(proveedor.getId(), codigoExterno).orElse(null);
        if (soloExistentes && existente == null) return "salteado";

        String marca = txt(art, "BRAND");
        String titulo = txt(art, "TITLE");
        String modelo = modeloDesde(marca, titulo);
        String categoria = txt(art, "CATEGORY");
        String imagen = txt(art, "IMAGE_URL");
        String especificaciones = txt(art, "DESCRIPTION");
        if (especificaciones == null) especificaciones = txt(art, "LONG_DESCRIPTION");
        if (especificaciones == null) especificaciones = "";

        String moneda = txt(art, "CURRENCY");
        boolean esUsd = moneda != null && moneda.toUpperCase().contains("USD");
        BigDecimal costoUsd = esUsd
                ? precioArs.setScale(2, RoundingMode.HALF_UP)
                : precioArs.divide(cotizacion, 2, RoundingMode.HALF_UP);

        final String marcaF = marca != null ? marca : "—";
        final String modeloF = modelo != null ? modelo : (titulo != null ? titulo : "Producto " + codigoExterno);

        boolean nuevo = existente == null;
        Producto producto = existente != null ? existente : new Producto();
        if (nuevo) {
            producto.setProveedor(proveedor);
            producto.setCodigoExterno(codigoExterno);
            producto.setFuente(FUENTE);
        }
        producto.setMarca(marcaF);
        producto.setModelo(modeloF);
        producto.setCategoria(categoria);
        if (imagen != null) producto.setImagenUrl(imagen);
        producto.setActivo(true);
        producto = productoRepository.save(producto);

        Variante variante = (producto.getVariantes() != null && !producto.getVariantes().isEmpty())
                ? producto.getVariantes().get(0)
                : varianteRepository.findByProductoId(producto.getId()).stream().findFirst().orElse(null);
        if (variante == null) {
            variante = new Variante();
            variante.setProducto(producto);
        }
        variante.setEspecificaciones(especificaciones);
        variante.setCostoUsd(costoUsd);
        variante.setMonedaOrigen(esUsd ? "USD" : "ARS");
        variante.setPrecioOrigen(precioArs);
        variante.setStock(stock != null ? Math.max(stock, 0) : 0);
        variante.setActivo(true);
        varianteRepository.save(variante);

        return nuevo ? "creado" : "actualizado";
    }

    private boolean coincide(JsonNode art, String categoria, String marca) {
        if (categoria != null && !categoria.isBlank()) {
            String c = txt(art, "CATEGORY");
            if (c == null || !c.equalsIgnoreCase(categoria.trim())) return false;
        }
        if (marca != null && !marca.isBlank()) {
            String m = txt(art, "BRAND");
            if (m == null || !m.equalsIgnoreCase(marca.trim())) return false;
        }
        return true;
    }

    private Proveedor obtenerOcrearProveedor() {
        return proveedorRepository.findByNombreIgnoreCase(NOMBRE_PROVEEDOR)
                .map(p -> {
                    if (!Boolean.TRUE.equals(p.getActivo())) { p.setActivo(true); proveedorRepository.save(p); }
                    return p;
                })
                .orElseGet(() -> {
                    Proveedor p = new Proveedor();
                    p.setNombre(NOMBRE_PROVEEDOR);
                    p.setMargenPorcentaje(new BigDecimal("15"));   // editable desde Proveedores
                    p.setFletePorcentaje(BigDecimal.ZERO);
                    p.setActivo(true);
                    return proveedorRepository.save(p);
                });
    }

    private String modeloDesde(String marca, String titulo) {
        if (titulo == null) return null;
        String t = titulo.trim();
        if (marca != null && t.toLowerCase().startsWith(marca.toLowerCase())) {
            String resto = t.substring(marca.length()).trim();
            if (!resto.isEmpty()) return resto;
        }
        return t;
    }

    /** Parsea un precio que puede venir en formato US (1234.56) o AR (1.234,56). */
    private BigDecimal parsePrecio(String s) {
        if (s == null) return BigDecimal.ZERO;
        String t = s.trim().replaceAll("[^0-9.,]", "");
        if (t.isEmpty()) return BigDecimal.ZERO;
        boolean hasComma = t.contains(","), hasDot = t.contains(".");
        if (hasComma && hasDot) {
            if (t.lastIndexOf(',') > t.lastIndexOf('.')) {
                t = t.replace(".", "").replace(",", ".");   // AR: . miles, , decimal
            } else {
                t = t.replace(",", "");                      // US: , miles, . decimal
            }
        } else if (hasComma) {
            t = t.replace(",", ".");
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String txt(JsonNode node, String field) {
        String v = node.path(field).asText(null);
        return (v == null || v.isBlank() || "null".equalsIgnoreCase(v)) ? null : v.trim();
    }
}
