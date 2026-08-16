package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.futuratecno.api.dto.EnvioCotizacionDTO;
import com.futuratecno.api.dto.ItemPedidoRequest;
import com.futuratecno.api.dto.ProductoSinEnvioDTO;
import com.futuratecno.domain.Categoria;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.CategoriaRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cotización de envío por Andreani para un carrito.
 *
 * <p>El peso y las dimensiones de cada producto se resuelven campo por campo: primero el override
 * del producto (Admin → Productos), después el default de su subcategoría y por último el de la
 * categoría padre (V14). Si algún campo queda sin resolver, no se cotiza — Andreani exige los
 * cuatro datos de cada bulto — y se responde "no disponible" sin romper el checkout.
 */
@Service
public class EnvioService {
    private static final Logger logger = LoggerFactory.getLogger(EnvioService.class);

    private final AndreaniClient andreaniClient;
    private final VarianteRepository varianteRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final PrecioService precioService;
    private final CotizacionService cotizacionService;

    public EnvioService(AndreaniClient andreaniClient,
                        VarianteRepository varianteRepository,
                        CategoriaRepository categoriaRepository,
                        ProductoRepository productoRepository,
                        PrecioService precioService,
                        CotizacionService cotizacionService) {
        this.andreaniClient = andreaniClient;
        this.varianteRepository = varianteRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.precioService = precioService;
        this.cotizacionService = cotizacionService;
    }

    @Transactional(readOnly = true)
    public EnvioCotizacionDTO cotizar(String cpDestino, List<ItemPedidoRequest> items) {
        if (!andreaniClient.estaConfigurado()) {
            return EnvioCotizacionDTO.noDisponible("La cotización de envío no está disponible.");
        }
        String cp = normalizarCp(cpDestino);
        if (cp == null) {
            throw new IllegalArgumentException("Ingresá un código postal válido (4 dígitos).");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No hay artículos para cotizar.");
        }

        BigDecimal cotizacionUsdArs = cotizacionService.obtenerCotizacionUsdArs();
        List<Map<String, Object>> products = new ArrayList<>();
        Map<Long, Categoria> cacheCategorias = new HashMap<>();

        for (ItemPedidoRequest item : items) {
            if (item.getVarianteId() == null || item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("Hay un artículo inválido en el carrito.");
            }
            Variante variante = varianteRepository.findById(item.getVarianteId())
                    .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Un artículo del carrito ya no está disponible."));
            Producto producto = variante.getProducto();
            if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
                throw new IllegalArgumentException("Un artículo del carrito ya no está disponible.");
            }

            Medidas medidas = resolverMedidas(producto, cacheCategorias);
            if (medidas == null) {
                logger.info("Envío: producto {} sin peso/dimensiones resolubles, no se cotiza.",
                        producto.getId());
                return EnvioCotizacionDTO.noDisponible(
                        "Todavía no podemos calcular el envío de este pedido. Lo coordinamos al confirmar.");
            }

            // El precio (en ARS, entero) lo usa Andreani para el seguro del envío.
            BigDecimal precioUsd = precioService.precioVentaUsd(variante, producto.getProveedor());
            long precioArs = precioService.aArs(precioUsd, cotizacionUsdArs)
                    .setScale(0, RoundingMode.HALF_UP).longValue();

            products.add(Map.of(
                    "quantity", item.getCantidad(),
                    "price", precioArs,
                    "dimensions", Map.of(
                            "width", medidas.anchoCm,
                            "height", medidas.altoCm,
                            "depth", medidas.largoCm,
                            "grams", medidas.pesoGramos)));
        }

        JsonNode rates;
        try {
            rates = andreaniClient.cotizar(cp, products);
        } catch (Exception e) {
            // La cotización nunca rompe el checkout: si Andreani está caído, se coordina a mano.
            logger.warn("Envío: fallo cotizando contra Andreani: {}", e.getMessage());
            return EnvioCotizacionDTO.noDisponible(
                    "No pudimos cotizar el envío en este momento. Lo coordinamos al confirmar.");
        }

        if (rates == null || !rates.isArray() || rates.isEmpty()) {
            return EnvioCotizacionDTO.noDisponible(
                    "No hay tarifas de envío para ese código postal. Lo coordinamos al confirmar.");
        }

        // Sucursal viene repetida (una entrada por punto de retiro del CP): nos quedamos con el
        // mínimo por modalidad. El orden de aparición de Andreani se respeta.
        Map<String, BigDecimal> porModalidad = new LinkedHashMap<>();
        for (JsonNode rate : rates) {
            String codigo = rate.path("code").asText("");
            BigDecimal total = new BigDecimal(rate.path("total").asText("0"));
            if (codigo.isBlank() || total.signum() <= 0) continue;
            porModalidad.merge(codigo, total, BigDecimal::min);
        }
        if (porModalidad.isEmpty()) {
            return EnvioCotizacionDTO.noDisponible(
                    "No hay tarifas de envío para ese código postal. Lo coordinamos al confirmar.");
        }

        EnvioCotizacionDTO dto = new EnvioCotizacionDTO();
        dto.setDisponible(true);
        porModalidad.forEach((codigo, total) ->
                dto.getOpciones().add(new EnvioCotizacionDTO.OpcionEnvio(codigo, total)));
        return dto;
    }

    /**
     * Costo congelable de una modalidad puntual, para guardarlo en el pedido al confirmar. Se
     * recotiza server-side (nunca se confía en el importe del cliente, mismo criterio que el
     * precio). Devuelve null si en este momento no se puede cotizar: el pedido igual se guarda
     * con la modalidad elegida y el costo queda "a cotizar".
     */
    @Transactional(readOnly = true)
    public BigDecimal costoDeModalidad(String cpDestino, String modalidad, List<ItemPedidoRequest> items) {
        try {
            EnvioCotizacionDTO cotizacion = cotizar(cpDestino, items);
            if (!cotizacion.isDisponible()) return null;
            return cotizacion.getOpciones().stream()
                    .filter(o -> o.getCodigo().equalsIgnoreCase(modalidad))
                    .map(EnvioCotizacionDTO.OpcionEnvio::getTotalArs)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            logger.warn("Envío: no se pudo congelar el costo ({}).", e.getMessage());
            return null;
        }
    }

    /** Auditoría administrativa: productos activos que hoy caerían en "a coordinar". */
    @Transactional(readOnly = true)
    public List<ProductoSinEnvioDTO> productosSinMedidasCotizables() {
        Map<Long, Categoria> cacheCategorias = new HashMap<>();
        List<ProductoSinEnvioDTO> resultado = new ArrayList<>();
        for (Producto producto : productoRepository.findByActivo(true)) {
            List<String> faltantes = faltantes(producto, cacheCategorias);
            if (faltantes.isEmpty()) continue;
            Categoria categoria = producto.getCategoriaId() == null ? null : cacheCategorias.computeIfAbsent(
                    producto.getCategoriaId(), id -> categoriaRepository.findById(id).orElse(null));
            resultado.add(new ProductoSinEnvioDTO(producto.getId(), nombreProducto(producto),
                    categoria == null ? "Sin categoría" : pathCategoria(categoria), producto.getCategoriaId(),
                    String.join(", ", faltantes)));
        }
        return resultado;
    }

    /**
     * Peso y dimensiones efectivos de un producto: override propio, si no default de la
     * subcategoría, si no de la categoría padre. Campo por campo. Null si algo queda sin dato.
     */
    private Medidas resolverMedidas(Producto producto, Map<Long, Categoria> cache) {
        Categoria hoja = null;
        Categoria padre = null;
        if (producto.getCategoriaId() != null) {
            hoja = cache.computeIfAbsent(producto.getCategoriaId(),
                    id -> categoriaRepository.findById(id).orElse(null));
            padre = hoja != null ? hoja.getPadre() : null;
        }

        Integer peso = primero(producto.getPesoGramos(),
                hoja != null ? hoja.getPesoGramosDefault() : null,
                padre != null ? padre.getPesoGramosDefault() : null);
        Integer alto = primero(producto.getAltoCm(),
                hoja != null ? hoja.getAltoCmDefault() : null,
                padre != null ? padre.getAltoCmDefault() : null);
        Integer ancho = primero(producto.getAnchoCm(),
                hoja != null ? hoja.getAnchoCmDefault() : null,
                padre != null ? padre.getAnchoCmDefault() : null);
        Integer largo = primero(producto.getLargoCm(),
                hoja != null ? hoja.getLargoCmDefault() : null,
                padre != null ? padre.getLargoCmDefault() : null);

        if (peso == null || alto == null || ancho == null || largo == null
                || peso <= 0 || alto <= 0 || ancho <= 0 || largo <= 0) {
            return null;
        }
        return new Medidas(peso, alto, ancho, largo);
    }

    private List<String> faltantes(Producto producto, Map<Long, Categoria> cache) {
        Categoria hoja = null;
        Categoria padre = null;
        if (producto.getCategoriaId() != null) {
            hoja = cache.computeIfAbsent(producto.getCategoriaId(), id -> categoriaRepository.findById(id).orElse(null));
            padre = hoja != null ? hoja.getPadre() : null;
        }
        List<String> faltantes = new ArrayList<>();
        if (!valido(primero(producto.getPesoGramos(), hoja != null ? hoja.getPesoGramosDefault() : null,
                padre != null ? padre.getPesoGramosDefault() : null))) faltantes.add("peso");
        if (!valido(primero(producto.getAltoCm(), hoja != null ? hoja.getAltoCmDefault() : null,
                padre != null ? padre.getAltoCmDefault() : null))) faltantes.add("alto");
        if (!valido(primero(producto.getAnchoCm(), hoja != null ? hoja.getAnchoCmDefault() : null,
                padre != null ? padre.getAnchoCmDefault() : null))) faltantes.add("ancho");
        if (!valido(primero(producto.getLargoCm(), hoja != null ? hoja.getLargoCmDefault() : null,
                padre != null ? padre.getLargoCmDefault() : null))) faltantes.add("largo");
        return faltantes;
    }

    private boolean valido(Integer valor) { return valor != null && valor > 0; }

    private String nombreProducto(Producto producto) {
        return (producto.getMarca() + " " + producto.getModelo()).trim();
    }

    private String pathCategoria(Categoria categoria) {
        return categoria.getPadre() == null ? categoria.getNombre() : categoria.getPadre().getNombre() + " > " + categoria.getNombre();
    }

    private Integer primero(Integer... valores) {
        for (Integer v : valores) {
            if (v != null) return v;
        }
        return null;
    }

    /** Deja solo dígitos: "B7400ABC" → "7400". Null si no quedan exactamente 4. */
    private String normalizarCp(String cp) {
        if (cp == null) return null;
        String digitos = cp.replaceAll("\\D", "");
        return digitos.length() == 4 ? digitos : null;
    }

    private record Medidas(int pesoGramos, int altoCm, int anchoCm, int largoCm) {}
}
