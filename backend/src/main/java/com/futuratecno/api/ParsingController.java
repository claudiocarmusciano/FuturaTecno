package com.futuratecno.api;

import com.futuratecno.api.dto.ParsingRequest;
import com.futuratecno.api.dto.ParsingResponse;
import com.futuratecno.application.ParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/parsing")
public class ParsingController {

    private final ParsingService parsingService;

    public ParsingController(ParsingService parsingService) {
        this.parsingService = parsingService;
    }

    @PostMapping
    public ResponseEntity<ParsingResponse> parsearLista(@RequestBody ParsingRequest request) {
        if (request.getTexto() == null || request.getTexto().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ParsingResponse response = parsingService.parsearLista(request.getTexto());
        return ResponseEntity.ok(response);
    }
}
