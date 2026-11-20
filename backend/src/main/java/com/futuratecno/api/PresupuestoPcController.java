package com.futuratecno.api;

import com.futuratecno.application.PresupuestoPcService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PresupuestoPcController {
    private final PresupuestoPcService service;
    public PresupuestoPcController(PresupuestoPcService service) { this.service = service; }

    @GetMapping("/api/presupuestos")
    public List<Map<String, Object>> mios(Principal principal) { return service.mios(principal.getName()); }

    @PostMapping("/api/presupuestos")
    public Map<String, Object> crear(Principal principal, @RequestBody Map<String, Object> body) {
        return service.guardar(principal.getName(), null, body);
    }

    @PutMapping("/api/presupuestos/{id}")
    public Map<String, Object> actualizar(Principal principal, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.guardar(principal.getName(), id, body);
    }

    @DeleteMapping("/api/presupuestos/{id}")
    public ResponseEntity<Void> borrar(Principal principal, @PathVariable Long id) {
        service.borrar(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/presupuestos")
    public List<Map<String, Object>> todos() { return service.todos(); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> errorValidacion(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
