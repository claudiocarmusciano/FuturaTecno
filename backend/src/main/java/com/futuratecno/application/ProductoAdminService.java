package com.futuratecno.application;

import com.futuratecno.api.dto.BuscarImagenesResponse;
import com.futuratecno.api.dto.ClasificarCategoriasResponse;
import com.futuratecno.api.dto.ImagenSimilarDTO;
import com.futuratecno.api.dto.ProductoAdminDTO;
import com.futuratecno.api.dto.ProductoEditDTO;
import com.futuratecno.api.dto.VarianteEditDTO;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Variante;
import com.futuratecno.domain.Categoria;
import com.futuratecno.infrastructure.CategoriaRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductoAdminService {
    private static final Logger logger = LoggerFactory.getLogger(ProductoAdminService.class);
    // La búsqueda incluye servicios externos. Un lote chico evita que una única acción de la
    // interfaz quede esperando varios minutos si alguno de ellos está lento o bloquea requests.
    private static final int MAXIMO_IMAGENES_POR_EJECUCION = 5;
    // Candidatas de la propia base que se le ofrecen al admin. Son pocas a propósito: elegir entre
    // veinte miniaturas parecidas no ayuda a decidir, y las buenas son siempre las primeras, que es
    // donde la clave coincide más.
    private static final int CANDIDATAS_POR_PRODUCTO = 8;

    private final ImagenManualService imagenManualService;
    private final DescripcionManualService descripcionManualService;
    private final AtributosManualService atributosManualService;
    private final MargenManualService margenManualService;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final IcecatService icecatService;
    private final GoogleImageService googleImageService;
    private final AnthropicImageService anthropicImageService;
    private final DuckDuckGoImageService duckDuckGoImageService;
    private final ImageUrlValidatorService imageUrlValidatorService;
    private final CotizacionService cotizacionService;
    private final PrecioService precioService;
    private final CategoriaClasificadorService categoriaClasificadorService;
    private final CategoriaService categoriaService;
    private final CategoriaRepository categoriaRepository;
    private final IdentidadTransicionService identidadTransicionService;

    public ProductoAdminService(ImagenManualService imagenManualService,
                                DescripcionManualService descripcionManualService,
                                AtributosManualService atributosManualService,
                                MargenManualService margenManualService,
                                ProductoRepository productoRepository,
                                VarianteRepository varianteRepository,
                                IcecatService icecatService,
                                GoogleImageService googleImageService,
                                AnthropicImageService anthropicImageService,
                                DuckDuckGoImageService duckDuckGoImageService,
                                ImageUrlValidatorService imageUrlValidatorService,
                                CotizacionService cotizacionService,
                                PrecioService precioService,
                                CategoriaClasificadorService categoriaClasificadorService,
                                CategoriaService categoriaService,
                                CategoriaRepository categoriaRepository,
                                IdentidadTransicionService identidadTransicionService) {
        this.imagenManualService = imagenManualService;
        this.descripcionManualService = descripcionManualService;
        this.atributosManualService = atributosManualService;
        this.margenManualService = margenManualService;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.icecatService = icecatService;
        this.googleImageService = googleImageService;
        this.anthropicImageService = anthropicImageService;
        this.duckDuckGoImageService = duckDuckGoImageService;
        this.imageUrlValidatorService = imageUrlValidatorService;
        this.cotizacionService = cotizacionService;
        this.precioService = precioService;
        this.categoriaClasificadorService = categoriaClasificadorService;
        this.categoriaService = categoriaService;
        this.categoriaRepository = categoriaRepository;
        this.identidadTransicionService = identidadTransicionService;
    }

    /**
     * Con miles de productos, pedir las variantes de a uno por producto (lo que hacía antes)
     * volvía este listado en miles de queries. Se traen las variantes de TODO el catálogo en una
     * sola consulta (con IN) y se agrupan por producto en memoria.
     */
    @Transactional(readOnly = true)
    public List<ProductoAdminDTO> listar() {
        List<Producto> productos = productoRepository.findByActivo(true);
        if (productos.isEmpty()) return List.of();

        List<Long> ids = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, List<Variante>> variantesPorProducto = varianteRepository
                .findByProductoIdInAndActivo(ids, true).stream()
                .collect(Collectors.groupingBy(v -> v.getProducto().getId()));
        // Una sola vez para todo el listado: la cotización es la misma para los 4.000 productos.
        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();

        return productos.stream()
                .map(p -> {
                    List<Variante> variantes = variantesPorProducto.getOrDefault(p.getId(), List.of());
                    var nombres = categoriaService.resolverNombres(p.getCategoriaId());
                    ProductoAdminDTO dto = new ProductoAdminDTO(
                            p.getId(),
                            nombres != null ? nombres.getSubcategoria() : null,
                            p.getMarca(),
                            p.getModelo(),
                            p.getProveedor() != null ? p.getProveedor().getNombre() : null,
                            p.getImagenUrl());
                    dto.setCategoriaId(p.getCategoriaId());
                    dto.setSku(sku(p));
                    if (!variantes.isEmpty()) {
                        Variante v = variantes.get(0);
                        dto.setEspecificaciones(v.getEspecificaciones());
                        // Para Admin → Imágenes: el color que dice el artículo, así la foto se elige
                        // del color correcto. Mismo vocabulario que la identidad.
                        dto.setColores(IdentidadProductoService.coloresVisibles(p.getModelo() + " " + v.getEspecificaciones()));
                        // Se calcula con el MISMO PrecioService que el catálogo público, así el
                        // admin ve exactamente el precio que ve el cliente — incluido el override
                        // de margen del producto (V41). Duplicar la cuenta acá sería mentirle.
                        dto.setCostoUsd(v.getCostoUsd());
                        if (v.getCostoUsd() != null) {
                            BigDecimal venta = precioService.precioVentaUsd(v, p, p.getProveedor());
                            dto.setVentaUsd(venta);
                            if (cotizacion != null) dto.setVentaArs(precioService.aArs(venta, cotizacion));
                        }
                    }
                    dto.setUltimaActualizacion(calcularUltimaActualizacion(p, variantes));
                    dto.setVistoEnSync(p.getVistoEnSyncAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * SKU camuflado: código corto del proveedor + identificador del artículo. No delata el proveedor
     * (se ve como un código de referencia interno), pero permite cruzarlo con el sistema del mayorista.
     */
    private String sku(Producto p) {
        String prefijo = (p.getProveedor() != null && p.getProveedor().getCodigo() != null && !p.getProveedor().getCodigo().isBlank())
                ? p.getProveedor().getCodigo() : "FT";
        String sufijo = (p.getCodigoExterno() != null && !p.getCodigoExterno().isBlank())
                ? p.getCodigoExterno() : "P" + p.getId();
        return prefijo + "-" + sufijo;
    }

    /** Fecha más reciente entre el producto y sus variantes activas (refleja la última actualización de precio). */
    private LocalDateTime calcularUltimaActualizacion(Producto p, List<Variante> variantesActivas) {
        LocalDateTime ultima = p.getUpdatedAt();
        for (Variante v : variantesActivas) {
            if (v.getUpdatedAt() != null && (ultima == null || v.getUpdatedAt().isAfter(ultima))) {
                ultima = v.getUpdatedAt();
            }
        }
        return ultima;
    }

    /**
     * Productos publicados que la sincronización no toca hace más de {@code dias}. En la práctica
     * son artículos que el mayorista dejó de ofrecer: la sync corre en modo "solo existentes", así
     * que lo que desapareció del feed se queda con el precio y el stock del último día que apareció.
     * Ordenados del más viejo al más nuevo, que es el orden en que conviene revisarlos.
     */
    @Transactional(readOnly = true)
    public List<ProductoAdminDTO> listarVencidos(int dias) {
        LocalDateTime corte = LocalDateTime.now().minusDays(Math.max(dias, 1));
        return listar().stream()
                .filter(d -> senalDeVigencia(d) != null && senalDeVigencia(d).isBefore(corte))
                .sorted(Comparator.comparing(ProductoAdminService::senalDeVigencia))
                .collect(Collectors.toList());
    }

    /**
     * La prueba más reciente de que el producto sigue vigente.
     *
     * <p>Para un mayorista es `vistoEnSync`: la última vez que apareció en su feed. NO sirve
     * `ultimaActualizacion`, que responde "¿cambió algo?": el 2026-09-16 una importación de Invid
     * informó 1.188 actualizados y solo 70 movieron la fecha, porque el resto vino con precio y
     * stock idénticos. Filtrar por eso habría propuesto dar de baja 1.134 productos que Invid
     * sigue ofreciendo (V40).
     *
     * <p>Para lo cargado por JSON no hay feed, así que se cae a `ultimaActualizacion`, que ahí sí
     * es honesta: refleja cuándo el admin recargó ese listado.
     */
    private static LocalDateTime senalDeVigencia(ProductoAdminDTO d) {
        return d.getVistoEnSync() != null ? d.getVistoEnSync() : d.getUltimaActualizacion();
    }

    @Transactional
    public void eliminar(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        recordarParaFuturasAltas(producto);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    /**
     * Antes de sacar un producto del catálogo se guarda por marca+modelo lo que costó trabajo
     * conseguir: la imagen, la categoría y las medidas. La fila queda igual (baja lógica), pero la
     * memoria es lo que hace que una futura alta —incluso desde OTRO proveedor, que crearía una
     * fila nueva— recupere todo sin volver a cargarlo a mano.
     *
     * <p>Cada dato va a la tabla que le corresponde y con la regla de esa tabla: la imagen entra
     * como automática, así que nunca pisa una que el admin haya elegido a mano (V32); la categoría
     * y las medidas incluyen a los mayoristas, porque ninguno trae peso ni dimensiones (V34); y la
     * descripción se saltea para Elit e Invid, que reescriben su ficha en cada sync (V33).
     */
    private void recordarParaFuturasAltas(Producto p) {
        if (p == null) return;
        atributosManualService.recordar(p);
        if (p.getImagenUrl() != null && !p.getImagenUrl().isBlank()) {
            imagenManualService.guardarAutomatica(p.getMarca(), p.getModelo(), p.getImagenUrl());
        }
        if (!DescripcionManualService.esDeMayorista(p.getFuente())) {
            varianteRepository.findByProductoIdAndActivo(p.getId(), true).stream()
                    .map(Variante::getEspecificaciones)
                    .filter(e -> e != null && !e.isBlank())
                    .findFirst()
                    .ifPresent(e -> descripcionManualService.guardar(p.getMarca(), p.getModelo(), e));
        }
    }

    /** Da de baja varios productos sin borrarlos físicamente. */
    @Transactional
    public int eliminarMasivamente(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        productoRepository.findAllById(ids).forEach(this::recordarParaFuturasAltas);
        return productoRepository.desactivarPorIds(ids);
    }

    /** Devuelve un producto con sus variantes en formato editable (precio en moneda de origen). */
    @Transactional(readOnly = true)
    /**
     * Fotos ya presentes en el catálogo que podrían servir para este producto, para que el admin
     * elija antes de gastar una búsqueda externa.
     *
     * <p><b>No se valida la URL desde el servidor, a propósito.</b> Se intentó y salió mal: pedir
     * cada candidata desde Railway descartaba todas las de `encrypted-tbn*.gstatic.com` —Google
     * bloquea las IPs de datacenter, igual que MercadoLibre—, y son 1.101 de las 4.122 imágenes
     * del catálogo. Eso dejaba sin candidatas a 141 de los 210 productos que sí tenían una. Eran
     * falsos negativos: esas fotos cargan perfecto en un navegador, que es donde importan.
     *
     * <p>Quien valida es la grilla del admin, que renderiza cada miniatura: una foto muerta se ve
     * como un recuadro vacío y queda marcada como inservible. Valida desde donde la imagen se va a
     * ver de verdad, no desde un datacenter, y de paso el endpoint responde sin salir a la red.
     */
    public List<ImagenSimilarDTO> imagenesSimilares(Long productoId) {
        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        return imagenManualService.similares(p.getMarca(), p.getModelo(), CANDIDATAS_POR_PRODUCTO).stream()
                .filter(c -> !c.productoId().equals(productoId))
                .toList();
    }

    public ProductoEditDTO obtenerParaEditar(Long productoId) {
        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

        ProductoEditDTO dto = new ProductoEditDTO();
        dto.setId(p.getId());
        dto.setCategoriaId(p.getCategoriaId());
        var nombres = categoriaService.resolverNombres(p.getCategoriaId());
        dto.setCategoria(nombres != null ? nombres.getSubcategoria() : null);
        dto.setMarca(p.getMarca());
        dto.setModelo(p.getModelo());
        dto.setProveedor(p.getProveedor() != null ? p.getProveedor().getNombre() : null);
        dto.setImagenUrl(p.getImagenUrl());
        dto.setPesoGramos(p.getPesoGramos());
        dto.setAltoCm(p.getAltoCm());
        dto.setAnchoCm(p.getAnchoCm());
        dto.setLargoCm(p.getLargoCm());
        Categoria hoja = p.getCategoriaId() != null ? categoriaRepository.findById(p.getCategoriaId()).orElse(null) : null;
        Categoria padre = hoja != null ? hoja.getPadre() : null;
        dto.setPesoGramosDefault(primero(hoja != null ? hoja.getPesoGramosDefault() : null, padre != null ? padre.getPesoGramosDefault() : null));
        dto.setAltoCmDefault(primero(hoja != null ? hoja.getAltoCmDefault() : null, padre != null ? padre.getAltoCmDefault() : null));
        dto.setAnchoCmDefault(primero(hoja != null ? hoja.getAnchoCmDefault() : null, padre != null ? padre.getAnchoCmDefault() : null));
        dto.setLargoCmDefault(primero(hoja != null ? hoja.getLargoCmDefault() : null, padre != null ? padre.getLargoCmDefault() : null));
        // El override del producto y, aparte, el valor del proveedor: el editor muestra el segundo
        // como referencia de qué se aplica cuando el campo queda vacío (mismo patrón que el peso).
        dto.setMargenPorcentaje(p.getMargenPorcentaje());
        dto.setFletePorcentaje(p.getFletePorcentaje());
        if (p.getProveedor() != null) {
            dto.setMargenPorcentajeProveedor(p.getProveedor().getMargenPorcentaje());
            dto.setFletePorcentajeProveedor(p.getProveedor().getFletePorcentaje());
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

    private Integer primero(Integer... valores) {
        for (Integer valor : valores) if (valor != null) return valor;
        return null;
    }

    /** Actualiza los datos del producto y de cada variante. Recalcula el costo USD según la moneda. */
    @Transactional
    public ProductoEditDTO actualizarProducto(Long productoId, ProductoEditDTO dto) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        String marcaAntes = producto.getMarca(), modeloAntes = producto.getModelo();
        if (dto.getCategoriaId() != null) producto.setCategoriaId(dto.getCategoriaId());
        if (dto.getMarca() != null && !dto.getMarca().isBlank()) producto.setMarca(dto.getMarca().trim());
        if (dto.getModelo() != null && !dto.getModelo().isBlank()) producto.setModelo(dto.getModelo().trim());
        // La imagen se puede corregir desde el editor. Un campo vacío elimina la URL inválida.
        producto.setImagenUrl(dto.getImagenUrl() != null && !dto.getImagenUrl().isBlank()
                ? dto.getImagenUrl().trim() : null);
        imagenManualService.guardar(producto.getMarca(), producto.getModelo(), producto.getImagenUrl());
        // A diferencia de marca/modelo, acá null es un valor válido y querido: "sin override,
        // usar el default de la categoría". Por eso se pisa siempre, no solo cuando viene cargado.
        producto.setPesoGramos(dto.getPesoGramos());
        producto.setAltoCm(dto.getAltoCm());
        producto.setAnchoCm(dto.getAnchoCm());
        producto.setLargoCm(dto.getLargoCm());
        // Mismo criterio para el margen y el flete (V41): vaciarlos vuelve al valor del proveedor,
        // que no es lo mismo que poner 0% — eso sería vender al costo.
        producto.setMargenPorcentaje(dto.getMargenPorcentaje());
        producto.setFletePorcentaje(dto.getFletePorcentaje());
        productoRepository.save(producto);
        // Categoría y medidas quedan recordadas por marca+modelo, para que el mismo artículo
        // cargado con otro proveedor no vuelva a nacer sin categoría ni medidas.
        atributosManualService.recordar(producto);
        // El margen se recuerda por marca+modelo PERO dentro del proveedor: es una decisión
        // comercial atada a lo que ese mayorista cobra. Vaciar los dos campos borra la memoria,
        // que es cómo el admin dice "volvé al margen del proveedor y olvidate".
        if (producto.getMargenPorcentaje() == null && producto.getFletePorcentaje() == null) {
            margenManualService.olvidar(producto.getProveedor() != null ? producto.getProveedor().getId() : null,
                    producto.getMarca(), producto.getModelo());
        } else {
            margenManualService.recordar(producto);
        }

        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();

        // Null = no vino ninguna variante en el request, así que no hay nada que recordar (y no
        // hay que borrar lo que ya estaba). "" sí es un valor: el admin vació la descripción.
        String descripcionEditada = null, descripcionAntes = null;
        if (dto.getVariantes() != null) {
            for (VarianteEditDTO ve : dto.getVariantes()) {
                if (ve.getId() == null) continue;
                Variante v = varianteRepository.findById(ve.getId()).orElse(null);
                if (v == null || v.getProducto() == null || !v.getProducto().getId().equals(productoId)) continue;

                String previa = v.getEspecificaciones();
                v.setEspecificaciones(ve.getEspecificaciones() != null ? ve.getEspecificaciones().trim() : "");
                // La descripción del producto es la de su primera variante, igual que en el listado.
                if (descripcionEditada == null) { descripcionEditada = v.getEspecificaciones(); descripcionAntes = previa; }
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

        // Se recuerda solo lo propio: Elit e Invid reescriben su ficha en cada sincronización, así
        // que guardar la suya no aportaría y taparía descripciones escritas a mano.
        if (descripcionEditada != null && !DescripcionManualService.esDeMayorista(producto.getFuente())) {
            descripcionManualService.guardar(producto.getMarca(), producto.getModelo(), descripcionEditada);
        }
        // Si cambió lo que define qué artículo es, la identidad guardada tiene que acompañar: la
        // carga por JSON compara contra ella, no contra el texto (ver recalcularTrasEdicion).
        boolean cambioNombre = !Objects.equals(marcaAntes, producto.getMarca()) || !Objects.equals(modeloAntes, producto.getModelo());
        boolean cambioDescripcion = descripcionEditada != null && !Objects.equals(descripcionAntes, descripcionEditada);
        if (cambioNombre || cambioDescripcion) {
            String especificaciones = descripcionEditada != null ? descripcionEditada : especificacionesDe(productoId);
            var resultado = identidadTransicionService.recalcularTrasEdicion(producto, especificaciones);
            logger.info("Identidad del producto {} tras editarlo: {} ({})", productoId, resultado, producto.getIdentidadClave());
        }

        return obtenerParaEditar(productoId);
    }

    /** La descripción del producto: la de su primera variante activa, igual que en el listado. */
    private String especificacionesDe(Long productoId) {
        return varianteRepository.findByProductoIdAndActivo(productoId, true).stream()
                .min(Comparator.comparing(Variante::getId))
                .map(Variante::getEspecificaciones).orElse(null);
    }

    /** Asigna el mismo categoriaId a varios productos de una. Devuelve cuántos se actualizaron. */
    @Transactional
    public int asignarCategoriaMasiva(List<Long> ids, Long categoriaId) {
        if (ids == null || ids.isEmpty() || categoriaId == null) return 0;
        int n = 0;
        for (Long id : ids) {
            Producto p = productoRepository.findById(id).orElse(null);
            if (p == null) continue;
            p.setCategoriaId(categoriaId);
            productoRepository.save(p);
            atributosManualService.recordar(p);
            n++;
        }
        return n;
    }

    @Transactional
    public ProductoAdminDTO actualizarImagen(Long productoId, String url) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        producto.setImagenUrl(url != null && !url.isBlank() ? url.trim() : null);
        imagenManualService.guardar(producto.getMarca(), producto.getModelo(), producto.getImagenUrl());
        productoRepository.save(producto);
        var nombres = categoriaService.resolverNombres(producto.getCategoriaId());
        ProductoAdminDTO dto = new ProductoAdminDTO(
                producto.getId(), nombres != null ? nombres.getSubcategoria() : null, producto.getMarca(),
                producto.getModelo(),
                producto.getProveedor() != null ? producto.getProveedor().getNombre() : null,
                producto.getImagenUrl());
        dto.setCategoriaId(producto.getCategoriaId());
        dto.setSku(sku(producto));
        return dto;
    }

    /**
     * Cascada de búsqueda de imagen para cada producto sin imagen:
     *   1) Icecat (por marca + código, si está configurado) — gratis, matchea pocos.
     *   2) Google Custom Search Images, si está configurado.
     *   3) DuckDuckGo Images, probando varios candidatos.
     *   4) Anthropic web search → og:image como último recurso.
     * Lo que no se encuentre queda para carga manual.
     */
    @Transactional
    public BuscarImagenesResponse buscarImagenesFaltantes() {
        boolean icecatOk = icecatService.estaConfigurado();
        boolean googleOk = googleImageService.estaConfigurado();
        boolean anthropicOk = anthropicImageService.estaConfigurado();

        List<Producto> faltantes = productoRepository.findByActivo(true).stream()
                .filter(p -> p.getImagenUrl() == null || p.getImagenUrl().isBlank())
                // Primero los que nunca se intentaron. Cuando se agoten, los intentos fallidos
                // vuelven a la cola por antigüedad para permitir que nuevas fuentes los resuelvan.
                .sorted(Comparator.comparing(Producto::getImagenBusquedaAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        List<Producto> sinImagen = faltantes.stream()
                .limit(MAXIMO_IMAGENES_POR_EJECUCION)
                .collect(Collectors.toList());

        int desdeMemoria = 0;
        int desdeIcecat = 0, desdeGoogle = 0, desdeAnthropic = 0, desdeDuckDuckGo = 0;
        for (Producto p : sinImagen) {
            String url = imagenManualService.buscar(p.getMarca(), p.getModelo()).orElse(null);
            // Misma regla que la carga por JSON: una foto recordada que da 404/410 no se reparte.
            if (url != null && imageUrlValidatorService.verificar(url) == ImageUrlValidatorService.Verificacion.MUERTA) {
                imagenManualService.olvidarUrl(url);
                logger.warn("Imagen recordada muerta para producto {}, se descarta: {}", p.getId(), url);
                url = null;
            }
            if (url != null) desdeMemoria++;

            // Especificaciones de la primera variante (CPU/RAM/SSD, color, capacidad, etc.):
            // ayudan a encontrar la publicación EXACTA en MercadoLibre. Se limpian los separadores.
            String especificaciones = "";
            if (p.getVariantes() != null && !p.getVariantes().isEmpty()) {
                String esp = p.getVariantes().get(0).getEspecificaciones();
                if (esp != null) {
                    especificaciones = esp.replace("/", " ").replaceAll("\\s+", " ").trim();
                }
            }

            // La categoría no entra en la consulta: "Tablets Samsung ..." u otros nombres de
            // hoja ensucian el resultado. Marca y modelo son la identidad del artículo.
            String consulta = construirConsultaImagen(p.getMarca(), p.getModelo(), especificaciones);

            // 1) Icecat (rápido y gratis; rara vez matchea esta clase de productos)
            if (url == null && icecatOk) {
                try {
                    Optional<String> r = icecatService.buscarImagen(p.getMarca(), p.getModelo());
                    if (r.isPresent() && imageUrlValidatorService.esImagenDirecta(r.get())) {
                        url = r.get();
                        desdeIcecat++;
                    }
                } catch (Exception e) {
                    logger.warn("Icecat falló para producto {}: {}", p.getId(), e.getMessage());
                }
            }

            // 2) Google Images formal. Se recorren candidatos porque el primero puede estar
            // caído, responder HTML o bloquear descargas desde el servidor.
            if (url == null && googleOk) {
                try {
                    url = primeraImagenValida(googleImageService.buscarImagenes(consulta));
                    if (url != null) desdeGoogle++;
                } catch (Exception e) {
                    logger.warn("Google falló para producto {}: {}", p.getId(), e.getMessage());
                }
            }

            // 3) DuckDuckGo Images como respaldo rápido, recorriendo más de un candidato.
            if (url == null) {
                try {
                    url = primeraImagenValida(duckDuckGoImageService.buscarImagenes(consulta));
                    if (url != null) desdeDuckDuckGo++;
                } catch (Exception e) {
                    logger.warn("DuckDuckGo falló para producto {}: {}", p.getId(), e.getMessage());
                }
            }

            // 4) Anthropic (búsqueda web + og:image) como fallback preciso, con timeout.
            if (url == null && anthropicOk) {
                try {
                    Optional<String> r = anthropicImageService.buscarImagen(consulta);
                    if (r.isPresent() && imageUrlValidatorService.esImagenDirecta(r.get())) {
                        url = r.get();
                        desdeAnthropic++;
                    }
                } catch (Exception e) {
                    logger.warn("Anthropic falló para producto {}: {}", p.getId(), e.getMessage());
                }
            }

            if (url != null) {
                p.setImagenUrl(url);
                productoRepository.save(p);
            } else {
                // Un error de una fuente no debe bloquear toda la cola en los próximos clics.
                p.setImagenBusquedaAt(LocalDateTime.now());
                productoRepository.save(p);
            }
        }

        int encontradas = desdeMemoria + desdeIcecat + desdeGoogle + desdeAnthropic + desdeDuckDuckGo;
        int noEncontradas = sinImagen.size() - encontradas;
        int sinProcesar = faltantes.size() - sinImagen.size();
        int pendientesTotales = faltantes.size() - encontradas;
        String mensaje = String.format(
                "Búsqueda completada: %d con imagen (%d guardadas, %d Google, %d Icecat, %d Anthropic, %d DuckDuckGo), %d sin resultado (de %d procesados). Quedan %d artículo(s) sin imagen en total.%s",
                encontradas, desdeMemoria, desdeGoogle, desdeIcecat, desdeAnthropic, desdeDuckDuckGo,
                noEncontradas, sinImagen.size(), pendientesTotales,
                sinProcesar > 0 ? " " + sinProcesar + " todavía no fueron procesados." : "");
        logger.info(mensaje);

        BuscarImagenesResponse resp = new BuscarImagenesResponse(sinImagen.size(), encontradas, noEncontradas, mensaje);
        resp.setDesdeIcecat(desdeIcecat);
        resp.setDesdeGoogle(desdeGoogle);
        resp.setDesdeAnthropic(desdeAnthropic);
        resp.setDesdeDuckDuckGo(desdeDuckDuckGo);
        return resp;
    }

    private String primeraImagenValida(List<String> candidatos) {
        for (String candidato : candidatos) {
            if (imageUrlValidatorService.esImagenDirecta(candidato)) return candidato;
        }
        return null;
    }

    private String construirConsultaImagen(String marca, String modelo, String especificaciones) {
        String base = String.join(" ",
                marca != null ? marca.trim() : "",
                modelo != null ? modelo.trim() : "").replaceAll("\\s+", " ").trim();
        if (especificaciones == null || especificaciones.isBlank()) return base;

        // Conserva datos que distinguen una variante (capacidad, color, conectividad), pero
        // limita la consulta para que una ficha extensa no tape el número de modelo.
        String variante = especificaciones.replaceAll("\\s+", " ").trim();
        if (variante.length() > 180) variante = variante.substring(0, 180).trim();
        return (base + " " + variante).trim();
    }

    /** Clasifica dentro del árbol de categorías todos los productos que todavía no lo tienen asignado. */
    @Transactional
    public ClasificarCategoriasResponse clasificarCategoriasFaltantes() {
        List<Producto> sinCategoria = productoRepository.findByActivo(true).stream()
                .filter(p -> p.getCategoriaId() == null)
                .collect(Collectors.toList());

        int clasificados = 0;
        for (Producto p : sinCategoria) {
            Long id = categoriaClasificadorService.clasificar(p, p.getCategoria());
            if (id != null) {
                p.setCategoriaId(id);
                productoRepository.save(p);
                clasificados++;
            }
        }

        int sinClasificar = sinCategoria.size() - clasificados;
        String mensaje = String.format(
                "Clasificación completada: %d productos clasificados, %d sin resultado (de %d procesados).",
                clasificados, sinClasificar, sinCategoria.size());
        logger.info(mensaje);
        return new ClasificarCategoriasResponse(sinCategoria.size(), clasificados, sinClasificar, mensaje);
    }
}
