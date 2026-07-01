package com.futuratecno.api;

import com.futuratecno.application.InvidImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Integración con el mayorista Invid: estado, filtros, previsualización e importación del catálogo.
 * Protegido (cae bajo /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/invid")
public class InvidController {
    private static final Logger logger = LoggerFactory.getLogger(InvidController.class);

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
        return ejecutar(invidImportService::filtros);
    }

    @PostMapping("/previsualizar")
    public ResponseEntity<?> previsualizar(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Map.of();
        return ejecutar(() -> invidImportService.previsualizar(
                str(b.get("categoria")), str(b.get("marca")), dec(b.get("precioMinUsd"))));
    }

    @PostMapping("/importar")
    public ResponseEntity<?> importar(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Map.of();
        boolean soloConStock = Boolean.TRUE.equals(b.get("soloConStock"));
        return ejecutar(() -> invidImportService.importar(
                str(b.get("categoria")), str(b.get("marca")), soloConStock, dec(b.get("precioMinUsd"))));
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar() {
        return ejecutar(invidImportService::sincronizar);
    }

    /** Ejecuta la acción devolviendo el motivo real si algo falla (en vez de un 500 genérico). */
    private ResponseEntity<?> ejecutar(Supplier<Object> accion) {
        if (!invidImportService.estaConfigurado()) {
            return error(HttpStatus.BAD_REQUEST,
                    "La API de Invid no está configurada. Cargá INVID_BASE_URL, INVID_USERNAME y INVID_PASSWORD.");
        }
        try {
            return ResponseEntity.ok(accion.get());
        } catch (Exception e) {
            logger.error("Invid: operación fallida", e);
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
