package com.futuratecno.api;

import com.futuratecno.api.dto.CategoriaTreeDTO;
import com.futuratecno.application.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Árbol de categorías (público, sin auth): lo usa el menú del catálogo y el selector del admin. */
@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaTreeDTO>> arbol() {
        return ResponseEntity.ok(categoriaService.obtenerArbol());
    }
}
