package com.futuratecno.api;

import com.futuratecno.application.ElitImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Integración con el mayorista Elit: estado de configuración, filtros disponibles,
 * previsualización e importación del catálogo. Protegido (cae bajo /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/elit")
public class ElitController {

    private final ElitImportService elitImportService;

    public ElitController(ElitImportService elitImportService) {
        this.elitImportService = elitImportService;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configurado", elitImportService.estaConfigurado());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/filtros")
    public ResponseEntity<?> filtros() {
        if (!elitImportService.estaConfigurado()) return apiNoConfigurada();
        return ResponseEntity.ok(elitImportService.filtros());
    }

    @PostMapping("/previsualizar")
    public ResponseEntity<?> previsualizar(@RequestBody(required = false) Map<String, Object> body) {
        if (!elitImportService.estaConfigurado()) return apiNoConfigurada();
        Map<String, Object> b = body != null ? body : Map.of();
        return ResponseEntity.ok(elitImportService.previsualizar(
                str(b.get("categoria")), str(b.get("marca")), str(b.get("store"))));
    }

    @PostMapping("/importar")
    public ResponseEntity<?> importar(@RequestBody(required = false) Map<String, Object> body) {
        if (!elitImportService.estaConfigurado()) return apiNoConfigurada();
        Map<String, Object> b = body != null ? body : Map.of();
        boolean soloConStock = Boolean.TRUE.equals(b.get("soloConStock"));
        return ResponseEntity.ok(elitImportService.importar(
                str(b.get("categoria")), str(b.get("marca")), soloConStock, str(b.get("store"))));
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar() {
        if (!elitImportService.estaConfigurado()) return apiNoConfigurada();
        return ResponseEntity.ok(elitImportService.sincronizar());
    }

    private ResponseEntity<Map<String, Object>> apiNoConfigurada() {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", "La API de Elit no está configurada. Cargá ELIT_USER_ID y ELIT_TOKEN en las variables de entorno.");
        return ResponseEntity.badRequest().body(err);
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
