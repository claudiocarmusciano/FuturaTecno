package com.futuratecno.application;

import com.futuratecno.api.dto.ImportacionResponse;
import com.futuratecno.api.dto.ProductoImportar;
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
import java.util.List;

@Service
public class ImportacionService {
    private static final Logger logger = LoggerFactory.getLogger(ImportacionService.class);

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final CotizacionService cotizacionService;

    public ImportacionService(ProductoRepository productoRepository,
                              VarianteRepository varianteRepository,
                              ProveedorRepository proveedorRepository,
                              CotizacionService cotizacionService) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.proveedorRepository = proveedorRepository;
        this.cotizacionService = cotizacionService;
    }

    @Transactional
    public ImportacionResponse importar(Long proveedorId, List<ProductoImportar> productos) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + proveedorId));

        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
        int creados = 0, actualizados = 0, omitidos = 0;

        for (ProductoImportar item : productos) {
            String marca = limpiar(item.getMarca());
            String modelo = limpiar(item.getModelo());

            // Sin marca/modelo o sin precio válido => se omite.
            if (marca == null || modelo == null || item.getPrecio() == null) {
                omitidos++;
                continue;
            }

            String moneda = "USD".equalsIgnoreCase(item.getMoneda()) ? "USD" : "ARS";
            BigDecimal precioOrigen = item.getPrecio();
            BigDecimal costoUsd = "USD".equals(moneda)
                    ? precioOrigen.setScale(2, RoundingMode.HALF_UP)
                    : precioOrigen.divide(cotizacion, 2, RoundingMode.HALF_UP);

            String especificaciones = item.getEspecificaciones() != null
                    ? item.getEspecificaciones().trim() : "";

            // Buscar el producto del proveedor por marca + modelo (o crearlo).
            Producto producto = productoRepository
                    .findByProveedorIdAndMarcaAndModelo(proveedorId, marca, modelo)
                    .orElseGet(() -> {
                        Producto nuevo = new Producto();
                        nuevo.setProveedor(proveedor);
                        nuevo.setMarca(marca);
                        nuevo.setModelo(modelo);
                        nuevo.setActivo(true);
                        return nuevo;
                    });
            producto.setCategoria(limpiar(item.getCategoria()));
            producto = productoRepository.save(producto);

            // Buscar variante por especificaciones dentro del producto (o crearla).
            Variante variante = varianteRepository
                    .findByProductoIdAndEspecificaciones(producto.getId(), especificaciones)
                    .orElse(null);

            if (variante != null) {
                variante.setCostoUsd(costoUsd);
                variante.setMonedaOrigen(moneda);
                variante.setPrecioOrigen(precioOrigen);
                variante.setActivo(true);
                varianteRepository.save(variante);
                actualizados++;
            } else {
                Variante nueva = new Variante();
                nueva.setProducto(producto);
                nueva.setEspecificaciones(especificaciones);
                nueva.setCostoUsd(costoUsd);
                nueva.setMonedaOrigen(moneda);
                nueva.setPrecioOrigen(precioOrigen);
                nueva.setStock(0);
                nueva.setActivo(true);
                varianteRepository.save(nueva);
                creados++;
            }
        }

        String mensaje = String.format(
                "Importación completada: %d creados, %d actualizados, %d omitidos (cotización dólar oficial: $%s).",
                creados, actualizados, omitidos, cotizacion);
        logger.info(mensaje);
        return new ImportacionResponse(creados, actualizados, omitidos, mensaje);
    }

    private String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
