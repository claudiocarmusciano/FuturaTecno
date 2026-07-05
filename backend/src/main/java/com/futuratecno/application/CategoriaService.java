package com.futuratecno.application;

import com.futuratecno.api.dto.CategoriaNombresDTO;
import com.futuratecno.api.dto.CategoriaTreeDTO;
import com.futuratecno.domain.Categoria;
import com.futuratecno.infrastructure.CategoriaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El árbol de categorías es fijo (sembrado por migración, sin CRUD desde la app), así que se
 * carga una sola vez a memoria al arrancar y se reutiliza — evita recorrer todas las filas en
 * cada request de catálogo. La profundidad no está fijada: una hoja puede ser una categoría
 * de primer nivel sin hijos (ej. "Tablets") o una subcategoría (ej. "Almacenamiento > Pen Drive").
 */
@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;

    private Map<Long, Categoria> porId;
    private Map<Long, List<Categoria>> hijosDe;
    /** Path completo ("Categoría > Subcategoría", o solo "Categoría" si no tiene hijos) -> id de la hoja. */
    private Map<String, Long> idPorPath;
    /**
     * Nombre de hoja (sin el resto del path) -> id, solo para nombres que no se repiten en
     * ninguna otra hoja. Sirve de último recurso cuando un mayorista informa una jerarquía más
     * profunda que la nuestra (ej. Invid manda "DDR3" bajo "Memoria Sodimm", pero en nuestro
     * árbol "Memoria Sodimm" ya es la hoja) — redondea hacia el ancestro más cercano que sí existe.
     */
    private Map<String, Long> idPorNombreDeHojaUnico;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @PostConstruct
    void cargar() {
        List<Categoria> todas = categoriaRepository.findAll();
        porId = todas.stream().collect(Collectors.toMap(Categoria::getId, c -> c));
        hijosDe = new HashMap<>();
        for (Categoria c : todas) {
            Long padreId = c.getPadre() != null ? c.getPadre().getId() : null;
            hijosDe.computeIfAbsent(padreId, k -> new ArrayList<>()).add(c);
        }
        idPorPath = new HashMap<>();
        idPorNombreDeHojaUnico = new HashMap<>();
        Map<String, Integer> ocurrenciasNombre = new HashMap<>();
        for (Categoria c : todas) {
            if (!esHoja(c)) continue;
            idPorPath.put(normalizarPath(pathDe(c)), c.getId());
            String nombreNorm = normalizarPath(c.getNombre());
            ocurrenciasNombre.merge(nombreNorm, 1, Integer::sum);
            idPorNombreDeHojaUnico.put(nombreNorm, c.getId());
        }
        // Si el mismo nombre de hoja aparece en más de una rama, no adivinamos: se saca del índice.
        ocurrenciasNombre.forEach((nombre, veces) -> {
            if (veces > 1) idPorNombreDeHojaUnico.remove(nombre);
        });
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
}
