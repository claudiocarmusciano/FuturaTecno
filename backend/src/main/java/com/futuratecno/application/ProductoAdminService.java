package com.futuratecno.application;

import com.futuratecno.api.dto.BuscarImagenesResponse;
import com.futuratecno.api.dto.ProductoAdminDTO;
import com.futuratecno.api.dto.ProductoEditDTO;
import com.futuratecno.api.dto.VarianteEditDTO;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductoAdminService {
    private static final Logger logger = LoggerFactory.getLogger(ProductoAdminService.class);

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final IcecatService icecatService;
    private final AnthropicImageService anthropicImageService;
    private final CotizacionService cotizacionService;

    public ProductoAdminService(ProductoRepository productoRepository,
                                VarianteRepository varianteRepository,
                                IcecatService icecatService,
                                AnthropicImageService anthropicImageService,
                                CotizacionService cotizacionService) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.icecatService = icecatService;
        this.anthropicImageService = anthropicImageService;
        this.cotizacionService = cotizacionService;
    }

    @Transactional(readOnly = true)
    public List<ProductoAdminDTO> listar() {
        return productoRepository.findByActivo(true).stream()
                .map(p -> {
                    ProductoAdminDTO dto = new ProductoAdminDTO(
                            p.getId(),
                            p.getCategoria(),
                            p.getMarca(),
                            p.getModelo(),
                            p.getProveedor() != null ? p.getProveedor().getNombre() : null,
                            p.getImagenUrl());
                    dto.setUltimaActualizacion(calcularUltimaActualizacion(p));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /** Fecha más reciente entre el producto y sus variantes activas (refleja la última actualización de precio). */
    private LocalDateTime calcularUltimaActualizacion(Producto p) {
        LocalDateTime ultima = p.getUpdatedAt();
        for (Variante v : varianteRepository.findByProductoIdAndActivo(p.getId(), true)) {
            if (v.getUpdatedAt() != null && (ultima == null || v.getUpdatedAt().isAfter(ultima))) {
                ultima = v.getUpdatedAt();
            }
        }
        return ultima;
    }

    @Transactional
    public void eliminar(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    /** Devuelve un producto con sus variantes en formato editable (precio en moneda de origen). */
    @Transactional(readOnly = true)
    public ProductoEditDTO obtenerParaEditar(Long productoId) {
        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

        ProductoEditDTO dto = new ProductoEditDTO();
        dto.setId(p.getId());
        dto.setCategoria(p.getCategoria());
        dto.setMarca(p.getMarca());
        dto.setModelo(p.getModelo());
        dto.setProveedor(p.getProveedor() != null ? p.getProveedor().getNombre() : null);
        dto.setImagenUrl(p.getImagenUrl());
        if (p.getProveedor() != null) {
            dto.setMargenPorcentaje(p.getProveedor().getMargenPorcentaje());
            dto.setFletePorcentaje(p.getProveedor().getFletePorcentaje());
        }
        dto.setCotizacion(cotizacionService.obtenerCotizacionUsdArs());

        List<VarianteEditDTO> variantes = new ArrayList<>();
        for (Variante v : varianteRepository.findByProductoIdAndActivo(p.getId(), true)) {
            String moneda = "USD".equals(v.getMonedaOrigen()) ? "USD" : (v.getMonedaOrigen() != null ? v.getMonedaOrigen() : "USD");
            BigDecimal precio = v.getPrecioOrigen() != null ? v.getPrecioOrigen() : v.getCostoUsd();
            variantes.add(new VarianteEditDTO(v.getId(), v.getEspecificaciones(), moneda, precio, v.getStock()));
        }
        dto.setVariantes(variantes);
        return dto;
    }

    /** Actualiza los datos del producto y de cada variante. Recalcula el costo USD según la moneda. */
    @Transactional
    public ProductoEditDTO actualizarProducto(Long productoId, ProductoEditDTO dto) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

        producto.setCategoria(vacioANull(dto.getCategoria()));
        if (dto.getMarca() != null && !dto.getMarca().isBlank()) producto.setMarca(dto.getMarca().trim());
        if (dto.getModelo() != null && !dto.getModelo().isBlank()) producto.setModelo(dto.getModelo().trim());
        productoRepository.save(producto);

        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();

        if (dto.getVariantes() != null) {
            for (VarianteEditDTO ve : dto.getVariantes()) {
                if (ve.getId() == null) continue;
                Variante v = varianteRepository.findById(ve.getId()).orElse(null);
                if (v == null || v.getProducto() == null || !v.getProducto().getId().equals(productoId)) continue;

                v.setEspecificaciones(ve.getEspecificaciones() != null ? ve.getEspecificaciones().trim() : "");
                if (ve.getPrecio() != null) {
                    String moneda = "USD".equalsIgnoreCase(ve.getMoneda()) ? "USD" : "ARS";
                    v.setMonedaOrigen(moneda);
                    v.setPrecioOrigen(ve.getPrecio());
                    v.setCostoUsd("USD".equals(moneda)
                            ? ve.getPrecio().setScale(2, RoundingMode.HALF_UP)
                            : ve.getPrecio().divide(cotizacion, 2, RoundingMode.HALF_UP));
                }
                if (ve.getStock() != null) v.setStock(ve.getStock());
                varianteRepository.save(v);
            }
        }

        return obtenerParaEditar(productoId);
    }

    private String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @Transactional
    public ProductoAdminDTO actualizarImagen(Long productoId, String url) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        producto.setImagenUrl(url != null && !url.isBlank() ? url.trim() : null);
        productoRepository.save(producto);
        return new ProductoAdminDTO(
                producto.getId(), producto.getCategoria(), producto.getMarca(),
                producto.getModelo(),
                producto.getProveedor() != null ? producto.getProveedor().getNombre() : null,
                producto.getImagenUrl());
    }

    /**
     * Cascada de búsqueda de imagen para cada producto sin imagen:
     *   1) Icecat (por marca + código, si está configurado) — gratis, matchea pocos.
     *   2) Anthropic web search → og:image (busca la página del producto y extrae su imagen).
     * Lo que no se encuentre queda para carga manual.
     */
    @Transactional
    public BuscarImagenesResponse buscarImagenesFaltantes() {
        boolean icecatOk = icecatService.estaConfigurado();
        boolean anthropicOk = anthropicImageService.estaConfigurado();

        List<Producto> sinImagen = productoRepository.findByActivo(true).stream()
                .filter(p -> p.getImagenUrl() == null || p.getImagenUrl().isBlank())
                .collect(Collectors.toList());

        int desdeIcecat = 0, desdeAnthropic = 0;
        for (Producto p : sinImagen) {
            String url = null;

            // Especificaciones de la primera variante (CPU/RAM/SSD, color, capacidad, etc.):
            // ayudan a encontrar la publicación EXACTA en MercadoLibre. Se limpian los separadores.
            String especificaciones = "";
            if (p.getVariantes() != null && !p.getVariantes().isEmpty()) {
                String esp = p.getVariantes().get(0).getEspecificaciones();
                if (esp != null) {
                    especificaciones = esp.replace("/", " ").replaceAll("\\s+", " ").trim();
                }
            }

            String consulta = String.join(" ",
                    p.getCategoria() != null ? p.getCategoria() : "",
                    p.getMarca() != null ? p.getMarca() : "",
                    p.getModelo() != null ? p.getModelo() : "",
                    especificaciones).replaceAll("\\s+", " ").trim();

            // 1) Icecat (rápido y gratis; rara vez matchea esta clase de productos)
            if (icecatOk) {
                try {
                    Optional<String> r = icecatService.buscarImagen(p.getMarca(), p.getModelo());
                    if (r.isPresent()) { url = r.get(); desdeIcecat++; }
                } catch (Exception e) {
                    logger.warn("Icecat falló para producto {}: {}", p.getId(), e.getMessage());
                }
            }

            // 2) Anthropic (búsqueda web + og:image) — la fuente principal
            if (url == null && anthropicOk) {
                try {
                    Optional<String> r = anthropicImageService.buscarImagen(consulta);
                    if (r.isPresent()) { url = r.get(); desdeAnthropic++; }
                } catch (Exception e) {
                    logger.warn("Anthropic falló para producto {}: {}", p.getId(), e.getMessage());
                }
            }

            if (url != null) {
                p.setImagenUrl(url);
                productoRepository.save(p);
            }
        }

        int encontradas = desdeIcecat + desdeAnthropic;
        int noEncontradas = sinImagen.size() - encontradas;
        String mensaje = String.format(
                "Búsqueda completada: %d con imagen (%d Icecat, %d Anthropic), %d sin resultado (de %d productos).",
                encontradas, desdeIcecat, desdeAnthropic, noEncontradas, sinImagen.size());
        logger.info(mensaje);

        BuscarImagenesResponse resp = new BuscarImagenesResponse(sinImagen.size(), encontradas, noEncontradas, mensaje);
        resp.setDesdeIcecat(desdeIcecat);
        resp.setDesdeAnthropic(desdeAnthropic);
        return resp;
    }
}
