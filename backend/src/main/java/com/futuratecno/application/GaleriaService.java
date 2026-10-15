package com.futuratecno.application;

import com.futuratecno.domain.Imagen;
import com.futuratecno.domain.Producto;
import com.futuratecno.infrastructure.ImagenRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Galería de fotos de un producto en Admin → Imágenes (2026-10-15). Elit e Invid mandan una sola
 * foto por producto, así que las fotos extra se cargan a mano. Quedan marcadas `manual` (V46) para
 * que la sync de Elit y la carga JSON, que reemplazan la galería en cada corrida, no las borren.
 */
@Service
public class GaleriaService {
    /** Tope de fotos manuales por producto: la ficha muestra miniaturas en una sola fila. */
    static final int MAX_MANUALES = 4;
    /** Las manuales van después de las del mayorista (que usan 0, 1, 2…). */
    private static final int ORDEN_BASE_MANUAL = 100;

    private final ProductoRepository productoRepository;
    private final ImagenRepository imagenRepository;
    private final ImageUrlValidatorService validador;

    public GaleriaService(ProductoRepository productoRepository, ImagenRepository imagenRepository,
                          ImageUrlValidatorService validador) {
        this.productoRepository = productoRepository;
        this.imagenRepository = imagenRepository;
        this.validador = validador;
    }

    /** Todas las fotos del producto en el orden de la ficha: la principal y después la galería. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(Long productoId) {
        Producto producto = producto(productoId);
        List<Map<String, Object>> out = new ArrayList<>();
        if (producto.getImagenUrl() != null && !producto.getImagenUrl().isBlank()) {
            out.add(foto(null, producto.getImagenUrl(), "principal"));
        }
        for (Imagen img : imagenRepository.findByProductoIdAndActivoOrderByOrden(productoId, true)) {
            if (img.getUrl().equals(producto.getImagenUrl())) continue;   // la carga JSON repite la principal
            out.add(foto(img.getId(), img.getUrl(), Boolean.TRUE.equals(img.getManual()) ? "manual" : "mayorista"));
        }
        return out;
    }

    @Transactional
    public List<Map<String, Object>> agregar(Long productoId, String url) {
        Producto producto = producto(productoId);
        String limpia = url == null ? "" : url.trim();
        if (!limpia.matches("(?i)https?://\\S+")) {
            throw new IllegalArgumentException("Pegá la dirección completa de la foto (empieza con https://).");
        }
        List<Imagen> actuales = imagenRepository.findByProductoIdAndActivoOrderByOrden(productoId, true);
        if (limpia.equals(producto.getImagenUrl()) || actuales.stream().anyMatch(i -> i.getUrl().equals(limpia))) {
            throw new IllegalArgumentException("Esa foto ya está en el producto.");
        }
        long manuales = actuales.stream().filter(i -> Boolean.TRUE.equals(i.getManual())).count();
        if (manuales >= MAX_MANUALES) {
            throw new IllegalArgumentException("Ya tiene " + MAX_MANUALES + " fotos agregadas a mano. Quitá una para sumar otra.");
        }
        // Solo se rechaza lo que seguro no es una foto (404/410 o no es imagen). Un sitio que bloquea
        // servidores (403, timeout) queda DUDOSA y se acepta: en el navegador del cliente sí carga.
        if (validador.verificar(limpia) == ImageUrlValidatorService.Verificacion.MUERTA) {
            throw new IllegalArgumentException("Esa dirección no devuelve una imagen. Copiá la dirección de la imagen, no la de la página.");
        }
        Imagen img = new Imagen();
        img.setProducto(producto);
        img.setUrl(limpia);
        img.setOrden(ORDEN_BASE_MANUAL + (int) manuales);
        img.setActivo(true);
        img.setManual(true);
        imagenRepository.save(img);
        return listar(productoId);
    }

    /** Quita una foto de la galería. Solo las manuales: las del mayorista volverían en la próxima sync. */
    @Transactional
    public List<Map<String, Object>> quitar(Long productoId, Long imagenId) {
        Imagen img = imagenRepository.findById(imagenId)
                .filter(i -> i.getProducto().getId().equals(productoId))
                .orElseThrow(() -> new IllegalArgumentException("Esa foto no es de este producto."));
        if (!Boolean.TRUE.equals(img.getManual())) {
            throw new IllegalArgumentException("Esa foto la manda el mayorista: volvería en la próxima sincronización.");
        }
        imagenRepository.delete(img);
        return listar(productoId);
    }

    private Producto producto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    private static Map<String, Object> foto(Long id, String url, String origen) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("url", url);
        m.put("origen", origen);
        return m;
    }
}
