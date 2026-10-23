package com.futuratecno.api;

import com.futuratecno.application.KadabraImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Kadabra: "Actualizar ahora" del Inicio del admin. Corre en segundo plano (la IA tarda unos minutos). */
@RestController
@RequestMapping("/api/admin/kadabra")
public class KadabraController {
    private final KadabraImportService kadabra;

    public KadabraController(KadabraImportService kadabra) {
        this.kadabra = kadabra;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return kadabra.estado();
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar() {
        try {
            boolean arranco = kadabra.iniciar();
            return ResponseEntity.ok(Map.of("iniciado", arranco,
                    "mensaje", arranco ? "Actualizando la lista de Kadabra. Tarda unos minutos." : "Ya se está actualizando."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
