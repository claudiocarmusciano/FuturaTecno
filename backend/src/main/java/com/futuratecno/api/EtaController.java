package com.futuratecno.api;

import com.futuratecno.api.dto.EtaResponse;
import com.futuratecno.application.EtaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eta")
@CrossOrigin(origins = "*")
public class EtaController {

    private final EtaService etaService;

    public EtaController(EtaService etaService) {
        this.etaService = etaService;
    }

    @GetMapping
    public ResponseEntity<EtaResponse> obtener() {
        return ResponseEntity.ok(new EtaResponse(
                etaService.calcularFechaEntrega(),
                etaService.getDiasHabiles(),
                etaService.getHoraCorte(),
                etaService.esAntesDeCorte()
        ));
    }
}
