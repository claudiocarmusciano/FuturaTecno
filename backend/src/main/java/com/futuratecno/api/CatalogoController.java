package com.futuratecno.api;

import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.application.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/productos")
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

    @GetMapping("/{id}")
    public ResponseEntity<ProductoCatalogoDTO> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(catalogoService.obtenerProducto(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
