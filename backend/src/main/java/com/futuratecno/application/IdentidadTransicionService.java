package com.futuratecno.application;

import com.futuratecno.application.IdentidadProductoService.Compatibilidad;
import com.futuratecno.application.IdentidadProductoService.Resolucion;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transición de los productos existentes a la identidad de la V42.
 *
 * <p>Los productos cargados antes de la V42 no tienen identidad. La carga por JSON funciona igual
 * con ellos (los resuelve en el momento y adopta al que coincide sin ambigüedad), pero conviene
 * asignarla de antemano para que el índice único los proteja. Esto NO fusiona, no da de baja y no
 * borra nada: un duplicado histórico puede tener pedidos apuntándole ({@code pedido_items
 * .producto_id}) y su foto, descripción o margen cargados a mano. Consolidarlo es una decisión del
 * admin; acá solo se informa con lo necesario para tomarla.
 *
 * <p>Solo mira productos sin {@code codigo_externo}: Elit e Invid siguen deduplicando por código.
 */
@Service
public class IdentidadTransicionService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final IdentidadProductoService identidad;
    private final CategoriaService categoriaService;
    private final JdbcTemplate jdbc;

    public IdentidadTransicionService(ProductoRepository productoRepository, VarianteRepository varianteRepository,
                                      IdentidadProductoService identidad, CategoriaService categoriaService,
                                      JdbcTemplate jdbc) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.identidad = identidad;
        this.categoriaService = categoriaService;
        this.jdbc = jdbc;
    }

    /** Un producto con su identidad calculada. */
    record Analizado(Producto producto, Resolucion resolucion) {}

    /** Resultado del análisis. Las listas de detalle vienen listas para mostrar o serializar. */
    public record Reporte(int analizados, int yaAsignados, int inequivocos,
                          List<Map<String, Object>> duplicados, List<Map<String, Object>> ambiguos,
                          List<Long> idsInequivocos) {}

    /**
     * Solo lectura. Clasifica cada producto sin identidad en:
     * <ul>
     *   <li><b>inequívoco</b>: se resuelve, nadie más del proveedor tiene esa identidad y nadie de su
     *       familia queda en conflicto con él → se le puede asignar sin riesgo;</li>
     *   <li><b>duplicado</b>: dos o más productos del proveedor resuelven a la misma identidad;</li>
     *   <li><b>ambiguo</b>: no se resuelve (faltan datos, contradicciones) o hay en su familia otro
     *       producto que no se puede afirmar igual ni distinto.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public Reporte analizar() {
        return analizar(analizarTodos());
    }

    private Reporte analizar(List<Analizado> todos) {
        Map<Long, Integer> pedidos = pedidosPorProducto();

        Map<String, List<Analizado>> porClave = new LinkedHashMap<>();
        Map<String, List<Analizado>> porFamilia = new HashMap<>();
        int yaAsignados = 0;
        for (Analizado a : todos) {
            if (a.producto().getIdentidadClave() != null) yaAsignados++;
            Long prov = a.producto().getProveedor().getId();
            if (a.resolucion().resuelta()) porClave.computeIfAbsent(prov + "|" + a.resolucion().clave(), k -> new ArrayList<>()).add(a);
            if (a.resolucion().familia() != null) porFamilia.computeIfAbsent(prov + "|" + a.resolucion().familia(), k -> new ArrayList<>()).add(a);
        }

        List<Map<String, Object>> duplicados = new ArrayList<>();
        List<Map<String, Object>> ambiguos = new ArrayList<>();
        List<Long> inequivocos = new ArrayList<>();
        for (List<Analizado> grupo : porClave.values()) {
            if (grupo.size() > 1) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("proveedorId", grupo.get(0).producto().getProveedor().getId());
                d.put("identidad", grupo.get(0).resolucion().clave());
                d.put("productos", grupo.stream().map(a -> fila(a, pedidos)).toList());
                duplicados.add(d);
            }
        }
        for (Analizado a : todos) {
            Producto p = a.producto();
            if (p.getIdentidadClave() != null) continue;
            Resolucion r = a.resolucion();
            if (!r.resuelta()) {
                Map<String, Object> f = fila(a, pedidos);
                f.put("motivos", r.motivos());
                ambiguos.add(f);
                continue;
            }
            if (porClave.get(p.getProveedor().getId() + "|" + r.clave()).size() > 1) continue;  // va en duplicados
            List<Long> enConflicto = porFamilia.getOrDefault(p.getProveedor().getId() + "|" + r.familia(), List.of()).stream()
                    .filter(o -> !o.producto().getId().equals(p.getId()))
                    .filter(o -> identidad.comparar(r, o.resolucion()) == Compatibilidad.CONFLICTO)
                    .map(o -> o.producto().getId()).toList();
            if (!enConflicto.isEmpty()) {
                Map<String, Object> f = fila(a, pedidos);
                f.put("motivos", List.of("En conflicto con los productos " + enConflicto
                        + ": no se puede afirmar si son el mismo artículo."));
                ambiguos.add(f);
            } else {
                inequivocos.add(p.getId());
            }
        }
        return new Reporte(todos.size(), yaAsignados, inequivocos.size(), duplicados, ambiguos, inequivocos);
    }

    /**
     * Asigna la identidad SOLO a los inequívocos que todavía no tienen una. Idempotente: correrlo
     * dos veces no cambia nada la segunda. La condición del UPDATE ({@code identidad_clave IS
     * NULL} y que nadie del proveedor la tenga) y el bloqueo por familia —el mismo que toma la
     * carga por JSON— evitan pisarse con una carga que esté corriendo.
     *
     * @return cuántos productos quedaron con identidad asignada
     */
    @Transactional
    public int asignarInequivocos() {
        List<Analizado> todos = analizarTodos();
        Reporte reporte = analizar(todos);
        Map<Long, Analizado> porId = new HashMap<>();
        todos.forEach(a -> porId.put(a.producto().getId(), a));
        int asignados = 0;
        // Mismo orden de bloqueo que la carga por JSON (por clave de familia): tomados en otro
        // orden, una asignación y una carga simultáneas podrían trabarse entre sí.
        List<Analizado> aAsignar = reporte.idsInequivocos().stream().map(porId::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(a -> a.resolucion().familia() != null ? a.resolucion().familia() : a.resolucion().clave()))
                .toList();
        for (Analizado a : aAsignar) {
            Long id = a.producto().getId();
            Resolucion r = a.resolucion();
            Long prov = a.producto().getProveedor().getId();
            jdbc.query("SELECT pg_advisory_xact_lock(CAST(? AS int), hashtext(?))", rs -> null,
                    prov.intValue(), r.familia() != null ? r.familia() : r.clave());
            asignados += jdbc.update("""
                    UPDATE productos SET identidad_clave = ?, identidad_version = ?, identidad_familia = ?,
                                         identidad_atributos = ?
                    WHERE id = ? AND identidad_clave IS NULL
                      AND NOT EXISTS (SELECT 1 FROM productos o WHERE o.proveedor_id = ? AND o.identidad_clave = ?)
                    """, r.clave(), r.version(), r.familia(), CargaJsonService.escribirAtributos(r.atributos()),
                    id, prov, r.clave());
        }
        return asignados;
    }

    /** Qué pasó con la identidad al editar un producto a mano. */
    public enum ResultadoEdicion { NO_APLICA, SIN_CAMBIO, ASIGNADA, CONSERVADA }

    /** Otro producto del mismo proveedor ya tiene la identidad que resultaría de la edición. */
    public static class IdentidadEnUsoException extends IllegalStateException {
        public IdentidadEnUsoException(String mensaje) { super(mensaje); }
    }

    /**
     * Recalcula la identidad después de que el admin editó marca, modelo o especificaciones. Sin
     * esto, completar un dato a mano (el color que faltaba) no cambiaba nada: la carga compara
     * contra la identidad GUARDADA, no contra el texto, y el artículo seguía yendo a revisión.
     *
     * <p>Dos resguardos. Si otro producto del proveedor ya tiene la identidad nueva, se lanza
     * {@link IdentidadEnUsoException} y la edición entera vuelve atrás: serían el mismo artículo
     * publicado dos veces. Y si el texto editado pierde un atributo que la identidad guardada tenía
     * (la carga la pudo resolver con especificaciones estructuradas que el texto no refleja entero),
     * se conserva la guardada: una edición menor no debe degradarla.
     */
    @Transactional
    public ResultadoEdicion recalcularTrasEdicion(Producto p, String especificaciones) {
        if (p.getCodigoExterno() != null && !p.getCodigoExterno().isBlank()) return ResultadoEdicion.NO_APLICA;
        Resolucion nueva = identidad.resolverGuardado(p.getMarca(), p.getModelo(), especificaciones, categoria(p.getCategoriaId()));
        if (!nueva.resuelta()) return p.getIdentidadClave() != null ? ResultadoEdicion.CONSERVADA : ResultadoEdicion.NO_APLICA;
        if (nueva.clave().equals(p.getIdentidadClave())) return ResultadoEdicion.SIN_CAMBIO;
        if (p.getIdentidadClave() != null && pierdeAtributos(nueva, CargaJsonService.leerAtributos(p.getIdentidadAtributos()))) {
            return ResultadoEdicion.CONSERVADA;
        }
        Long prov = p.getProveedor().getId();
        // Mismo bloqueo que la carga por JSON y la asignación: sin él, una carga simultánea podría
        // tomar la misma identidad entre la consulta y el UPDATE.
        jdbc.query("SELECT pg_advisory_xact_lock(CAST(? AS int), hashtext(?))", rs -> null,
                prov.intValue(), nueva.familia() != null ? nueva.familia() : nueva.clave());
        List<Long> duenios = jdbc.queryForList(
                "SELECT id FROM productos WHERE proveedor_id = ? AND identidad_clave = ? AND id <> ? ORDER BY id",
                Long.class, prov, nueva.clave(), p.getId());
        if (!duenios.isEmpty()) {
            throw new IdentidadEnUsoException("Con estos datos, el producto sería el mismo artículo que el " + duenios
                    + " de este proveedor (" + nueva.clave() + "). No se guardó nada: revisá cuál de los dos corresponde.");
        }
        p.setIdentidadClave(nueva.clave());
        p.setIdentidadVersion(nueva.version());
        p.setIdentidadFamilia(nueva.familia());
        p.setIdentidadAtributos(CargaJsonService.escribirAtributos(nueva.atributos()));
        productoRepository.save(p);
        varianteRepository.findByProductoIdIn(List.of(p.getId()))
                .forEach(v -> { CargaJsonService.proyectarAtributos(v, nueva); varianteRepository.save(v); });
        return ResultadoEdicion.ASIGNADA;
    }

    /** ¿La guardada conocía algún atributo del teléfono que la nueva ya no conoce? */
    static boolean pierdeAtributos(Resolucion nueva, Map<String, String> guardados) {
        if (!nueva.esTelefono()) return false;
        return IdentidadProductoService.CAMPOS_TELEFONO.stream()
                .anyMatch(c -> guardados.get(c) != null && nueva.atributos().get(c) == null);
    }

    private List<Analizado> analizarTodos() {
        List<Producto> productos = productoRepository.findAll().stream()
                .filter(p -> p.getCodigoExterno() == null || p.getCodigoExterno().isBlank())
                .sorted(Comparator.comparing(Producto::getId))
                .toList();
        Map<Long, String> specs = new HashMap<>();
        List<Long> ids = productos.stream().map(Producto::getId).toList();
        for (int desde = 0; desde < ids.size(); desde += 500) {
            varianteRepository.findByProductoIdIn(ids.subList(desde, Math.min(desde + 500, ids.size()))).stream()
                    .sorted(Comparator.comparing(Variante::getActivo).thenComparing(Variante::getUpdatedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .forEach(v -> specs.put(v.getProducto().getId(), v.getEspecificaciones()));
        }
        List<Analizado> out = new ArrayList<>();
        for (Producto p : productos) {
            Resolucion r = p.getIdentidadClave() != null
                    ? new Resolucion(IdentidadProductoService.Estado.RESUELTA, p.getIdentidadVersion(), p.getIdentidadClave(),
                            p.getIdentidadFamilia(), CargaJsonService.leerAtributos(p.getIdentidadAtributos()), List.of(), false)
                    : identidad.resolverGuardado(p.getMarca(), p.getModelo(), specs.get(p.getId()), categoria(p.getCategoriaId()));
            out.add(new Analizado(p, r));
        }
        return out;
    }

    private String categoria(Long categoriaId) {
        var n = categoriaService.resolverNombres(categoriaId);
        if (n == null) return null;
        return n.getCategoriaPadre() != null ? n.getCategoriaPadre() + " > " + n.getSubcategoria() : n.getSubcategoria();
    }

    private Map<Long, Integer> pedidosPorProducto() {
        Map<Long, Integer> m = new HashMap<>();
        jdbc.query("SELECT producto_id, count(*) n FROM pedido_items WHERE producto_id IS NOT NULL GROUP BY producto_id",
                rs -> { m.put(rs.getLong("producto_id"), rs.getInt("n")); });
        return m;
    }

    private static Map<String, Object> fila(Analizado a, Map<Long, Integer> pedidos) {
        Producto p = a.producto();
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("id", p.getId());
        f.put("proveedorId", p.getProveedor().getId());
        f.put("marca", p.getMarca());
        f.put("modelo", p.getModelo());
        f.put("activo", p.getActivo());
        f.put("lineasDePedido", pedidos.getOrDefault(p.getId(), 0));
        f.put("identidadAsignada", p.getIdentidadClave());
        return f;
    }
}
