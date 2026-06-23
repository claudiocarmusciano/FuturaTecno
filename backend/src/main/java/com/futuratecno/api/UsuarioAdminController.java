package com.futuratecno.api;

import com.futuratecno.api.dto.UsuarioDTO;
import com.futuratecno.infrastructure.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioAdminController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAdminController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista los clientes registrados (rol USUARIO): la base de emails. */
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listar() {
        List<UsuarioDTO> usuarios = usuarioRepository.findByRolOrderByCreatedAtDesc("USUARIO").stream()
                .map(u -> new UsuarioDTO(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }
}
