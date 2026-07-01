package com.futuratecno.api;

import com.futuratecno.application.InvidImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Integración con el mayorista Invid: estado, filtros, previsualización e importación del catálogo.
 * Protegido (cae bajo /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/invid")
public class InvidController {

    private final InvidImportService invidImportService;

    public InvidController(InvidImportService invidImportService) {
        this.invidImportService = invidImportService;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configurado", invidImportService.estaConfigurado());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/filtros")
    public ResponseEntity<?> filtros() {
        if (!invidImportService.estaConfigurado()) return apiNoConfigurada();
        return ResponseEntity.ok(invidImportService.filtros());
    }

    @PostMapping("/previsualizar")
    public ResponseEntity<?> previsualizar(@RequestBody(required = false) Map<String, Object> body) {
        if (!invidImportService.estaConfigurado()) return apiNoConfigurada();
        Map<String, Object> b = body != null ? body : Map.of();
        return ResponseEntity.ok(invidImportService.previsualizar(str(b.get("categoria")), str(b.get("marca"))));
    }

    @PostMapping("/importar")
    public ResponseEntity<?> importar(@RequestBody(required = false) Map<String, Object> body) {
        if (!invidImportService.estaConfigurado()) return apiNoConfigurada();
        Map<String, Object> b = body != null ? body : Map.of();
        boolean soloConStock = Boolean.TRUE.equals(b.get("soloConStock"));
        return ResponseEntity.ok(invidImportService.importar(str(b.get("categoria")), str(b.get("marca")), soloConStock));
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar() {
        if (!invidImportService.estaConfigurado()) return apiNoConfigurada();
        return ResponseEntity.ok(invidImportService.sincronizar());
    }

    private ResponseEntity<Map<String, Object>> apiNoConfigurada() {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", "La API de Invid no está configurada. Cargá INVID_BASE_URL, INVID_USERNAME y INVID_PASSWORD en las variables de entorno.");
        return ResponseEntity.badRequest().body(err);
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
