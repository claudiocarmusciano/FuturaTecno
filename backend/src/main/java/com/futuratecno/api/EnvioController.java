package com.futuratecno.api;

import com.futuratecno.api.dto.CotizarEnvioRequest;
import com.futuratecno.api.dto.EnvioCotizacionDTO;
import com.futuratecno.application.EnvioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cotización de envío para el checkout. Público a propósito: el carrito se arma sin cuenta y la
 * cotización se muestra antes de pedir sesión. No expone datos sensibles (solo tarifas).
 */
@RestController
@RequestMapping("/api/envio")
@CrossOrigin(origins = "*")
public class EnvioController {

    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    @PostMapping("/cotizar")
    public ResponseEntity<?> cotizar(@RequestBody CotizarEnvioRequest req) {
        try {
            EnvioCotizacionDTO dto = envioService.cotizar(req.getCpDestino(), req.getItems());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
