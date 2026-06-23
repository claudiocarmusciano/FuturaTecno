package com.futuratecno.api;

import com.futuratecno.application.CotizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/cotizacion")
@CrossOrigin(origins = "*")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtener() {
        return ResponseEntity.ok(Map.of(
                "valor", cotizacionService.obtenerCotizacionUsdArs(),
                "fuente", "dólar oficial"
        ));
    }
}
