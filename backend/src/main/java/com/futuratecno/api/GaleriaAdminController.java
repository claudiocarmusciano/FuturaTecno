package com.futuratecno.api;

import com.futuratecno.application.GaleriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Galería de fotos de un producto (Admin → Imágenes). Bajo /api/admin/** → solo ADMIN. */
@RestController
@RequestMapping("/api/admin/productos/{id}/galeria")
public class GaleriaAdminController {
    private final GaleriaService galeriaService;

    public GaleriaAdminController(GaleriaService galeriaService) {
        this.galeriaService = galeriaService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@PathVariable Long id) {
        return responder(() -> galeriaService.listar(id));
    }

    @PostMapping
    public ResponseEntity<?> agregar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return responder(() -> galeriaService.agregar(id, body.get("url")));
    }

    @DeleteMapping("/{imagenId}")
    public ResponseEntity<?> quitar(@PathVariable Long id, @PathVariable Long imagenId) {
        return responder(() -> galeriaService.quitar(id, imagenId));
    }

    private ResponseEntity<?> responder(java.util.function.Supplier<Object> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
