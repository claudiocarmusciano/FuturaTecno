package com.futuratecno.api;

import com.futuratecno.application.ElitImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Integración con el mayorista Elit: estado, filtros, previsualización e importación del catálogo.
 * Protegido (cae bajo /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/elit")
public class ElitController {
    private static final Logger logger = LoggerFactory.getLogger(ElitController.class);

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
        return ejecutar(elitImportService::filtros);
    }

    @PostMapping("/previsualizar")
    public ResponseEntity<?> previsualizar(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Map.of();
        return ejecutar(() -> elitImportService.previsualizar(
                str(b.get("categoria")), str(b.get("marca")), str(b.get("store"))));
    }

    @PostMapping("/importar")
    public ResponseEntity<?> importar(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Map.of();
        boolean soloConStock = Boolean.TRUE.equals(b.get("soloConStock"));
        return ejecutar(() -> elitImportService.importar(
                str(b.get("categoria")), str(b.get("marca")), soloConStock, str(b.get("store")), dec(b.get("precioMinUsd"))));
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar() {
        return ejecutar(elitImportService::sincronizar);
    }

    /** Ejecuta la acción devolviendo el motivo real si algo falla (en vez de un 500 genérico). */
    private ResponseEntity<?> ejecutar(Supplier<Object> accion) {
        if (!elitImportService.estaConfigurado()) {
            return error(HttpStatus.BAD_REQUEST,
                    "La API de Elit no está configurada. Cargá ELIT_USER_ID y ELIT_TOKEN.");
        }
        try {
            return ResponseEntity.ok(accion.get());
        } catch (Exception e) {
            logger.error("Elit: operación fallida", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return error(HttpStatus.BAD_GATEWAY, msg);
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", mensaje);
        return ResponseEntity.status(status).body(err);
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private java.math.BigDecimal dec(Object o) {
        if (o == null) return null;
        String s = o.toString().trim().replace(",", ".");
        if (s.isEmpty()) return null;
        try { return new java.math.BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }
}
