package com.futuratecno.application;

import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;
import com.futuratecno.domain.Imagen;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ImagenRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogoService {
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ImagenRepository imagenRepository;
    private final CotizacionService cotizacionService;
    private final CategoriaService categoriaService;
    private final PrecioService precioService;

    public CatalogoService(ProductoRepository productoRepository,
                           VarianteRepository varianteRepository,
                           ImagenRepository imagenRepository,
                           CotizacionService cotizacionService,
                           CategoriaService categoriaService,
                           PrecioService precioService) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.imagenRepository = imagenRepository;
        this.cotizacionService = cotizacionService;
        this.categoriaService = categoriaService;
        this.precioService = precioService;
    }

    /**
     * Trae TODO el catálogo activo de una. Con miles de productos, hacer una query de variantes
     * y otra de imágenes POR PRODUCTO volvía esto en miles de queries y varios segundos de
     * respuesta — acá se traen las variantes y las imágenes de todo el catálogo en dos consultas
     * (con IN), agrupadas por producto en memoria, en vez de una vez por producto.
     */
    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> listarCatalogo() {
        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
        List<Producto> productos = productoRepository.findByActivo(true);
        if (productos.isEmpty()) return List.of();

        List<Long> ids = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, List<Variante>> variantesPorProducto = varianteRepository
                .findByProductoIdInAndActivo(ids, true).stream()
                .collect(Collectors.groupingBy(v -> v.getProducto().getId()));
        Map<Long, List<Imagen>> imagenesPorProducto = imagenRepository
                .findByProductoIdInAndActivoOrderByOrden(ids, true).stream()
                .collect(Collectors.groupingBy(img -> img.getProducto().getId()));

        List<ProductoCatalogoDTO> resultado = new ArrayList<>();
        for (Producto producto : productos) {
            ProductoCatalogoDTO dto = toDTO(producto, cotizacion,
                    variantesPorProducto.getOrDefault(producto.getId(), List.of()),
                    imagenesPorProducto.getOrDefault(producto.getId(), List.of()));
            if (!dto.getVariantes().isEmpty()) resultado.add(dto);
        }
        return resultado;
    }

    /** Un solo producto: acá sí alcanza con las consultas puntuales, no hay N que multiplicar. */
    @Transactional(readOnly = true)
    public ProductoCatalogoDTO obtenerProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        List<Variante> variantes = varianteRepository.findByProductoIdAndActivo(id, true);
        List<Imagen> imagenes = imagenRepository.findByProductoIdAndActivoOrderByOrden(id, true);
        ProductoCatalogoDTO dto = toDTO(producto, cotizacionService.obtenerCotizacionUsdArs(), variantes, imagenes);
        if (dto.getVariantes().isEmpty()) {
            throw new IllegalArgumentException("Producto no disponible en catálogo");
        }
        return dto;
    }

    private ProductoCatalogoDTO toDTO(Producto producto, BigDecimal cotizacion,
                                      List<Variante> variantes, List<Imagen> imagenes) {
        Proveedor proveedor = producto.getProveedor();

        // La "última actualización" del artículo = la fecha más reciente entre el producto
        // y sus variantes (el precio se guarda en la variante, que se actualiza al pisarlo).
        java.time.LocalDateTime ultimaAct = producto.getUpdatedAt();

        List<VarianteCatalogoDTO> variantesDto = new ArrayList<>();
        for (Variante v : variantes) {
            if (v.getUpdatedAt() != null && (ultimaAct == null || v.getUpdatedAt().isAfter(ultimaAct))) {
                ultimaAct = v.getUpdatedAt();
            }
            BigDecimal precioVentaUsd = precioService.precioVentaUsd(v, proveedor);
            BigDecimal precioVentaArs = precioService.aArs(precioVentaUsd, cotizacion);

            variantesDto.add(new VarianteCatalogoDTO(
                    v.getId(), DescripcionProductoSanitizer.limpiar(v.getEspecificaciones()),
                    precioVentaUsd, precioVentaArs));
        }

        var nombresCategoria = categoriaService.resolverNombres(producto.getCategoriaId());
        ProductoCatalogoDTO dto = new ProductoCatalogoDTO(
                producto.getId(), nombresCategoria != null ? nombresCategoria.getSubcategoria() : null,
                producto.getMarca(), producto.getModelo(), producto.getImagenUrl(), variantesDto);
        dto.setCategoriaId(producto.getCategoriaId());
        if (nombresCategoria != null) {
            dto.setSeccion(nombresCategoria.getSeccion());
            dto.setCategoriaPadre(nombresCategoria.getCategoriaPadre());
        }
        dto.setSku(producto.skuCamuflado());
        dto.setImagenes(imagenesDe(producto, imagenes));
        dto.setUltimaActualizacion(ultimaAct);
        return dto;
    }

    /** Imagen principal + galería (2ª/3ª) traída automáticamente por el importador (hoy solo Elit). */
    private List<String> imagenesDe(Producto producto, List<Imagen> imagenes) {
        List<String> urls = new ArrayList<>();
        if (producto.getImagenUrl() != null && !producto.getImagenUrl().isBlank()) {
            urls.add(producto.getImagenUrl());
        }
        for (Imagen img : imagenes) {
            urls.add(img.getUrl());
        }
        return urls;
    }

}
