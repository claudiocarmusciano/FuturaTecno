package com.futuratecno.api;

import com.futuratecno.api.dto.MisPuntosDTO;
import com.futuratecno.application.PuntosService;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/puntos")
public class PuntosController {
    private final PuntosService puntosService;
    public PuntosController(PuntosService puntosService) { this.puntosService = puntosService; }
    @GetMapping("/mis-puntos")
    public ResponseEntity<MisPuntosDTO> misPuntos(Principal principal) {
        return ResponseEntity.ok(puntosService.misPuntos(principal.getName()));
    }
}
