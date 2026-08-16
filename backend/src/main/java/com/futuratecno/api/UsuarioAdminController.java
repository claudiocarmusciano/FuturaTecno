package com.futuratecno.api;

import com.futuratecno.api.dto.UsuarioDTO;
import com.futuratecno.application.AuthService;
import com.futuratecno.domain.Usuario;
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
    private final AuthService authService;

    public UsuarioAdminController(UsuarioRepository usuarioRepository, AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    /** Lista los clientes registrados (rol USUARIO): la base de emails. */
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listar() {
        List<UsuarioDTO> usuarios = usuarioRepository.findByRolOrderByCreatedAtDesc("USUARIO").stream()
                .map(this::aDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping("/{id}/validar-whatsapp")
    public ResponseEntity<?> validarWhatsapp(@PathVariable Long id) {
        try {
            authService.validarWhatsappManual(id);
            return usuarioRepository.findById(id).map(this::aDto).map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/validar-instagram")
    public ResponseEntity<?> validarInstagram(@PathVariable Long id) {
        try {
            authService.validarInstagramManual(id);
            return usuarioRepository.findById(id).map(this::aDto).map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    private UsuarioDTO aDto(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO(u.getId(), u.getEmail(), u.getNombre(), u.getApellido(),
                u.getCelular(), u.getRol(), u.getCreatedAt());
        dto.setEmailVerificado(u.getEmailVerificado());
        dto.setDni(u.getDni());
        dto.setFechaNacimiento(u.getFechaNacimiento());
        dto.setWhatsappVerificado(u.getWhatsappVerificado());
        dto.setWhatsappAgendado(u.getPasoWhatsappAgendado());
        dto.setWhatsappVerificacionCodigo(u.getWhatsappVerificacionCodigo());
        dto.setInstagramUsuario(u.getInstagramUsuario());
        dto.setInstagramCompletado(u.getPasoInstagramCompletado());
        dto.setInstagramVerificado(u.getInstagramVerificado());
        dto.setCodigoSorteo(u.getCodigoSorteo());
        dto.setBasesAceptadasEn(u.getBasesAceptadasEn());
        return dto;
    }
}
