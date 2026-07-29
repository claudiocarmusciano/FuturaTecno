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

    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> listarCatalogo() {
        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
        List<ProductoCatalogoDTO> resultado = new ArrayList<>();
        for (Producto producto : productoRepository.findByActivo(true)) {
            resultado.add(toDTO(producto, cotizacion));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public ProductoCatalogoDTO obtenerProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        return toDTO(producto, cotizacionService.obtenerCotizacionUsdArs());
    }

    private ProductoCatalogoDTO toDTO(Producto producto, BigDecimal cotizacion) {
        Proveedor proveedor = producto.getProveedor();

        // La "última actualización" del artículo = la fecha más reciente entre el producto
        // y sus variantes (el precio se guarda en la variante, que se actualiza al pisarlo).
        java.time.LocalDateTime ultimaAct = producto.getUpdatedAt();

        List<VarianteCatalogoDTO> variantesDto = new ArrayList<>();
        for (Variante v : varianteRepository.findByProductoIdAndActivo(producto.getId(), true)) {
            if (v.getUpdatedAt() != null && (ultimaAct == null || v.getUpdatedAt().isAfter(ultimaAct))) {
                ultimaAct = v.getUpdatedAt();
            }
            BigDecimal precioVentaUsd = precioService.precioVentaUsd(v, proveedor);
            BigDecimal precioVentaArs = precioService.aArs(precioVentaUsd, cotizacion);

            variantesDto.add(new VarianteCatalogoDTO(
                    v.getId(), v.getEspecificaciones(), precioVentaUsd, precioVentaArs));
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
        dto.setImagenes(imagenesDe(producto));
        dto.setUltimaActualizacion(ultimaAct);
        return dto;
    }

    /** Imagen principal + galería (2ª/3ª) traída automáticamente por el importador (hoy solo Elit). */
    private List<String> imagenesDe(Producto producto) {
        List<String> urls = new ArrayList<>();
        if (producto.getImagenUrl() != null && !producto.getImagenUrl().isBlank()) {
            urls.add(producto.getImagenUrl());
        }
        for (Imagen img : imagenRepository.findByProductoIdAndActivoOrderByOrden(producto.getId(), true)) {
            urls.add(img.getUrl());
        }
        return urls;
    }

}
