package com.futuratecno.api;

import com.futuratecno.application.FlrImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Flor Qtech (FLR): importar ahora la planilla, además de las dos corridas diarias. */
@RestController
@RequestMapping("/api/admin/flr")
public class FlrController {
    private static final Logger logger = LoggerFactory.getLogger(FlrController.class);
    private final FlrImportService flrImportService;

    public FlrController(FlrImportService flrImportService) {
        this.flrImportService = flrImportService;
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar() {
        try {
            return ResponseEntity.ok(flrImportService.sincronizar());
        } catch (Exception e) {
            logger.error("Flor Qtech: importación fallida", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", msg));
        }
    }
}
