package com.futuratecno.api;

import com.futuratecno.application.ListasNotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Botón "Procesar listas Notion" del admin. Queda bajo /api/admin/**, o sea solo ADMIN. */
@RestController
@RequestMapping("/api/admin/listas-notion")
public class ListasNotionController {

    private final ListasNotionService listasNotionService;

    public ListasNotionController(ListasNotionService listasNotionService) {
        this.listasNotionService = listasNotionService;
    }

    @PostMapping("/procesar")
    public ResponseEntity<Map<String, String>> procesar(Authentication auth) {
        try {
            listasNotionService.procesar(auth != null ? auth.getName() : null);
            return ResponseEntity.ok(Map.of("mensaje",
                    "Listo: n8n empezó a procesar las listas en Pendiente. El resultado de cada una aparece en Notion en unos minutos."));
        } catch (ListasNotionService.NoDisponibleException e) {
            HttpStatus status = e.isConfigurado() ? HttpStatus.BAD_GATEWAY : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/estado")
    public Map<String, Object> estado() {
        return listasNotionService.estado();
    }

    /** Lo llama n8n al final del workflow, autenticado como admin (el mismo login de la carga). */
    @PostMapping("/resultado")
    public ResponseEntity<Void> resultado(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> resumen) {
        listasNotionService.registrarResultado(resumen);
        return ResponseEntity.noContent().build();
    }
}
