package com.futuratecno.api;

import com.futuratecno.api.dto.PedidoDTO;
import com.futuratecno.application.PedidoService;
import com.futuratecno.domain.EstadoPedido;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Bandeja de pedidos del admin. Bajo /api/admin/** → ya exige rol ADMIN por SecurityConfig. */
@RestController
@RequestMapping("/api/admin/pedidos")
@CrossOrigin(origins = "*")
public class PedidoAdminController {

    private final PedidoService pedidoService;

    public PedidoAdminController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String estado) {
        EstadoPedido filtro = null;
        if (estado != null && !estado.isBlank()) {
            try {
                filtro = EstadoPedido.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Estado desconocido: " + estado));
            }
        }
        List<PedidoDTO> pedidos = pedidoService.listarParaAdmin(filtro);
        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta el estado."));
        }
        try {
            EstadoPedido nuevo = EstadoPedido.valueOf(estado.trim().toUpperCase());
            return ResponseEntity.ok(pedidoService.cambiarEstado(id, nuevo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No se pudo cambiar el estado: " + e.getMessage()));
        }
    }
}
