package com.futuratecno.api;

import com.futuratecno.api.dto.ComponentePcDTO;
import com.futuratecno.application.ArmaTuPcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Público, como el catálogo: cae en el {@code anyRequest().permitAll()} de SecurityConfig. */
@RestController
@RequestMapping("/api/arma-tu-pc")
public class ArmaTuPcController {

    private final ArmaTuPcService armaTuPcService;

    public ArmaTuPcController(ArmaTuPcService armaTuPcService) {
        this.armaTuPcService = armaTuPcService;
    }

    @GetMapping("/componentes")
    public ResponseEntity<List<ComponentePcDTO>> componentes() {
        return ResponseEntity.ok(armaTuPcService.listarComponentes());
    }
}
