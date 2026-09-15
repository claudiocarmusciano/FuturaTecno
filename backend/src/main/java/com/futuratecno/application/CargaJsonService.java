package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.api.dto.CategoriaNombresDTO;
import com.futuratecno.domain.Imagen;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ImagenRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carga manual de productos a partir de un JSON (sección admin "Cargar artículos por JSON").
 * Emula el guardado del parsing pero además: carga imágenes (galería) e intenta clasificar la
 * categoría automáticamente. Precio siempre en USD ({@code precio_usd}). Dedup por proveedor+marca+modelo.
 */
@Service
public class CargaJsonService {
    private static final Logger logger = LoggerFactory.getLogger(CargaJsonService.class);
    private static final String FUENTE = "JSON";
    // Orden preferido de las specs para armar el texto de la variante.
    private static final String[] CLAVES_SPEC = {
            "procesador", "ram", "almacenamiento", "pantalla", "gpu", "sistema_operativo", "otros"
    };

    private final ImagenManualService imagenManualService;
    private final DescripcionManualService descripcionManualService;
    private final AtributosManualService atributosManualService;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final ImagenRepository imagenRepository;
    private final CategoriaClasificadorService categoriaClasificadorService;
    private final CategoriaService categoriaService;
    private final ImageUrlValidatorService imageUrlValidatorService;

    public CargaJsonService(ImagenManualService imagenManualService,
                            DescripcionManualService descripcionManualService,
                            AtributosManualService atributosManualService,
                            ProductoRepository productoRepository,
                            VarianteRepository varianteRepository,
                            ProveedorRepository proveedorRepository,
                            ImagenRepository imagenRepository,
                            CategoriaClasificadorService categoriaClasificadorService,
                            CategoriaService categoriaService,
                            ImageUrlValidatorService imageUrlValidatorService) {
        this.imagenManualService = imagenManualService;
        this.descripcionManualService = descripcionManualService;
        this.atributosManualService = atributosManualService;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.proveedorRepository = proveedorRepository;
        this.imagenRepository = imagenRepository;
        this.categoriaClasificadorService = categoriaClasificadorService;
        this.categoriaService = categoriaService;
        this.imageUrlValidatorService = imageUrlValidatorService;
    }

    @Transactional
    public CargaJsonResponse cargar(Long proveedorId, List<ArticuloJsonDTO> articulos) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + proveedorId));

        CargaJsonResponse res = new CargaJsonResponse();
        int creados = 0, actualizados = 0, omitidos = 0, sinCategoria = 0;

        for (ArticuloJsonDTO art : (articulos != null ? articulos : List.<ArticuloJsonDTO>of())) {
            String marca = limpiar(art.getMarca());
            String modelo = derivarModelo(art, marca);
            BigDecimal precio = art.getPrecioUsd();

            if (marca == null || modelo == null || precio == null || precio.signum() <= 0) {
                omitidos++;
                res.getItems().add(new CargaJsonResponse.Item(
                        etiqueta(marca, modelo, art), "omitido", null,
                        "Falta marca, modelo o precio_usd válido."));
                continue;
            }

            // Exacto primero; si no, la clave suelta. El modelo lo redacta la IA en cada carga, así
            // que "iPhone 17 Pro 256GB eSIM" y "iPhone 17 Pro 256 GB (eSIM)" son el mismo teléfono
            // y sin esto nacía un producto nuevo por cada redacción: el catálogo mostraba el mismo
            // artículo dos veces a precios distintos y había que recargarle la foto a mano.
            // La clave conserva capacidad, color y versión, así que un 512GB nunca cae sobre un
            // 256GB. Solo aplica a esta vía: Elit e Invid deduplican por codigo_externo, que es
            // estable, y no tienen este problema.
            var existente = productoRepository.findByProveedorIdAndMarcaAndModelo(proveedorId, marca, modelo)
                    .or(() -> productoRepository
                            .idPorClaveSuelta(proveedorId, ImagenManualService.clave(marca, modelo))
                            .flatMap(productoRepository::findById));
            boolean nuevo = existente.isEmpty();
            Producto producto = existente.orElseGet(Producto::new);
            if (nuevo) {
                producto.setProveedor(proveedor);
                producto.setMarca(marca);
                producto.setModelo(modelo);
                producto.setFuente(FUENTE);
            }
            // Una nueva carga manual debe volver a publicar un producto que se había dado de baja.
            // De lo contrario figura como "actualizado" pero continúa oculto del panel y catálogo.
            producto.setActivo(true);
            producto.setCategoria(limpiar(art.getCategoria()));

            Optional<String> recordada = imagenManualService.buscar(marca, modelo);
            // Las URLs del JSON solo entran si la base no tenía nada, y recién ahí se verifica que
            // estén vivas: hasta ahora alcanzaba con que empezaran por "http". Una URL muerta no
            // solo publicaba el producto con la foto rota — además se guardaba como imagen
            // automática y se reusaba en cada carga futura de ese marca+modelo, así que el error
            // de un día se volvía permanente. Si no valida, el producto queda SIN imagen y aparece
            // en Admin → Imágenes para resolverlo ahí.
            List<String> delJson = recordada.isEmpty() ? imagenesLimpias(art.getImagenes()) : List.of();
            List<String> vivas = soloVivas(delJson);
            boolean fotoRota = vivas.isEmpty() && !delJson.isEmpty();
            List<String> imagenes = recordada.map(List::of).orElse(vivas);
            if (!imagenes.isEmpty()) {
                producto.setImagenUrl(imagenes.get(0));
                // Una imagen nueva se recuerda para que el próximo listado con este marca+modelo no
                // vuelva a pagar una búsqueda. Entra como automática: con ON CONFLICT DO NOTHING
                // jamás pisa una que el admin haya elegido a mano. Si ya venía de la memoria no se
                // reescribe, sería un INSERT por artículo que no cambia nada.
                if (recordada.isEmpty()) imagenManualService.guardarAutomatica(marca, modelo, imagenes.get(0));
            }

            // Categoría y medidas ya resueltas para este marca+modelo, aunque haya sido con otro
            // proveedor. Solo rellena huecos: lo que la fila ya tenga cargado manda.
            atributosManualService.aplicar(producto);

            // Clasificación automática: solo si todavía no tiene categoría (ni propia ni recordada).
            // Si no se puede resolver (categoría ambigua o IA sin crédito), queda null → el admin
            // la asigna a mano.
            if (producto.getCategoriaId() == null) {
                try {
                    producto.setCategoriaId(categoriaClasificadorService.clasificar(producto, limpiar(art.getCategoria())));
                } catch (Exception e) {
                    logger.warn("Clasificación falló para '{} {}': {}", marca, modelo, e.toString());
                }
            }
            final Producto prod = productoRepository.save(producto);

            // Variante con el precio (siempre USD) y las specs.
            // La descripción curada a mano gana sobre la del JSON, igual que la imagen: si alguien
            // ya la corrigió, un reimport no debe pisarla con el texto crudo del proveedor.
            String especificaciones = descripcionManualService.buscar(marca, modelo)
                    .orElseGet(() -> construirEspecificaciones(art));
            // Un producto cargado por JSON tiene UNA sola variante: la capacidad, el color y la
            // versión SIM/eSIM viajan dentro del modelo ("iPhone 17 Pro 256GB eSIM"), así que las
            // especificaciones son una descripción y no algo que distinga. Buscar la variante por
            // ese texto creaba una fila nueva cada vez que la IA lo redactaba distinto ("256GB ·
            // Versión eSIM" vs "256GB · eSIM") en lugar de actualizar el precio, y el catálogo
            // terminaba ofreciendo el mismo teléfono dos veces a precios distintos (ver V35).
            // Ante varias activas gana la tocada más recientemente, mismo criterio que la V35.
            Variante variante = varianteRepository.findByProductoIdAndActivo(prod.getId(), true).stream()
                    .max(Comparator.comparing(Variante::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(Variante::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElseGet(() -> {
                        Variante v = new Variante();
                        v.setProducto(prod);
                        v.setStock(0);
                        return v;
                    });
            variante.setEspecificaciones(especificaciones);
            variante.setCostoUsd(precio.setScale(2, RoundingMode.HALF_UP));
            variante.setMonedaOrigen("USD");
            variante.setPrecioOrigen(precio);
            variante.setActivo(true);
            varianteRepository.save(variante);

            // Galería de imágenes: se reemplaza por la del JSON (borra las anteriores del producto).
            if (!imagenes.isEmpty()) {
                imagenRepository.deleteAll(imagenRepository.findByProductoIdAndActivoOrderByOrden(prod.getId(), true));
                int orden = 0;
                for (String url : imagenes) {
                    Imagen img = new Imagen();
                    img.setProducto(prod);
                    img.setUrl(url);
                    img.setOrden(orden++);
                    img.setActivo(true);
                    imagenRepository.save(img);
                }
            }

            String categoriaPath = pathDe(prod.getCategoriaId());
            if (categoriaPath == null) sinCategoria++;
            if (nuevo) creados++; else actualizados++;
            res.getItems().add(new CargaJsonResponse.Item(
                    marca + " " + modelo, nuevo ? "creado" : "actualizado", categoriaPath,
                    fotoRota ? "La imagen del JSON no responde. Quedó sin foto: cargala desde Admin → Imágenes." : null));
        }

        res.setCreados(creados);
        res.setActualizados(actualizados);
        res.setOmitidos(omitidos);
        res.setSinCategoria(sinCategoria);
        res.setMensaje(String.format(
                "Carga completada: %d creados, %d actualizados, %d omitidos. %d quedaron sin categoría (asignar a mano).",
                creados, actualizados, omitidos, sinCategoria));
        logger.info(res.getMensaje());
        return res;
    }

    /** Modelo "limpio": usa `modelo` si vino; si no, la primera parte de `modelo_exacto` (o `listado`), sin el prefijo de marca. */
    private String derivarModelo(ArticuloJsonDTO art, String marca) {
        if (limpiar(art.getModelo()) != null) return limpiar(art.getModelo());
        String base = art.getModeloExacto() != null ? art.getModeloExacto()
                : (art.getListado() != null ? art.getListado() : null);
        if (base == null) return null;
        base = base.trim();
        int coma = base.indexOf(',');
        if (coma > 0) base = base.substring(0, coma).trim();
        // "ASUS 15.6\" ... – USD 320" (listado): cortar en un guión largo de precio si quedó
        int guion = base.indexOf('–');
        if (guion > 0) base = base.substring(0, guion).trim();
        if (marca != null && base.toLowerCase().startsWith(marca.toLowerCase() + " ")) {
            base = base.substring(marca.length()).trim();
        }
        if (base.length() > 200) base = base.substring(0, 200).trim();
        return base.isEmpty() ? null : base;
    }

    /** Texto de especificaciones (≤500) a partir del objeto `especificaciones`, en orden legible. */
    private String construirEspecificaciones(ArticuloJsonDTO art) {
        Map<String, Object> esp = art.getEspecificaciones();
        if (esp == null || esp.isEmpty()) return "";
        List<String> partes = new ArrayList<>();
        for (String k : CLAVES_SPEC) {
            String v = valor(esp.get(k));
            if (v != null) partes.add(v);
        }
        for (Map.Entry<String, Object> e : esp.entrySet()) {
            boolean yaEsta = false;
            for (String k : CLAVES_SPEC) if (k.equals(e.getKey())) { yaEsta = true; break; }
            String v = valor(e.getValue());
            if (!yaEsta && v != null) partes.add(v);
        }
        String texto = String.join(" · ", partes);
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }

    /**
     * Descarta las URLs que no devuelven una imagen de verdad: 404, HTML disfrazado, host caído.
     * Solo se llama cuando la imagen va a usarse (la base no tenía nada), así que en una recarga
     * de un listado ya cargado no dispara ni una sola petición. El validador pide únicamente los
     * primeros 1.024 bytes y corta a los 3 s de conexión y 5 s de lectura.
     */
    private List<String> soloVivas(List<String> urls) {
        if (urls.isEmpty()) return List.of();
        List<String> vivas = new ArrayList<>();
        for (String url : urls) {
            if (imageUrlValidatorService.esImagenDirecta(url)) vivas.add(url);
            else logger.info("Imagen descartada, no responde como imagen: {}", url);
        }
        return vivas;
    }

    private List<String> imagenesLimpias(List<String> imagenes) {
        LinkedHashSet<String> unicas = new LinkedHashSet<>();   // dedup preservando el orden
        if (imagenes != null) {
            for (String u : imagenes) {
                if (u != null && !u.isBlank() && u.trim().startsWith("http")) unicas.add(u.trim());
            }
        }
        return new ArrayList<>(unicas);
    }

    private String pathDe(Long categoriaId) {
        CategoriaNombresDTO n = categoriaService.resolverNombres(categoriaId);
        if (n == null) return null;
        return n.getCategoriaPadre() != null ? n.getCategoriaPadre() + " > " + n.getSubcategoria() : n.getSubcategoria();
    }

    private String etiqueta(String marca, String modelo, ArticuloJsonDTO art) {
        if (marca != null || modelo != null) return ((marca != null ? marca : "") + " " + (modelo != null ? modelo : "")).trim();
        return art.getListado() != null ? art.getListado() : "(sin nombre)";
    }

    private String valor(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
