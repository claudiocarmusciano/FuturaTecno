package com.futuratecno.api;

import com.futuratecno.api.dto.CatalogoPaginaDTO;
import com.futuratecno.api.dto.PortadaCatalogoDTO;
import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.application.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoCatalogoDTO>> listar() {
        return ResponseEntity.ok(catalogoService.listarCatalogo());
    }

    /**
     * Catálogo paginado y filtrado en el servidor: devuelve 24 productos (o {@code size}, hasta 96)
     * en vez de los miles de {@code GET /api/productos}, que queda por compatibilidad.
     */
    @GetMapping("/buscar")
    public ResponseEntity<CatalogoPaginaDTO> buscar(
            @RequestParam(required = false) Long cat,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(catalogoService.buscar(cat, marca, q, min, max, orden, page, size));
    }

    @GetMapping("/portada")
    public ResponseEntity<PortadaCatalogoDTO> portada() {
        return ResponseEntity.ok(catalogoService.portada());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoCatalogoDTO> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(catalogoService.obtenerProducto(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
