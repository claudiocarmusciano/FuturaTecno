package com.futuratecno.api;

import com.futuratecno.application.ArrepentimientoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Botón de arrepentimiento. Público: la norma no permite exigir registro ni login para usarlo. */
@RestController
@RequestMapping("/api/arrepentimiento")
public class ArrepentimientoController {

    private final ArrepentimientoService service;

    public ArrepentimientoController(ArrepentimientoService service) {
        this.service = service;
    }

    /** {@code sitio} es un campo trampa: invisible para una persona, los bots lo completan. */
    public record Body(String nombre, String email, String telefono, String numeroPedido, String detalle, String sitio) {}

    @PostMapping
    public ResponseEntity<Map<String, String>> solicitar(@RequestBody Body b, HttpServletRequest request) {
        if (b.sitio() != null && !b.sitio().isBlank()) {
            // Al bot se le responde como si hubiera salido bien: así no aprende a esquivarlo.
            return ResponseEntity.ok(Map.of("codigo", "ARR-RECIBIDO"));
        }
        try {
            String codigo = service.solicitar(new ArrepentimientoService.Solicitud(
                    b.nombre(), b.email(), b.telefono(), b.numeroPedido(), b.detalle()), request.getRemoteAddr());
            return ResponseEntity.ok(Map.of("codigo", codigo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ArrepentimientoService.DemasiadasSolicitudesException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        }
    }
}
