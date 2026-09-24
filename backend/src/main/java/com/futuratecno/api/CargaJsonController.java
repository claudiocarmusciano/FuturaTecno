package com.futuratecno.api;

import com.futuratecno.api.dto.CargaJsonRequest;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.application.CargaJsonService;
import com.futuratecno.application.GenerarListadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/carga-json")
@CrossOrigin(origins = "*")
public class CargaJsonController {

    private static final Logger logger = LoggerFactory.getLogger(CargaJsonController.class);

    private final CargaJsonService cargaJsonService;

    private final GenerarListadoService generador;

    public CargaJsonController(CargaJsonService cargaJsonService, GenerarListadoService generador) {
        this.generador = generador;
        this.cargaJsonService = cargaJsonService;
    }

    public record ListadoRequest(String texto) {}
    public record ImagenRequest(String marca, String modelo) {}

    // Todo fallo se loguea antes de responder: el mensaje que ve el admin es deliberadamente corto,
    // así que sin log la causa real (cuota agotada, clave mal, modelo inexistente) se perdía entera.
    @PostMapping("/generar")
    public ResponseEntity<?> generar(@RequestBody ListadoRequest req) {
        try { return ResponseEntity.ok(generador.generar(req.texto())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (IllegalStateException e) {
            logger.warn("Generar listado: la IA no pudo responder — {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
        catch (Exception e) {
            logger.error("Generar listado: fallo inesperado", e);
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo generar el listado. Revisá la conexión o la configuración de IA y reintentá."));
        }
    }

    @PostMapping("/generar-imagen")
    public ResponseEntity<?> imagen(@RequestBody ImagenRequest req) {
        try { return ResponseEntity.ok(generador.imagen(req.marca(), req.modelo())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (IllegalStateException e) {
            logger.warn("Buscar imagen de {} {}: la IA no pudo responder — {}", req.marca(), req.modelo(), e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
        catch (Exception e) {
            logger.error("Buscar imagen de {} {}: fallo inesperado", req.marca(), req.modelo(), e);
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo buscar la imagen. Podés ingresarla manualmente."));
        }
    }

    /**
     * Identidad de cada artículo de un borrador, sin tocar la base: el panel la usa para marcar
     * duplicados antes de importar. Dos filas con el mismo nombre pero otra RAM NO son duplicado;
     * dos redacciones del mismo teléfono sí.
     */
    @PostMapping("/identidades")
    public ResponseEntity<?> identidades(@RequestBody CargaJsonRequest req) {
        if (req.getArticulos() == null) return ResponseEntity.badRequest().body(Map.of("error", "Falta la lista de artículos."));
        return ResponseEntity.ok(cargaJsonService.identidades(req.getArticulos()));
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
