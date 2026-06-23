package com.futuratecno.api;

import com.futuratecno.api.dto.BuscarImagenesResponse;
import com.futuratecno.api.dto.ProductoAdminDTO;
import com.futuratecno.api.dto.ProductoEditDTO;
import com.futuratecno.application.ProductoAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/productos")
@CrossOrigin(origins = "*")
public class ProductoAdminController {

    private final ProductoAdminService productoAdminService;

    public ProductoAdminController(ProductoAdminService productoAdminService) {
        this.productoAdminService = productoAdminService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoAdminDTO>> listar() {
        return ResponseEntity.ok(productoAdminService.listar());
    }

    @PostMapping("/buscar-imagenes")
    public ResponseEntity<BuscarImagenesResponse> buscarImagenes() {
        return ResponseEntity.ok(productoAdminService.buscarImagenesFaltantes());
    }

    @PutMapping("/{id}/imagen")
    public ResponseEntity<ProductoAdminDTO> actualizarImagen(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(productoAdminService.actualizarImagen(id, body.get("url")));
    }

    @GetMapping("/{id}/editar")
    public ResponseEntity<ProductoEditDTO> obtenerParaEditar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productoAdminService.obtenerParaEditar(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoEditDTO> actualizar(@PathVariable Long id, @RequestBody ProductoEditDTO dto) {
        try {
            return ResponseEntity.ok(productoAdminService.actualizarProducto(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoAdminService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
