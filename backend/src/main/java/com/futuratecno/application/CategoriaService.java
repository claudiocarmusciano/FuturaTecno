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
 * El árbol de categorías es fijo (sembrado por la V9, sin CRUD desde la app), así que se
 * carga una sola vez a memoria al arrancar y se reutiliza — evita recorrer 100 filas por
 * cada request de catálogo.
 */
@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;

    private Map<Long, Categoria> porId;
    private Map<Long, List<Categoria>> hijosDe;
    /** Path completo ("Sección > Categoría > Subcategoría") -> id de la subcategoría (hoja). */
    private Map<String, Long> idPorPath;

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
        for (Categoria c : todas) {
            if (esHoja(c)) {
                idPorPath.put(pathDe(c), c.getId());
            }
        }
    }

    private boolean esHoja(Categoria c) {
        List<Categoria> hijos = hijosDe.get(c.getId());
        return hijos == null || hijos.isEmpty();
    }

    private String pathDe(Categoria hoja) {
        Categoria categoriaPadre = hoja.getPadre();
        Categoria seccion = categoriaPadre != null ? categoriaPadre.getPadre() : null;
        return (seccion != null ? seccion.getNombre() : "") + " > "
                + (categoriaPadre != null ? categoriaPadre.getNombre() : "") + " > " + hoja.getNombre();
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

    /** Nombres de sección/categoría/subcategoría para una hoja. Null si categoriaId no existe o no es hoja. */
    public CategoriaNombresDTO resolverNombres(Long categoriaId) {
        if (categoriaId == null) return null;
        Categoria hoja = porId.get(categoriaId);
        if (hoja == null) return null;
        Categoria categoriaPadre = hoja.getPadre();
        Categoria seccion = categoriaPadre != null ? categoriaPadre.getPadre() : null;
        return new CategoriaNombresDTO(
                seccion != null ? seccion.getNombre() : null,
                categoriaPadre != null ? categoriaPadre.getNombre() : null,
                hoja.getNombre());
    }

    /** Todos los paths de hoja válidos, para ofrecerle la lista cerrada a la IA clasificadora. */
    public List<String> pathsDeHoja() {
        return List.copyOf(idPorPath.keySet());
    }

    /** Resuelve un path exacto ("Sección > Categoría > Subcategoría") al id de esa hoja, o null si no matchea. */
    public Long idPorPath(String path) {
        return path == null ? null : idPorPath.get(path.trim());
    }
}
