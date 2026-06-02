package com.futuratecno.api;

import com.futuratecno.api.dto.ProveedorDTO;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ProveedorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/proveedores")
@CrossOrigin(origins = "*")
public class ProveedorController {
    private final ProveedorRepository proveedorRepository;

    public ProveedorController(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProveedorDTO>> listar() {
        List<Proveedor> proveedores = proveedorRepository.findAll();
        List<ProveedorDTO> dtos = proveedores.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> obtener(@PathVariable Long id) {
        return proveedorRepository.findById(id)
            .map(p -> ResponseEntity.ok(toDTO(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> crear(@RequestBody ProveedorDTO dto) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre());
        proveedor.setMargenPorcentaje(dto.getMargenPorcentaje());
        proveedor.setCostoFleteUsd(dto.getCostoFleteUsd());
        proveedor.setActivo(true);

        Proveedor guardado = proveedorRepository.save(proveedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> actualizar(@PathVariable Long id, @RequestBody ProveedorDTO dto) {
        return proveedorRepository.findById(id)
            .map(proveedor -> {
                proveedor.setNombre(dto.getNombre());
                proveedor.setMargenPorcentaje(dto.getMargenPorcentaje());
                proveedor.setCostoFleteUsd(dto.getCostoFleteUsd());
                Proveedor actualizado = proveedorRepository.save(proveedor);
                return ResponseEntity.ok(toDTO(actualizado));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return proveedorRepository.findById(id)
            .map(proveedor -> {
                proveedor.setActivo(false);
                proveedorRepository.save(proveedor);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private ProveedorDTO toDTO(Proveedor proveedor) {
        return new ProveedorDTO(
            proveedor.getId(),
            proveedor.getNombre(),
            proveedor.getMargenPorcentaje(),
            proveedor.getCostoFleteUsd(),
            proveedor.getActivo(),
            proveedor.getCreatedAt(),
            proveedor.getUpdatedAt()
        );
    }
}
