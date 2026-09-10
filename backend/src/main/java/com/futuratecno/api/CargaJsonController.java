package com.futuratecno.api;

import com.futuratecno.api.dto.CargaJsonRequest;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.application.CargaJsonService;
import com.futuratecno.application.GenerarListadoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/carga-json")
@CrossOrigin(origins = "*")
public class CargaJsonController {

    private final CargaJsonService cargaJsonService;

    private final GenerarListadoService generador;

    public CargaJsonController(CargaJsonService cargaJsonService, GenerarListadoService generador) {
        this.generador = generador;
        this.cargaJsonService = cargaJsonService;
    }

    public record ListadoRequest(String texto) {}
    public record ImagenRequest(String marca, String modelo) {}

    @PostMapping("/generar")
    public ResponseEntity<?> generar(@RequestBody ListadoRequest req) {
        try { return ResponseEntity.ok(generador.generar(req.texto())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (IllegalStateException e) { return ResponseEntity.status(503).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.status(502).body(Map.of("error", "No se pudo generar el listado. Revisá la conexión o la configuración de IA y reintentá.")); }
    }

    @PostMapping("/generar-imagen")
    public ResponseEntity<?> imagen(@RequestBody ImagenRequest req) {
        try { return ResponseEntity.ok(generador.imagen(req.marca(), req.modelo())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (IllegalStateException e) { return ResponseEntity.status(503).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.status(502).body(Map.of("error", "No se pudo buscar la imagen. Podés ingresarla manualmente.")); }
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
