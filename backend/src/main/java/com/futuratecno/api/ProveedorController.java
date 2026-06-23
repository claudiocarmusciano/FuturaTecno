package com.futuratecno.api;

import com.futuratecno.api.dto.ProveedorDTO;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ProductoRepository;
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
    private final ProductoRepository productoRepository;

    public ProveedorController(ProveedorRepository proveedorRepository, ProductoRepository productoRepository) {
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProveedorDTO>> listar() {
        List<Proveedor> proveedores = proveedorRepository.findAll();
        List<ProveedorDTO> dtos = proveedores.stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
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
    public ResponseEntity<?> crear(@RequestBody ProveedorDTO dto) {
        // Si ya existe un proveedor con ese nombre: si está activo, es duplicado;
        // si está borrado (inactivo), lo reactivamos con los nuevos valores.
        var existente = proveedorRepository.findByNombreIgnoreCase(dto.getNombre().trim());
        if (existente.isPresent()) {
            Proveedor p = existente.get();
            if (Boolean.TRUE.equals(p.getActivo())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Ya existe un proveedor activo con ese nombre.");
            }
            p.setActivo(true);
            p.setMargenPorcentaje(dto.getMargenPorcentaje());
            p.setFletePorcentaje(dto.getFletePorcentaje());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(proveedorRepository.save(p)));
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre().trim());
        proveedor.setMargenPorcentaje(dto.getMargenPorcentaje());
        proveedor.setFletePorcentaje(dto.getFletePorcentaje());
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
                proveedor.setFletePorcentaje(dto.getFletePorcentaje());
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
                // También damos de baja sus productos para que no queden "huérfanos" en el catálogo.
                List<Producto> productos = productoRepository.findByProveedorIdAndActivo(id, true);
                for (Producto p : productos) {
                    p.setActivo(false);
                    productoRepository.save(p);
                }
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private ProveedorDTO toDTO(Proveedor proveedor) {
        return new ProveedorDTO(
            proveedor.getId(),
            proveedor.getNombre(),
            proveedor.getMargenPorcentaje(),
            proveedor.getFletePorcentaje(),
            proveedor.getActivo(),
            proveedor.getCreatedAt(),
            proveedor.getUpdatedAt()
        );
    }
}
