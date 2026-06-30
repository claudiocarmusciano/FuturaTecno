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
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Importa el catálogo de Elit a FuturaTecno usando {@link ElitApiClient}.
 * Cada producto de Elit se mapea a un Producto + una Variante (1:1), deduplicando por el id de Elit
 * (guardado en codigo_externo). El costo se toma como precio + IVA; el margen y flete los pone el
 * proveedor "Elit" en FuturaTecno. Las imágenes vienen incluidas en la API.
 */
@Service
public class ElitImportService {
    private static final Logger logger = LoggerFactory.getLogger(ElitImportService.class);
    private static final String NOMBRE_PROVEEDOR = "Elit";
    private static final String FUENTE = "ELIT";
    private static final int PAGINA = 100;          // máximo permitido por la API
    private static final int TOPE_PAGINAS = 200;     // tope de seguridad (200 x 100 = 20.000 items)

    private final ElitApiClient elitApiClient;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;

    // Caché simple de filtros (categorías/marcas) para los desplegables del panel.
    private Map<String, Object> filtrosCache;
    private Instant filtrosCacheTs;

    public ElitImportService(ElitApiClient elitApiClient,
                             ProductoRepository productoRepository,
                             VarianteRepository varianteRepository,
                             ProveedorRepository proveedorRepository) {
        this.elitApiClient = elitApiClient;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.proveedorRepository = proveedorRepository;
    }

    public boolean estaConfigurado() {
        return elitApiClient.estaConfigurado();
    }

    /** Cuántos productos coinciden con el filtro (sin importar), más una muestra de nombres. */
    public Map<String, Object> previsualizar(String categoria, String marca, String store) {
        JsonNode resp = elitApiClient.consultarProductos(5, 0, categoria, marca, null, store);
        int total = resp.path("paginador").path("total").asInt(0);
        List<String> muestra = new ArrayList<>();
        for (JsonNode p : resp.path("resultado")) {
            muestra.add(p.path("nombre").asText(""));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("muestra", muestra);
        return out;
    }

    /** Lista de categorías y marcas disponibles (escanea el catálogo una vez y cachea 6 h). */
    public Map<String, Object> filtros() {
        if (filtrosCache != null && filtrosCacheTs != null
                && Duration.between(filtrosCacheTs, Instant.now()).toHours() < 6) {
            return filtrosCache;
        }
        TreeSet<String> categorias = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> marcas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        int offset = 0, total = Integer.MAX_VALUE, paginas = 0;
        while (offset < total && paginas < TOPE_PAGINAS) {
            JsonNode resp = elitApiClient.consultarProductos(PAGINA, offset, null, null, null, "all");
            total = resp.path("paginador").path("total").asInt(0);
            JsonNode arr = resp.path("resultado");
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode p : arr) {
                String c = p.path("categoria").asText("");
                String m = p.path("marca").asText("");
                if (!c.isBlank()) categorias.add(c);
                if (!m.isBlank()) marcas.add(m);
            }
            offset += PAGINA;
            paginas++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("categorias", new ArrayList<>(categorias));
        out.put("marcas", new ArrayList<>(marcas));
        filtrosCache = out;
        filtrosCacheTs = Instant.now();
        return out;
    }

    @Transactional
    public Map<String, Object> importar(String categoria, String marca, boolean soloConStock, String store) {
        if (!estaConfigurado()) {
            throw new IllegalStateException("La API de Elit no está configurada (faltan ELIT_USER_ID / ELIT_TOKEN).");
        }
        Proveedor proveedor = obtenerOcrearProveedor();

        int offset = 0, total = Integer.MAX_VALUE, paginas = 0;
        int creados = 0, actualizados = 0, salteadosSinStock = 0;

        while (offset < total && paginas < TOPE_PAGINAS) {
            JsonNode resp = elitApiClient.consultarProductos(PAGINA, offset, categoria, marca, null, store);
            total = resp.path("paginador").path("total").asInt(0);
            JsonNode arr = resp.path("resultado");
            if (!arr.isArray() || arr.isEmpty()) break;

            for (JsonNode prod : arr) {
                int stock = prod.path("stock_total").asInt(0);
                if (soloConStock && stock <= 0) {
                    salteadosSinStock++;
                    continue;
                }
                boolean nuevo = upsertProducto(proveedor, prod, stock);
                if (nuevo) creados++; else actualizados++;
            }
            offset += PAGINA;
            paginas++;
        }

        String mensaje = String.format(
                "Importación de Elit completada: %d nuevos, %d actualizados, %d sin stock salteados (de %d que coincidían con el filtro).",
                creados, actualizados, salteadosSinStock, total == Integer.MAX_VALUE ? 0 : total);
        logger.info(mensaje);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("creados", creados);
        out.put("actualizados", actualizados);
        out.put("salteadosSinStock", salteadosSinStock);
        out.put("mensaje", mensaje);
        return out;
    }

    /** Crea o actualiza el Producto + Variante a partir de un item de Elit. Devuelve true si es nuevo. */
    private boolean upsertProducto(Proveedor proveedor, JsonNode prod, int stock) {
        String codigoExterno = prod.path("id").asText(null);
        String marca = txt(prod, "marca");
        String nombre = txt(prod, "nombre");
        String modelo = modeloDesde(marca, nombre);

        // Costo = precio (USD neto) + IVA. El margen/flete los aplica el proveedor en FuturaTecno.
        BigDecimal precio = dec(prod, "precio");
        BigDecimal iva = dec(prod, "iva");
        BigDecimal costoUsd = precio
                .multiply(BigDecimal.ONE.add(iva.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        String categoria = txt(prod, "categoria");
        String imagen = primeraImagen(prod);
        String especificaciones = txt(prod, "descripcion");
        if (especificaciones == null) especificaciones = "";

        final String marcaF = marca != null ? marca : "—";
        final String modeloF = modelo != null ? modelo : (nombre != null ? nombre : "Producto " + codigoExterno);

        boolean[] esNuevo = {false};
        Producto producto = productoRepository
                .findByProveedorIdAndCodigoExterno(proveedor.getId(), codigoExterno)
                .orElseGet(() -> {
                    esNuevo[0] = true;
                    Producto p = new Producto();
                    p.setProveedor(proveedor);
                    p.setCodigoExterno(codigoExterno);
                    p.setFuente(FUENTE);
                    p.setActivo(true);
                    return p;
                });
        producto.setMarca(marcaF);
        producto.setModelo(modeloF);
        producto.setCategoria(categoria);
        if (imagen != null) producto.setImagenUrl(imagen);
        producto.setActivo(true);
        producto = productoRepository.save(producto);

        // Una sola variante por producto importado de Elit.
        Variante variante = (producto.getVariantes() != null && !producto.getVariantes().isEmpty())
                ? producto.getVariantes().get(0)
                : varianteRepository.findByProductoId(producto.getId()).stream().findFirst().orElse(null);
        if (variante == null) {
            variante = new Variante();
            variante.setProducto(producto);
        }
        variante.setEspecificaciones(especificaciones);
        variante.setCostoUsd(costoUsd);
        variante.setMonedaOrigen("USD");
        variante.setPrecioOrigen(precio);
        variante.setStock(Math.max(stock, 0));
        variante.setActivo(true);
        varianteRepository.save(variante);

        return esNuevo[0];
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

    /** Quita la marca del comienzo del nombre para no duplicarla (marca se muestra aparte). */
    private String modeloDesde(String marca, String nombre) {
        if (nombre == null) return null;
        String n = nombre.trim();
        if (marca != null && n.toLowerCase().startsWith(marca.toLowerCase())) {
            String resto = n.substring(marca.length()).trim();
            if (!resto.isEmpty()) return resto;
        }
        return n;
    }

    private String primeraImagen(JsonNode prod) {
        JsonNode imgs = prod.path("imagenes");
        if (imgs.isArray() && !imgs.isEmpty()) {
            String u = imgs.get(0).asText("");
            if (!u.isBlank()) return u;
        }
        return null;
    }

    private String txt(JsonNode node, String field) {
        String v = node.path(field).asText(null);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private BigDecimal dec(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isNumber() ? n.decimalValue() : BigDecimal.ZERO;
    }
}
