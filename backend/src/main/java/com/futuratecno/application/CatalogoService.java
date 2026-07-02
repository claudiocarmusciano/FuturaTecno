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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ImagenRepository imagenRepository;
    private final CotizacionService cotizacionService;

    public CatalogoService(ProductoRepository productoRepository,
                           VarianteRepository varianteRepository,
                           ImagenRepository imagenRepository,
                           CotizacionService cotizacionService) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.imagenRepository = imagenRepository;
        this.cotizacionService = cotizacionService;
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
        BigDecimal fletePct = proveedor.getFletePorcentaje() != null ? proveedor.getFletePorcentaje() : BigDecimal.ZERO;
        BigDecimal margen = proveedor.getMargenPorcentaje() != null ? proveedor.getMargenPorcentaje() : BigDecimal.ZERO;
        BigDecimal factorFlete = BigDecimal.ONE.add(fletePct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal factorMargen = BigDecimal.ONE.add(margen.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));

        // La "última actualización" del artículo = la fecha más reciente entre el producto
        // y sus variantes (el precio se guarda en la variante, que se actualiza al pisarlo).
        java.time.LocalDateTime ultimaAct = producto.getUpdatedAt();

        List<VarianteCatalogoDTO> variantesDto = new ArrayList<>();
        for (Variante v : varianteRepository.findByProductoIdAndActivo(producto.getId(), true)) {
            if (v.getUpdatedAt() != null && (ultimaAct == null || v.getUpdatedAt().isAfter(ultimaAct))) {
                ultimaAct = v.getUpdatedAt();
            }
            // Precio de venta = costo USD * (1 + flete%) * (1 + margen%)
            BigDecimal precioVentaUsd = v.getCostoUsd()
                    .multiply(factorFlete)
                    .multiply(factorMargen)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioVentaArs = precioVentaUsd.multiply(cotizacion)
                    .setScale(2, RoundingMode.HALF_UP);

            variantesDto.add(new VarianteCatalogoDTO(
                    v.getId(), v.getEspecificaciones(), precioVentaUsd, precioVentaArs));
        }

        ProductoCatalogoDTO dto = new ProductoCatalogoDTO(
                producto.getId(), producto.getCategoria(), producto.getMarca(),
                producto.getModelo(), producto.getImagenUrl(), variantesDto);
        dto.setSku(sku(producto));
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

    /**
     * SKU camuflado: código corto del proveedor + identificador del artículo. No delata el proveedor
     * (se ve como un código de referencia interno), pero permite cruzarlo con el sistema del mayorista.
     */
    private String sku(Producto producto) {
        Proveedor proveedor = producto.getProveedor();
        String prefijo = (proveedor != null && proveedor.getCodigo() != null && !proveedor.getCodigo().isBlank())
                ? proveedor.getCodigo() : "FT";
        String sufijo = (producto.getCodigoExterno() != null && !producto.getCodigoExterno().isBlank())
                ? producto.getCodigoExterno() : "P" + producto.getId();
        return prefijo + "-" + sufijo;
    }
}
