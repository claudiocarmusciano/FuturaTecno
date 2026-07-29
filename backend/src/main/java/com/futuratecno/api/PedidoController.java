package com.futuratecno.api;

import com.futuratecno.api.dto.CrearPedidoRequest;
import com.futuratecno.api.dto.PedidoDTO;
import com.futuratecno.application.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Pedidos del cliente. Todo /api/pedidos/** exige sesión (ver SecurityConfig): el carrito se arma
 * sin cuenta, pero confirmar requiere estar logueado.
 */
@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearPedidoRequest req, Principal principal) {
        try {
            PedidoDTO dto = pedidoService.crear(req, principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mis")
    public ResponseEntity<List<PedidoDTO>> misPedidos(Principal principal) {
        return ResponseEntity.ok(pedidoService.misPedidos(principal.getName()));
    }

    @GetMapping("/{numero}")
    public ResponseEntity<?> obtener(@PathVariable String numero, Principal principal) {
        try {
            return ResponseEntity.ok(pedidoService.obtenerPropio(numero, principal.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
