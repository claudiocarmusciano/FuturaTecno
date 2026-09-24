package com.futuratecno.api;

import com.futuratecno.application.IdentidadTransicionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Transición de los productos existentes a la identidad de la V42. Protegido por
 * {@code /api/admin/**} (rol ADMIN) en SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/identidad")
public class IdentidadAdminController {

    private final IdentidadTransicionService transicion;

    public IdentidadAdminController(IdentidadTransicionService transicion) {
        this.transicion = transicion;
    }

    /** Solo lectura: inequívocos, duplicados históricos y casos ambiguos. No escribe nada. */
    @GetMapping("/transicion")
    public ResponseEntity<IdentidadTransicionService.Reporte> reporte() {
        return ResponseEntity.ok(transicion.analizar());
    }

    /**
     * Asigna la identidad a los inequívocos que todavía no la tienen. No fusiona, no da de baja y
     * no borra: los duplicados y los ambiguos quedan como están para que el admin decida.
     */
    @PostMapping("/transicion/asignar")
    public ResponseEntity<Map<String, Object>> asignar() {
        int n = transicion.asignarInequivocos();
        return ResponseEntity.ok(Map.of("asignados", n));
    }
}
