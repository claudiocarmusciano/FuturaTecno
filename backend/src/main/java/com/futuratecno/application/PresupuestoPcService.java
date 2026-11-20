package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.domain.PresupuestoPc;
import com.futuratecno.domain.Usuario;
import com.futuratecno.infrastructure.PresupuestoPcRepository;
import com.futuratecno.infrastructure.UsuarioRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Presupuestos de "Armá tu PC" de los clientes registrados; el admin los ve todos. */
@Service
public class PresupuestoPcService {
    static final int MAXIMO_POR_USUARIO = 50;
    private static final int MAX_JSON = 20_000;

    private final PresupuestoPcRepository repo;
    private final UsuarioRepository usuarios;
    private final ObjectMapper mapper = new ObjectMapper();

    public PresupuestoPcService(PresupuestoPcRepository repo, UsuarioRepository usuarios) {
        this.repo = repo;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> mios(String email) {
        return repo.findByUsuarioIdOrderByUpdatedAtDesc(usuario(email).getId()).stream().map(p -> vista(p, false)).toList();
    }

    @Transactional
    public Map<String, Object> guardar(String email, Long id, Map<String, Object> body) {
        Usuario u = usuario(email);
        PresupuestoPc p;
        if (id == null) {
            if (repo.countByUsuarioId(u.getId()) >= MAXIMO_POR_USUARIO)
                throw new IllegalArgumentException("Llegaste al máximo de " + MAXIMO_POR_USUARIO + " presupuestos. Borrá alguno para guardar otro.");
            p = new PresupuestoPc();
            p.setUsuario(u);
        } else {
            p = repo.findByIdAndUsuarioId(id, u.getId()).orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado."));
        }
        String nombre = body.get("nombre") instanceof String s ? s.trim() : "";
        p.setNombre(nombre.isEmpty() ? "Mi PC" : nombre.length() > 120 ? nombre.substring(0, 120) : nombre);
        p.setSeleccion(json(body.get("seleccion"), "la selección"));
        p.setResumen(json(body.get("resumen"), "el resumen"));
        p.setTotalArs(numero(body.get("totalArs"), true));
        p.setTotalUsd(numero(body.get("totalUsd"), false));
        return vista(repo.save(p), false);
    }

    @Transactional
    public void borrar(String email, Long id) {
        PresupuestoPc p = repo.findByIdAndUsuarioId(id, usuario(email).getId())
                .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado."));
        repo.delete(p);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> todos() {
        return repo.findTop500ByOrderByUpdatedAtDesc().stream().map(p -> vista(p, true)).toList();
    }

    private Usuario usuario(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    private String json(Object valor, String que) {
        if (valor == null) throw new IllegalArgumentException("Falta " + que + ".");
        try {
            String s = mapper.writeValueAsString(valor);
            if (s.length() > MAX_JSON) throw new IllegalArgumentException("El presupuesto es demasiado grande.");
            return s;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Formato inválido en " + que + ".");
        }
    }

    private static BigDecimal numero(Object valor, boolean obligatorio) {
        if (valor == null && !obligatorio) return null;
        try {
            BigDecimal n = new BigDecimal(String.valueOf(valor));
            if (n.signum() < 0 || n.compareTo(new BigDecimal("1000000000000")) >= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Total inválido.");
        }
    }

    private Map<String, Object> vista(PresupuestoPc p, boolean conCliente) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("nombre", p.getNombre());
        m.put("seleccion", leer(p.getSeleccion()));
        m.put("resumen", leer(p.getResumen()));
        m.put("totalArs", p.getTotalArs());
        m.put("totalUsd", p.getTotalUsd());
        m.put("creado", p.getCreatedAt());
        m.put("actualizado", p.getUpdatedAt());
        if (conCliente) {
            Usuario u = p.getUsuario();
            String nombre = ((u.getNombre() == null ? "" : u.getNombre()) + " " + (u.getApellido() == null ? "" : u.getApellido())).trim();
            m.put("cliente", nombre.isEmpty() ? u.getEmail() : nombre);
            m.put("email", u.getEmail());
            m.put("celular", u.getCelular());
        }
        return m;
    }

    private JsonNode leer(String s) {
        try { return mapper.readTree(s); } catch (Exception e) { return null; }
    }
}
