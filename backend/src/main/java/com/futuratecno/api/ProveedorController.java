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
@RequestMapping("/api/admin/proveedores")
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
        String codigo = normalizarCodigo(dto.getCodigo(), dto.getNombre());
        ResponseEntity<?> conflicto = validarCodigoUnico(codigo, existente.map(Proveedor::getId).orElse(null));
        if (conflicto != null) return conflicto;

        if (existente.isPresent()) {
            Proveedor p = existente.get();
            if (Boolean.TRUE.equals(p.getActivo())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Ya existe un proveedor activo con ese nombre.");
            }
            p.setActivo(true);
            p.setCodigo(codigo);
            p.setMargenPorcentaje(dto.getMargenPorcentaje());
            p.setFletePorcentaje(dto.getFletePorcentaje());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(proveedorRepository.save(p)));
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre().trim());
        proveedor.setCodigo(codigo);
        proveedor.setMargenPorcentaje(dto.getMargenPorcentaje());
        proveedor.setFletePorcentaje(dto.getFletePorcentaje());
        proveedor.setActivo(true);

        Proveedor guardado = proveedorRepository.save(proveedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProveedorDTO dto) {
        String codigo = normalizarCodigo(dto.getCodigo(), dto.getNombre());
        ResponseEntity<?> conflicto = validarCodigoUnico(codigo, id);
        if (conflicto != null) return conflicto;

        return proveedorRepository.findById(id)
            .map(proveedor -> {
                proveedor.setNombre(dto.getNombre());
                proveedor.setCodigo(codigo);
                proveedor.setMargenPorcentaje(dto.getMargenPorcentaje());
                proveedor.setFletePorcentaje(dto.getFletePorcentaje());
                Proveedor actualizado = proveedorRepository.save(proveedor);
                return ResponseEntity.ok(toDTO(actualizado));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /** Mayúsculas + sin espacios; si viene vacío, se deriva del nombre (primeras 3 letras/números). */
    private String normalizarCodigo(String codigo, String nombre) {
        String c = codigo != null ? codigo.trim().toUpperCase() : "";
        if (c.isEmpty() && nombre != null) {
            c = nombre.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            c = c.length() > 3 ? c.substring(0, 3) : c;
        }
        return c;
    }

    /** Devuelve 409 si el código ya lo usa otro proveedor activo (excluyendo el que se está guardando). */
    private ResponseEntity<?> validarCodigoUnico(String codigo, Long idPropio) {
        if (codigo == null || codigo.isBlank()) return null;
        var otro = proveedorRepository.findByCodigoIgnoreCase(codigo);
        if (otro.isPresent() && Boolean.TRUE.equals(otro.get().getActivo()) && !otro.get().getId().equals(idPropio)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un proveedor activo con ese código.");
        }
        return null;
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
            proveedor.getCodigo(),
            proveedor.getMargenPorcentaje(),
            proveedor.getFletePorcentaje(),
            proveedor.getActivo(),
            proveedor.getCreatedAt(),
            proveedor.getUpdatedAt()
        );
    }
}
