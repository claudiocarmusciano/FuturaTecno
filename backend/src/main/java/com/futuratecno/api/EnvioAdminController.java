package com.futuratecno.api;

import com.futuratecno.api.dto.ProductoSinEnvioDTO;
import com.futuratecno.application.EnvioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Auditoría de cotización de envíos, disponible solo dentro del panel administrativo. */
@RestController
@RequestMapping("/api/admin/envios")
@CrossOrigin(origins = "*")
public class EnvioAdminController {
    private final EnvioService envioService;

    public EnvioAdminController(EnvioService envioService) { this.envioService = envioService; }

    @GetMapping("/sin-cotizar")
    public ResponseEntity<List<ProductoSinEnvioDTO>> sinCotizar() {
        return ResponseEntity.ok(envioService.productosSinMedidasCotizables());
    }
}
