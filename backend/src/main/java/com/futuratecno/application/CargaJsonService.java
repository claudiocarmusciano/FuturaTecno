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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.application.IdentidadProductoService.Resolucion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Carga manual de productos a partir de un JSON (sección admin "Cargar artículos por JSON").
 * Emula el guardado del parsing pero además: carga imágenes (galería) e intenta clasificar la
 * categoría automáticamente. Precio siempre en USD ({@code precio_usd}).
 *
 * <p>Qué producto es cada artículo lo decide la identidad (V42, {@link IdentidadProductoService}),
 * no el nombre: el mismo teléfono redactado distinto actualiza el mismo producto, y variantes
 * distintas (64GB/128GB, 4GB/8GB de RAM, G04/G04s) nunca se pisan. Lo dudoso queda "revision" en
 * la respuesta, sin crear ni actualizar nada.
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
    private final MargenManualService margenManualService;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final ImagenRepository imagenRepository;
    private final CategoriaClasificadorService categoriaClasificadorService;
    private final CategoriaService categoriaService;
    private final ImageUrlValidatorService imageUrlValidatorService;
    private final IdentidadProductoService identidad;
    private final JdbcTemplate jdbc;

    public CargaJsonService(ImagenManualService imagenManualService,
                            DescripcionManualService descripcionManualService,
                            AtributosManualService atributosManualService,
                            MargenManualService margenManualService,
                            ProductoRepository productoRepository,
                            VarianteRepository varianteRepository,
                            ProveedorRepository proveedorRepository,
                            ImagenRepository imagenRepository,
                            CategoriaClasificadorService categoriaClasificadorService,
                            CategoriaService categoriaService,
                            ImageUrlValidatorService imageUrlValidatorService,
                            IdentidadProductoService identidad,
                            JdbcTemplate jdbc) {
        this.imagenManualService = imagenManualService;
        this.descripcionManualService = descripcionManualService;
        this.atributosManualService = atributosManualService;
        this.margenManualService = margenManualService;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.proveedorRepository = proveedorRepository;
        this.imagenRepository = imagenRepository;
        this.categoriaClasificadorService = categoriaClasificadorService;
        this.categoriaService = categoriaService;
        this.imageUrlValidatorService = imageUrlValidatorService;
        this.identidad = identidad;
        this.jdbc = jdbc;
    }

    @Transactional
    public CargaJsonResponse cargar(Long proveedorId, List<ArticuloJsonDTO> articulos) {
        return cargar(proveedorId, articulos, List.of());
    }

    /** Reemplazo explícito y acotado: todo el lote o nada; conserva los registros históricos. */
    @Transactional
    public CargaJsonResponse cargar(Long proveedorId, List<ArticuloJsonDTO> articulos, List<Long> reemplazarIds) {
        var excluidos = new LinkedHashSet<Long>();
        if (reemplazarIds != null) {
            if (reemplazarIds.size() > 50 || reemplazarIds.stream().anyMatch(id -> id == null || id <= 0))
                throw new IllegalArgumentException("IDs de reemplazo inválidos (máximo 50).");
            excluidos.addAll(reemplazarIds);
        }
        boolean reemplazo = !excluidos.isEmpty();
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + proveedorId));

        CargaJsonResponse res = new CargaJsonResponse();
        int creados = 0, actualizados = 0, omitidos = 0, sinCategoria = 0, revision = 0;
        List<ArticuloJsonDTO> lista = articulos != null ? articulos : List.of();

        // 1) Resolver la identidad de todo el lote antes de tocar la base.
        List<Resolucion> resoluciones = new ArrayList<>();
        for (ArticuloJsonDTO art : lista) {
            String marca = limpiar(art.getMarca());
            String modelo = derivarModelo(art, marca);
            resoluciones.add(marca == null || modelo == null ? null
                    : identidad.resolver(marca, modelo, especificacionesParaIdentidad(art, modelo), limpiar(art.getCategoria())));
        }

        // 2) Bloquear las familias del lote, siempre en el mismo orden. Con esto, dos cargas
        // simultáneas del mismo artículo no pueden consultar las dos "no existe" y crear dos filas:
        // la segunda espera a que la primera confirme y después la encuentra. El orden fijo evita
        // que dos lotes con los mismos artículos en otro orden se traben entre sí. El índice único
        // de la V42 queda como red de seguridad.
        bloquearFamilias(proveedorId, resoluciones);
        List<Producto> anteriores = new ArrayList<>();
        if (reemplazo) {
            if (lista.isEmpty() || resoluciones.stream().anyMatch(r -> r == null || !r.resuelta())
                    || lista.stream().anyMatch(a -> a.getImagenes() != null && !a.getImagenes().isEmpty()))
                throw new IllegalArgumentException("Reemplazo requiere identidades resueltas e imágenes vacías.");
            var claves = resoluciones.stream().map(Resolucion::clave).collect(java.util.stream.Collectors.toSet());
            if (claves.size() != lista.size()) throw new IllegalArgumentException("Variantes duplicadas en el reemplazo.");
            var familias = resoluciones.stream().map(Resolucion::familia).collect(java.util.stream.Collectors.toSet());
            String placeholders = String.join(",", java.util.Collections.nCopies(excluidos.size(), "?"));
            jdbc.queryForList("SELECT id FROM productos WHERE id IN (" + placeholders + ") ORDER BY id FOR UPDATE",
                    Long.class, excluidos.toArray());
            anteriores = productoRepository.findAllById(excluidos);
            if (anteriores.size() != excluidos.size()) throw new IllegalArgumentException("No existen todos los productos a reemplazar.");
            for (Producto anterior : anteriores) {
                if (anterior.getProveedor() == null || !proveedorId.equals(anterior.getProveedor().getId())
                        || anterior.getCodigoExterno() != null || !"JSON".equals(anterior.getFuente()))
                    throw new IllegalArgumentException("El reemplazo solo admite productos JSON del mismo proveedor.");
                String specs = varianteRepository.findByProductoIdAndActivo(anterior.getId(), true).stream()
                        .min(Comparator.comparing(Variante::getId)).map(Variante::getEspecificaciones).orElse(null);
                Resolucion previa = resolucionGuardada(anterior, specs);
                if (!familias.contains(previa.familia()) || claves.contains(previa.clave()))
                    throw new IllegalArgumentException("El producto " + anterior.getId() + " no es un agrupado de las familias del lote.");
            }
        }

        Map<String, Integer> vistasEnLote = new HashMap<>();
        Map<Long, Resolucion> cacheGuardadas = new HashMap<>();
        Map<String, List<Producto>> cacheMarcas = new HashMap<>();

        for (int i = 0; i < lista.size(); i++) {
            ArticuloJsonDTO art = lista.get(i);
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

            Resolucion r = resoluciones.get(i);
            Decision decision;
            Integer filaPrevia = r.resuelta() ? vistasEnLote.putIfAbsent(r.clave(), i + 1) : null;
            if (filaPrevia != null) {
                decision = Decision.revision(List.of("El mismo artículo ya aparece en la fila " + filaPrevia
                        + " de esta carga: no se actualiza dos veces con precios que pueden ser distintos."), List.of());
            } else {
                decision = decidir(identidad, r, candidatos(proveedorId, marca, modelo, r, cacheGuardadas, cacheMarcas).stream()
                        .filter(c -> !excluidos.contains(c.producto().getId())).toList());
            }

            if (decision.accion() == Accion.REVISION) {
                revision++;
                CargaJsonResponse.Item item = new CargaJsonResponse.Item(marca + " " + modelo, "revision", null,
                        String.join(" ", decision.motivos()));
                item.setIdentidad(r.clave());
                item.setCandidatos(decision.relacionados());
                res.getItems().add(item);
                continue;
            }

            boolean nuevo = decision.accion() == Accion.CREAR;
            Producto producto = nuevo ? new Producto() : decision.producto();
            if (nuevo) {
                producto.setProveedor(proveedor);
                producto.setMarca(marca);
                producto.setModelo(nombreAlCrear(modelo, r));
                producto.setFuente(FUENTE);
            }
            // El nombre visible de un producto existente NO se pisa con la redacción de esta carga:
            // la identidad decide que es el mismo artículo, el nombre lo sigue eligiendo el admin.
            asignarIdentidad(producto, r);
            // Una nueva carga manual debe volver a publicar un producto que se había dado de baja.
            // De lo contrario figura como "actualizado" pero continúa oculto del panel y catálogo.
            producto.setActivo(true);
            producto.setCategoria(limpiar(art.getCategoria()));

            // Nombres con los que se conoce este artículo, para las memorias manuales: el guardado
            // primero (es con el que el admin las editó), después el de esta carga y después el de
            // otros productos con la misma identidad (el mismo artículo en otro proveedor).
            List<NombreArticulo> nombres = nombresDelArticulo(producto, marca, modelo, r);

            Optional<String> recordada = reemplazo ? Optional.empty() : imagenManualService.buscar(nombres);
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
                // jamás pisa una que el admin haya elegido a mano. Se guarda con el nombre del
                // producto, que es con el que la buscan el admin y las próximas cargas.
                if (recordada.isEmpty()) imagenManualService.guardarAutomatica(producto.getMarca(), producto.getModelo(), imagenes.get(0));
            }

            // Categoría y medidas ya resueltas para este artículo, aunque haya sido con otro
            // proveedor o con otra redacción. Solo rellena huecos: lo que la fila ya tenga manda.
            atributosManualService.aplicar(producto, nombres);
            // Margen y flete recordados para este artículo EN ESTE PROVEEDOR (V41).
            margenManualService.aplicar(producto, nombres);

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
            // saveAndFlush: las consultas de candidatos del resto del lote tienen que verlo.
            final Producto prod = productoRepository.saveAndFlush(producto);

            // Variante con el precio (siempre USD) y las specs.
            // La descripción curada a mano gana sobre la del JSON, igual que la imagen: si alguien
            // ya la corrigió, un reimport no debe pisarla con el texto crudo del proveedor.
            String especificaciones = reemplazo ? construirEspecificaciones(art) : descripcionManualService.buscar(nombres)
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
            proyectarAtributos(variante, r);
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
            CargaJsonResponse.Item item = new CargaJsonResponse.Item(
                    marca + " " + modelo, nuevo ? "creado" : "actualizado", categoriaPath,
                    fotoRota ? "La imagen del JSON no responde. Quedó sin foto: cargala desde Admin → Imágenes." : null);
            item.setProductoId(prod.getId());
            item.setIdentidad(r.clave());
            if (!decision.motivos().isEmpty()) item.setAviso(String.join(" ", decision.motivos()));
            res.getItems().add(item);
        }

        res.setCreados(creados);
        res.setActualizados(actualizados);
        res.setOmitidos(omitidos);
        res.setSinCategoria(sinCategoria);
        if (reemplazo) {
            if (revision != 0 || omitidos != 0 || creados + actualizados != lista.size()) {
                // Sin los motivos, quien llama (n8n) no tiene cómo saber qué artículo frenó el lote:
                // la transacción se revierte y la respuesta por ítem se pierde.
                String detalle = res.getItems().stream()
                        .filter(i -> !"creado".equals(i.getEstado()) && !"actualizado".equals(i.getEstado()))
                        .limit(5)
                        .map(i -> i.getProducto() + " (" + i.getEstado() + "): " + i.getMotivo())
                        .collect(java.util.stream.Collectors.joining("; "));
                throw new IllegalArgumentException("Reemplazo cancelado: hay artículos omitidos o en revisión. "
                        + "No se cambió ningún producto. " + detalle);
            }
            // Baja lógica al final, en la misma transacción: no borra pedidos, variantes ni fotos históricas.
            for (Producto anterior : anteriores) {
                anterior.setActivo(false);
                productoRepository.save(anterior);
                for (Variante v : varianteRepository.findByProductoIdAndActivo(anterior.getId(), true)) {
                    v.setActivo(false);
                    varianteRepository.save(v);
                }
            }
        }
        res.setRevision(revision);
        res.setMensaje(String.format(
                "Carga completada: %d creados, %d actualizados, %d omitidos, %d para revisar. %d quedaron sin categoría (asignar a mano).%s",
                creados, actualizados, omitidos, revision, sinCategoria,
                reemplazo ? " Reemplazo: dados de baja los productos anteriores " + excluidos + "." : ""));
        logger.info(res.getMensaje());
        return res;
    }

    // ------------------------------------------------------------------ identidad

    /** Vista previa para el borrador: la identidad de cada artículo, en el mismo orden. No toca la base. */
    public List<Map<String, Object>> identidades(List<ArticuloJsonDTO> articulos) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ArticuloJsonDTO art : articulos) {
            String marca = limpiar(art.getMarca());
            String modelo = derivarModelo(art, marca);
            Map<String, Object> fila = new LinkedHashMap<>();
            if (marca == null || modelo == null) {
                fila.put("estado", "omitido");
                fila.put("identidad", null);
                fila.put("motivos", List.of("Falta marca o modelo."));
            } else {
                Resolucion r = identidad.resolver(marca, modelo, especificacionesParaIdentidad(art, modelo), limpiar(art.getCategoria()));
                fila.put("estado", r.resuelta() ? "resuelta" : "revision");
                fila.put("identidad", r.clave());
                fila.put("version", r.version());
                fila.put("motivos", r.motivos());
            }
            out.add(fila);
        }
        return out;
    }

    enum Accion { CREAR, ACTUALIZAR, REVISION }

    /** Un producto existente con la identidad con la que se lo compara. */
    record Candidato(Producto producto, Resolucion resolucion) {}

    /**
     * @param producto     el producto a actualizar (solo en ACTUALIZAR)
     * @param motivos      por qué va a revisión; en ACTUALIZAR, avisos que no impiden la carga
     * @param relacionados ids de los productos existentes que motivaron la decisión
     */
    record Decision(Accion accion, Producto producto, List<String> motivos, List<Long> relacionados) {
        static Decision revision(List<String> motivos, List<Long> relacionados) {
            return new Decision(Accion.REVISION, null, motivos, relacionados);
        }
    }

    /**
     * Qué hacer con un artículo entrante, comparándolo con los productos del proveedor que
     * podrían ser el mismo. No hay atajos: el nombre exacto y la clave suelta traen candidatos,
     * pero lo que decide es la comparación de atributos.
     * <ul>
     *   <li>Un producto que ya tiene esta identidad asignada es el dueño: se actualiza.</li>
     *   <li>Si no, un único producto que resuelve a la misma identidad se adopta y se actualiza.</li>
     *   <li>Dos o más que resuelven igual son duplicados históricos: revisión, no se elige uno.</li>
     *   <li>Un candidato que no se puede afirmar igual ni distinto (le falta un dato que el
     *       entrante tiene, o al revés): revisión.</li>
     *   <li>Nada de lo anterior: se crea.</li>
     * </ul>
     */
    static Decision decidir(IdentidadProductoService svc, Resolucion entrante, List<Candidato> candidatos) {
        if (!entrante.resuelta()) {
            List<Long> familia = candidatos.stream()
                    .filter(c -> entrante.familia() != null && entrante.familia().equals(c.resolucion().familia()))
                    .map(c -> c.producto().getId()).toList();
            return Decision.revision(entrante.motivos(), familia);
        }
        Candidato duenio = null;
        List<Candidato> iguales = new ArrayList<>();
        List<Candidato> conflictos = new ArrayList<>();
        for (Candidato c : candidatos) {
            if (entrante.clave().equals(c.producto().getIdentidadClave())) duenio = c;
            switch (svc.comparar(entrante, c.resolucion())) {
                case IGUAL -> iguales.add(c);
                case CONFLICTO -> conflictos.add(c);
                case DISTINTA -> { }
            }
        }
        if (duenio != null && !Boolean.TRUE.equals(duenio.producto().getActivo())
                && conflictos.stream().anyMatch(c -> Boolean.TRUE.equals(c.producto().getActivo()))) {
            return Decision.revision(List.of("El producto anterior está retirado y hay variantes activas más específicas. No se reactiva el agrupado."), ids(conflictos));
        }
        if (duenio != null) {
            Long idDuenio = duenio.producto().getId();
            List<Long> otros = iguales.stream().map(c -> c.producto().getId()).filter(id -> !id.equals(idDuenio)).toList();
            List<String> avisos = otros.isEmpty() ? List.of() : List.of("Hay otros productos con esta misma identidad "
                    + "(ids " + otros + "): posibles duplicados históricos, revisalos en Admin → Identidad.");
            return new Decision(Accion.ACTUALIZAR, duenio.producto(), avisos, otros);
        }
        if (iguales.size() > 1) {
            // Duplicados históricos. Si el admin ya los resolvió dando de baja a todos menos uno
            // (así se consolidaron el 18/9/2026), el activo es el elegido: se actualiza ese. Con
            // varios activos, o ninguno, no hay a quién elegir sin adivinar.
            List<Candidato> activos = iguales.stream().filter(c -> Boolean.TRUE.equals(c.producto().getActivo())).toList();
            if (activos.size() == 1) {
                List<Long> inactivos = iguales.stream().filter(c -> c != activos.get(0)).map(c -> c.producto().getId()).toList();
                return new Decision(Accion.ACTUALIZAR, activos.get(0).producto(), List.of("Se actualizó el producto "
                        + "activo; hay copias dadas de baja del mismo artículo (ids " + inactivos + ")."), inactivos);
            }
            return Decision.revision(List.of("Hay " + iguales.size() + " productos existentes que son este mismo "
                    + "artículo (ids " + ids(iguales) + ") y " + (activos.isEmpty() ? "ninguno está activo" : activos.size() + " están activos")
                    + ": duplicados históricos. Consolidalos (Admin → Identidad) antes de recargar."), ids(iguales));
        }
        if (iguales.size() == 1) {
            List<String> avisos = conflictos.isEmpty() ? List.of() : List.of("Otros productos parecidos no se pudieron "
                    + "comparar del todo (ids " + ids(conflictos) + "); revisá que no sean el mismo artículo.");
            return new Decision(Accion.ACTUALIZAR, iguales.get(0).producto(), avisos, ids(conflictos));
        }
        if (!conflictos.isEmpty()) {
            return Decision.revision(List.of("Podría ser el mismo artículo que los productos " + ids(conflictos)
                    + ", pero a uno de los dos le falta un dato que distingue variantes (conectividad, SIM, color, "
                    + "región…) o sus datos no se pueden leer. Completá el JSON o el producto y recargá."), ids(conflictos));
        }
        return new Decision(Accion.CREAR, null, List.of(), List.of());
    }

    private static List<Long> ids(List<Candidato> cs) {
        return cs.stream().map(c -> c.producto().getId()).toList();
    }

    /**
     * Productos del proveedor que podrían ser este artículo, cada uno con su identidad: la
     * guardada si ya tiene una, o resuelta ahora desde su nombre y sus especificaciones si es
     * anterior a la V42. Para teléfonos se miran todos los de la marca, porque un producto viejo
     * redactado distinto no comparte ni la clave suelta.
     */
    private List<Candidato> candidatos(Long proveedorId, String marca, String modelo, Resolucion r,
                                       Map<Long, Resolucion> cacheGuardadas, Map<String, List<Producto>> cacheMarcas) {
        Map<Long, Producto> porId = new LinkedHashMap<>();
        productoRepository.candidatosDeIdentidad(proveedorId, r.clave(), r.familia(),
                        ImagenManualService.clave(marca, modelo), normalizar(marca), normalizar(modelo))
                .forEach(p -> porId.put(p.getId(), p));
        if (r.esTelefono() && r.atributos().get("marca") != null) {
            String m = r.atributos().get("marca");
            List<String> marcas = m.equals("xiaomi") ? List.of("xiaomi", "redmi", "poco") : List.of(m);
            cacheMarcas.computeIfAbsent(proveedorId + "|" + m, k -> productoRepository.deMarcasEnProveedor(proveedorId, marcas))
                    .forEach(p -> porId.putIfAbsent(p.getId(), p));
        }
        if (porId.isEmpty()) return List.of();

        List<Long> sinResolver = porId.keySet().stream().filter(id -> !cacheGuardadas.containsKey(id)).toList();
        Map<Long, String> specs = new HashMap<>();
        if (!sinResolver.isEmpty()) {
            varianteRepository.findByProductoIdIn(sinResolver).stream()
                    .sorted(Comparator.comparing(Variante::getActivo).thenComparing(Variante::getUpdatedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .forEach(v -> specs.put(v.getProducto().getId(), v.getEspecificaciones()));
        }
        List<Candidato> out = new ArrayList<>();
        for (Producto p : porId.values()) {
            Resolucion rp = cacheGuardadas.computeIfAbsent(p.getId(), id -> resolucionGuardada(p, specs.get(id)));
            // De la búsqueda por marca solo interesan los de la misma familia; lo demás de la marca
            // (otro modelo, un televisor) no es candidato.
            boolean porNombre = Objects.equals(p.getIdentidadClave(), r.clave())
                    || Objects.equals(r.familia(), rp.familia())
                    || ImagenManualService.clave(p.getMarca(), p.getModelo()).equals(ImagenManualService.clave(marca, modelo));
            if (porNombre) out.add(new Candidato(p, rp));
        }
        return out;
    }

    private Resolucion resolucionGuardada(Producto p, String especificaciones) {
        if (p.getIdentidadClave() != null && p.getIdentidadVersion() != null) {
            return new Resolucion(IdentidadProductoService.Estado.RESUELTA, p.getIdentidadVersion(), p.getIdentidadClave(),
                    p.getIdentidadFamilia(), leerAtributos(p.getIdentidadAtributos()), List.of(), false);
        }
        return identidad.resolverGuardado(p.getMarca(), p.getModelo(), especificaciones, pathDe(p.getCategoriaId()));
    }

    private void asignarIdentidad(Producto p, Resolucion r) {
        p.setIdentidadClave(r.clave());
        p.setIdentidadVersion(r.version());
        p.setIdentidadFamilia(r.familia());
        p.setIdentidadAtributos(escribirAtributos(r.atributos()));
    }

    /**
     * Los campos estructurados de la variante (ramGb, almacenamientoGb, color) existían desde la V1
     * sin que nadie los escribiera. Se llenan como PROYECCIÓN de la identidad del producto, en el
     * mismo lugar y con los mismos valores: la fuente de verdad es {@code productos.identidad_*} y
     * estos campos no se usan para decidir nada, así que no pueden contradecirla.
     */
    static void proyectarAtributos(Variante v, Resolucion r) {
        if (!r.esTelefono()) return;
        v.setAlmacenamientoGb(entero(r.atributos().get("almacenamiento_gb")));
        v.setRamGb(entero(r.atributos().get("ram_gb")));
        v.setColor(r.atributos().get("color"));
    }

    private List<NombreArticulo> nombresDelArticulo(Producto producto, String marca, String modelo, Resolucion r) {
        List<NombreArticulo> nombres = new ArrayList<>();
        nombres.add(new NombreArticulo(producto.getMarca(), producto.getModelo()));
        nombres.add(new NombreArticulo(marca, modelo));
        if (r.clave() != null) {
            for (Object[] fila : productoRepository.nombresPorIdentidad(r.clave())) {
                nombres.add(new NombreArticulo((String) fila[0], (String) fila[1]));
            }
        }
        return NombreArticulo.distintos(nombres);
    }

    private void bloquearFamilias(Long proveedorId, List<Resolucion> resoluciones) {
        new TreeSet<>(resoluciones.stream().filter(Objects::nonNull)
                .map(r -> r.familia() != null ? r.familia() : r.clave())
                .filter(Objects::nonNull).toList())
                .forEach(k -> jdbc.query("SELECT pg_advisory_xact_lock(CAST(? AS int), hashtext(?))",
                        rs -> null, proveedorId.intValue(), k));
    }

    private static Integer entero(String s) {
        try { return s == null ? null : Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    static String escribirAtributos(Map<String, String> atributos) {
        try { return JSON.writeValueAsString(atributos); } catch (Exception e) { return null; }
    }

    static Map<String, String> leerAtributos(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return JSON.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Modelo "limpio": usa `modelo` si vino; si no, la primera parte de `modelo_exacto` (o `listado`), sin el prefijo de marca. */
    private String derivarModelo(ArticuloJsonDTO art, String marca) {
        if (limpiar(art.getModelo()) != null) return sinMarcaRepetida(limpiar(art.getModelo()), marca);
        String base = art.getModeloExacto() != null ? art.getModeloExacto()
                : (art.getListado() != null ? art.getListado() : null);
        if (base == null) return null;
        base = base.trim();
        int coma = base.indexOf(',');
        if (coma > 0) base = base.substring(0, coma).trim();
        // "ASUS 15.6\" ... – USD 320" (listado): cortar en un guión largo de precio si quedó
        int guion = base.indexOf('–');
        if (guion > 0) base = base.substring(0, guion).trim();
        base = sinMarcaRepetida(base, marca);
        if (base.length() > 200) base = base.substring(0, 200).trim();
        return base.isEmpty() ? null : base;
    }

    /**
     * "Motorola" + "Motorola G04 4G 64GB" → "G04 4G 64GB". El catálogo muestra marca + modelo, así
     * que la marca repetida se veía dos veces ("Motorola Motorola G04"). Pasa con cualquier fuente
     * del JSON, no solo con el generador. Si el modelo es solo la marca, se deja como está.
     */
    static String sinMarcaRepetida(String modelo, String marca) {
        if (modelo == null || marca == null || marca.isBlank()) return modelo;
        String m = modelo.strip(), mc = marca.strip();
        if (m.length() > mc.length() && m.regionMatches(true, 0, mc, 0, mc.length())
                && !Character.isLetterOrDigit(m.charAt(mc.length()))) {
            String resto = m.substring(mc.length()).replaceFirst("^[\\s\\-–:,]+", "").strip();
            if (!resto.isEmpty()) return resto;
        }
        return m;
    }

    /**
     * Especificaciones para resolver la identidad. Si el JSON trae además {@code modelo_exacto}
     * (la redacción completa del listado) y dice algo que {@code modelo} no, se lee como una fuente
     * más: "Motorola G04 4G 64GB / 4GB RAM (Green)" aporta el color. Si contradice al modelo, la
     * identidad lo detecta y el artículo va a revisión.
     */
    private Map<String, Object> especificacionesParaIdentidad(ArticuloJsonDTO art, String modelo) {
        String exacto = limpiar(art.getModeloExacto());
        if (exacto == null || exacto.equalsIgnoreCase(modelo)) return art.getEspecificaciones();
        Map<String, Object> m = new LinkedHashMap<>();
        if (art.getEspecificaciones() != null) m.putAll(art.getEspecificaciones());
        m.put("modelo_exacto", exacto);
        return m;
    }

    /**
     * Nombre con el que se crea un producto nuevo. Si su color se supo por las especificaciones o
     * por {@code modelo_exacto} pero no está en el modelo, se agrega: dos colores del mismo
     * teléfono son dos productos, y en la tienda no pueden verse con el mismo nombre.
     */
    static String nombreAlCrear(String modelo, Resolucion r) {
        if (!r.esTelefono() || !"especificaciones".equals(r.atributos().get("fuente.color"))) return modelo;
        String color = IdentidadProductoService.nombreVisibleColor(r.atributos().get("color"));
        return color == null ? modelo : modelo + " " + color;
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
