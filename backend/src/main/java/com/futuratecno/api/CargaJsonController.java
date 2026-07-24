package com.futuratecno.api;

import com.futuratecno.api.dto.CargaJsonRequest;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.application.CargaJsonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/carga-json")
@CrossOrigin(origins = "*")
public class CargaJsonController {

    private final CargaJsonService cargaJsonService;

    public CargaJsonController(CargaJsonService cargaJsonService) {
        this.cargaJsonService = cargaJsonService;
    }

    @PostMapping
    public ResponseEntity<?> cargar(@RequestBody CargaJsonRequest req) {
        if (req.getProveedorId() == null || req.getArticulos() == null || req.getArticulos().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta el proveedor o la lista de artículos."));
        }
        try {
            CargaJsonResponse res = cargaJsonService.cargar(req.getProveedorId(), req.getArticulos());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
