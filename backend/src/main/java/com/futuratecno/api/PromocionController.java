package com.futuratecno.api;

import com.futuratecno.api.dto.PromocionDTO;
import com.futuratecno.application.PromocionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class PromocionController {
    private final PromocionService service;
    public PromocionController(PromocionService service) { this.service = service; }

    @GetMapping("/api/promociones")
    public List<PromocionDTO> publicadas() { return service.listarPublicadas(); }

    @GetMapping("/api/promociones/{id}/imagen/{tipo}")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id, @PathVariable String tipo) {
        var img = service.imagen(id, "movil".equals(tipo));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.mime()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(img.bytes());
    }

    @GetMapping("/api/admin/promociones")
    public List<PromocionDTO> listarAdmin() { return service.listarAdmin(); }

    @PostMapping(value = "/api/admin/promociones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PromocionDTO crear(
            @RequestParam(required = false) String titulo, @RequestParam(required = false) String texto,
            @RequestParam(required = false) String enlace, @RequestParam(defaultValue = "0") Integer orden,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestPart MultipartFile escritorio, @RequestPart(required = false) MultipartFile movil) {
        return service.crear(titulo, texto, enlace, orden, fechaInicio, fechaFin, activo, escritorio, movil);
    }

    @PutMapping(value = "/api/admin/promociones/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PromocionDTO actualizar(
            @PathVariable Long id, @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String texto, @RequestParam(required = false) String enlace,
            @RequestParam(defaultValue = "0") Integer orden,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestParam(defaultValue = "false") boolean quitarMovil,
            @RequestPart(required = false) MultipartFile escritorio,
            @RequestPart(required = false) MultipartFile movil) {
        return service.actualizar(id, titulo, texto, enlace, orden, fechaInicio, fechaFin, activo, escritorio, movil, quitarMovil);
    }

    @DeleteMapping("/api/admin/promociones/{id}")
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> errorValidacion(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
