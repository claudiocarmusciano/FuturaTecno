package com.futuratecno.application;

import com.futuratecno.api.dto.CategoriaAdminDTO;
import com.futuratecno.api.dto.CategoriaNombresDTO;
import com.futuratecno.api.dto.CategoriaTreeDTO;
import com.futuratecno.domain.Categoria;
import com.futuratecno.infrastructure.CategoriaRepository;
import com.futuratecno.infrastructure.ProductoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El árbol de categorías se cachea entero en memoria: lo consulta cada request de catálogo
 * (resolverNombres) y recorrer todas las filas cada vez sería un desperdicio.
 *
 * <p>El árbol **ya no es fijo** — se puede crear, renombrar, mover y borrar desde el admin — así
 * que toda mutación tiene que llamar a {@link #cargar()} para refrescar el caché; si no, el cambio
 * no se ve hasta reiniciar. Los mapas se arman en variables locales y recién al final se asignan,
 * para que un request concurrente nunca lea una estructura a medio construir.
 *
 * <p>La profundidad no está fijada en el código: una hoja puede ser una categoría de primer nivel
 * sin hijos (ej. "Tablets") o una subcategoría (ej. "Almacenamiento > Pen Drive"). El admin sí
 * limita el alta a dos niveles, que es como está armado el árbol hoy.
 */
@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    private volatile Map<Long, Categoria> porId;
    private volatile Map<Long, List<Categoria>> hijosDe;
    /** Path completo ("Categoría > Subcategoría", o solo "Categoría" si no tiene hijos) -> id de la hoja. */
    private volatile Map<String, Long> idPorPath;
    /**
     * Nombre de hoja (sin el resto del path) -> id, solo para nombres que no se repiten en
     * ninguna otra hoja. Sirve de último recurso cuando un mayorista informa una jerarquía más
     * profunda que la nuestra (ej. Invid manda "DDR3" bajo "Memoria Sodimm", pero en nuestro
     * árbol "Memoria Sodimm" ya es la hoja) — redondea hacia el ancestro más cercano que sí existe.
     */
    private volatile Map<String, Long> idPorNombreDeHojaUnico;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @PostConstruct
    final void cargar() {
        List<Categoria> todas = categoriaRepository.findAll();

        Map<Long, Categoria> nuevoPorId = todas.stream()
                .collect(Collectors.toMap(Categoria::getId, c -> c));
        Map<Long, List<Categoria>> nuevoHijosDe = new HashMap<>();
        for (Categoria c : todas) {
            Long padreId = c.getPadre() != null ? c.getPadre().getId() : null;
            nuevoHijosDe.computeIfAbsent(padreId, k -> new ArrayList<>()).add(c);
        }
        Map<String, Long> nuevoIdPorPath = new HashMap<>();
        Map<String, Long> nuevoIdPorNombre = new HashMap<>();
        Map<String, Integer> ocurrenciasNombre = new HashMap<>();
        for (Categoria c : todas) {
            List<Categoria> hijos = nuevoHijosDe.get(c.getId());
            if (hijos != null && !hijos.isEmpty()) continue;   // no es hoja
            nuevoIdPorPath.put(normalizarPath(pathDe(c)), c.getId());
            String nombreNorm = normalizarPath(c.getNombre());
            ocurrenciasNombre.merge(nombreNorm, 1, Integer::sum);
            nuevoIdPorNombre.put(nombreNorm, c.getId());
        }
        // Si el mismo nombre de hoja aparece en más de una rama, no adivinamos: se saca del índice.
        ocurrenciasNombre.forEach((nombre, veces) -> {
            if (veces > 1) nuevoIdPorNombre.remove(nombre);
        });

        // Recién acá se publican, ya completos.
        porId = nuevoPorId;
        hijosDe = nuevoHijosDe;
        idPorPath = nuevoIdPorPath;
        idPorNombreDeHojaUnico = nuevoIdPorNombre;
    }

    /** Mayúsculas + espacios colapsados, para que el matching de paths no dependa de mayúsculas/espacios exactos. */
    private String normalizarPath(String path) {
        return path.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private boolean esHoja(Categoria c) {
        List<Categoria> hijos = hijosDe.get(c.getId());
        return hijos == null || hijos.isEmpty();
    }

    /** Camino desde la raíz hasta este nodo, uniendo nombres con " > " (recursivo, sin asumir una profundidad fija). */
    private String pathDe(Categoria nodo) {
        if (nodo.getPadre() == null) return nodo.getNombre();
        return pathDe(nodo.getPadre()) + " > " + nodo.getNombre();
    }

    public List<CategoriaTreeDTO> obtenerArbol() {
        return construirNivel(null);
    }

    private List<CategoriaTreeDTO> construirNivel(Long padreId) {
        List<Categoria> hijos = hijosDe.getOrDefault(padreId, List.of());
        List<CategoriaTreeDTO> out = new ArrayList<>();
        for (Categoria c : hijos) {
            out.add(new CategoriaTreeDTO(c.getId(), c.getNombre(), construirNivel(c.getId())));
        }
        return out;
    }

    /**
     * Nombres de categoría/subcategoría para una hoja. Si la hoja es una categoría de primer
     * nivel sin hijos (ej. "Tablets"), categoriaPadre viene null. Null si categoriaId no existe.
     */
    public CategoriaNombresDTO resolverNombres(Long categoriaId) {
        if (categoriaId == null) return null;
        Categoria hoja = porId.get(categoriaId);
        if (hoja == null) return null;
        Categoria padre = hoja.getPadre();
        return new CategoriaNombresDTO(
                null,
                padre != null ? padre.getNombre() : null,
                hoja.getNombre());
    }

    /** Todos los paths de hoja válidos, para ofrecerle la lista cerrada a la IA clasificadora. */
    public List<String> pathsDeHoja() {
        return List.copyOf(idPorPath.keySet());
    }

    /**
     * Resuelve un path (ej. "Almacenamiento > Pen Drive" o "Tablets") al id de esa hoja, o null
     * si no matchea. No distingue mayúsculas/minúsculas ni espacios de más.
     */
    public Long idPorPath(String path) {
        return path == null ? null : idPorPath.get(normalizarPath(path));
    }

    /**
     * Resuelve por nombre de hoja "suelto" (sin el resto del path), solo si ese nombre no se
     * repite en ninguna otra rama del árbol. Null si no hay match único.
     */
    public Long idPorNombreDeHojaUnico(String nombre) {
        return nombre == null ? null : idPorNombreDeHojaUnico.get(normalizarPath(nombre));
    }

    // ===== ABM desde el admin =====

    /** Árbol con los conteos de uso, para que el admin sepa qué puede borrar y qué no. */
    @Transactional(readOnly = true)
    public List<CategoriaAdminDTO> listarParaAdmin() {
        List<CategoriaAdminDTO> out = new ArrayList<>();
        for (Categoria raiz : hijosDe.getOrDefault(null, List.of())) {
            out.add(aAdminDTO(raiz));
        }
        out.sort(Comparator.comparing(CategoriaAdminDTO::getNombre, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private CategoriaAdminDTO aAdminDTO(Categoria c) {
        List<CategoriaAdminDTO> hijos = new ArrayList<>();
        for (Categoria h : hijosDe.getOrDefault(c.getId(), List.of())) {
            hijos.add(aAdminDTO(h));
        }
        hijos.sort(Comparator.comparing(CategoriaAdminDTO::getNombre, String.CASE_INSENSITIVE_ORDER));

        CategoriaAdminDTO dto = new CategoriaAdminDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setPadreId(c.getPadre() != null ? c.getPadre().getId() : null);
        dto.setCantidadProductos(productoRepository.countByCategoriaId(c.getId()));
        dto.setHijos(hijos);
        dto.setPesoGramosDefault(c.getPesoGramosDefault());
        dto.setAltoCmDefault(c.getAltoCmDefault());
        dto.setAnchoCmDefault(c.getAnchoCmDefault());
        dto.setLargoCmDefault(c.getLargoCmDefault());
        return dto;
    }

    @Transactional
    public CategoriaAdminDTO crear(String nombre, Long padreId, Integer peso, Integer alto, Integer ancho, Integer largo) {
        String limpio = validarNombre(nombre);
        Categoria padre = resolverPadre(padreId, null);
        verificarNombreLibre(limpio, padreId, null);

        Categoria c = new Categoria();
        c.setNombre(limpio);
        c.setPadre(padre);
        aplicarMedidas(c, peso, alto, ancho, largo);
        categoriaRepository.save(c);
        cargar();
        return aAdminDTO(porId.get(c.getId()));
    }

    /** Renombra y/o mueve de padre. padreId null = pasa a ser categoría de primer nivel. */
    @Transactional
    public CategoriaAdminDTO actualizar(Long id, String nombre, Long padreId, Integer peso, Integer alto, Integer ancho, Integer largo) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));
        String limpio = validarNombre(nombre);
        Categoria padre = resolverPadre(padreId, id);

        // Mover una categoría que tiene subcategorías dentro de otra crearía un tercer nivel.
        if (padreId != null && !hijosDe.getOrDefault(id, List.of()).isEmpty()) {
            throw new IllegalArgumentException(
                    "\"" + c.getNombre() + "\" tiene subcategorías, así que no puede pasar a ser subcategoría de otra. "
                    + "Movelas o borralas primero.");
        }
        verificarNombreLibre(limpio, padreId, id);

        c.setNombre(limpio);
        c.setPadre(padre);
        aplicarMedidas(c, peso, alto, ancho, largo);
        categoriaRepository.save(c);
        cargar();
        return aAdminDTO(porId.get(id));
    }

    /**
     * Borra una categoría vacía. Si tiene productos o subcategorías se rechaza con el detalle:
     * hay un FK desde productos.categoria_id que de todas formas lo impediría, pero así el admin
     * se entera de cuántos hay que reasignar en vez de comerse un error de base.
     */
    @Transactional
    public void eliminar(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));

        long subcategorias = categoriaRepository.countByPadreId(id);
        long productos = productoRepository.countByCategoriaId(id);
        if (subcategorias > 0 || productos > 0) {
            StringBuilder msg = new StringBuilder("No se puede borrar \"").append(c.getNombre()).append("\": tiene ");
            if (productos > 0) {
                msg.append(productos).append(productos == 1 ? " producto" : " productos");
            }
            if (productos > 0 && subcategorias > 0) msg.append(" y ");
            if (subcategorias > 0) {
                msg.append(subcategorias).append(subcategorias == 1 ? " subcategoría" : " subcategorías");
            }
            // El cierre concuerda con lo que realmente tiene adentro.
            if (productos > 0 && subcategorias > 0) {
                msg.append(". Vaciala primero.");
            } else if (productos > 0) {
                msg.append(productos == 1 ? ". Reasignalo primero." : ". Reasignalos primero.");
            } else {
                msg.append(subcategorias == 1 ? ". Movela o borrala primero." : ". Movelas o borralas primero.");
            }
            throw new IllegalStateException(msg.toString());
        }

        categoriaRepository.delete(c);
        cargar();
    }

    private String validarNombre(String nombre) {
        String limpio = nombre != null ? nombre.trim().replaceAll("\\s+", " ") : "";
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        if (limpio.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar los 100 caracteres.");
        }
        return limpio;
    }

    private void aplicarMedidas(Categoria categoria, Integer peso, Integer alto, Integer ancho, Integer largo) {
        validarMedida("Peso", peso);
        validarMedida("Alto", alto);
        validarMedida("Ancho", ancho);
        validarMedida("Largo", largo);
        categoria.setPesoGramosDefault(peso);
        categoria.setAltoCmDefault(alto);
        categoria.setAnchoCmDefault(ancho);
        categoria.setLargoCmDefault(largo);
    }

    private void validarMedida(String nombre, Integer valor) {
        if (valor != null && valor <= 0) throw new IllegalArgumentException(nombre + " debe ser mayor a cero.");
    }

    /** Valida el padre elegido: existe, no es la propia categoría y no es ya una subcategoría. */
    private Categoria resolverPadre(Long padreId, Long idQueSeEdita) {
        if (padreId == null) return null;
        if (padreId.equals(idQueSeEdita)) {
            throw new IllegalArgumentException("Una categoría no puede ser subcategoría de sí misma.");
        }
        Categoria padre = categoriaRepository.findById(padreId)
                .orElseThrow(() -> new IllegalArgumentException("La categoría padre no existe."));
        if (padre.getPadre() != null) {
            throw new IllegalArgumentException(
                    "\"" + padre.getNombre() + "\" ya es una subcategoría: el árbol tiene dos niveles.");
        }
        return padre;
    }

    /** Dos hermanas no pueden llamarse igual (ignorando mayúsculas y espacios de más). */
    private void verificarNombreLibre(String nombre, Long padreId, Long idQueSeEdita) {
        String buscado = normalizarPath(nombre);
        for (Categoria hermana : hijosDe.getOrDefault(padreId, List.of())) {
            if (hermana.getId().equals(idQueSeEdita)) continue;
            if (normalizarPath(hermana.getNombre()).equals(buscado)) {
                throw new IllegalArgumentException(
                        padreId == null
                                ? "Ya existe una categoría llamada \"" + hermana.getNombre() + "\"."
                                : "Esa categoría ya tiene una subcategoría llamada \"" + hermana.getNombre() + "\".");
            }
        }
    }
}
