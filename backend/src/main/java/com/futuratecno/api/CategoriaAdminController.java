package com.futuratecno.api;

import com.futuratecno.api.dto.CategoriaAdminDTO;
import com.futuratecno.api.dto.CategoriaRequest;
import com.futuratecno.application.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** ABM del árbol de categorías. Bajo /api/admin/** → ya exige rol ADMIN por SecurityConfig. */
@RestController
@RequestMapping("/api/admin/categorias")
@CrossOrigin(origins = "*")
public class CategoriaAdminController {

    private final CategoriaService categoriaService;

    public CategoriaAdminController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaAdminDTO>> listar() {
        return ResponseEntity.ok(categoriaService.listarParaAdmin());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CategoriaRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(categoriaService.crear(req.getNombre(), req.getPadreId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody CategoriaRequest req) {
        try {
            return ResponseEntity.ok(categoriaService.actualizar(id, req.getNombre(), req.getPadreId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            categoriaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // Está en uso: 409, con el detalle de cuántos productos/subcategorías hay que mover.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
