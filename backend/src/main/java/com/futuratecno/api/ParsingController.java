package com.futuratecno.api;

import com.futuratecno.api.dto.ImportacionRequest;
import com.futuratecno.api.dto.ImportacionResponse;
import com.futuratecno.api.dto.ParsingRequest;
import com.futuratecno.api.dto.ParsingResponse;
import com.futuratecno.application.ImportacionService;
import com.futuratecno.application.ParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/parsing")
public class ParsingController {

    private final ParsingService parsingService;
    private final ImportacionService importacionService;

    public ParsingController(ParsingService parsingService, ImportacionService importacionService) {
        this.parsingService = parsingService;
        this.importacionService = importacionService;
    }

    @PostMapping
    public ResponseEntity<ParsingResponse> parsearLista(@RequestBody ParsingRequest request) {
        if (request.getTexto() == null || request.getTexto().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ParsingResponse response = parsingService.parsearLista(request.getTexto());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirmar")
    public ResponseEntity<ImportacionResponse> confirmarImportacion(@RequestBody ImportacionRequest request) {
        if (request.getProveedorId() == null || request.getProductos() == null || request.getProductos().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ImportacionResponse response = importacionService.importar(request.getProveedorId(), request.getProductos());
        return ResponseEntity.ok(response);
    }
}
