package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.application.IdentidadProductoService.Resolucion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Corre las reglas de identidad contra el catálogo público real ({@code GET /api/productos}) y
 * reporta cuántos teléfonos se resuelven, por qué quedan en revisión y qué grupos comparten
 * identidad (candidatos a duplicado). No forma parte de la suite normal.
 * {@code curl -s --compressed https://www.futuratecno.com.ar/api/productos -o /tmp/catalogo.json}
 * {@code mvn test -Dtest=IdentidadProductoCatalogoRealTest -Dcatalogo=/tmp/catalogo.json}
 */
@EnabledIfSystemProperty(named = "catalogo", matches = ".+")
class IdentidadProductoCatalogoRealTest {

    @Test
    void reportar() throws Exception {
        IdentidadProductoService svc = new IdentidadProductoService();
        JsonNode catalogo = new ObjectMapper().readTree(new File(System.getProperty("catalogo")));
        int telefonos = 0, resueltos = 0;
        Map<String, Integer> motivos = new TreeMap<>();
        Map<String, List<String>> porClave = new TreeMap<>();
        List<String> ejemplosRevision = new ArrayList<>();
        for (JsonNode n : catalogo) {
            String specs = n.path("variantes").path(0).path("especificaciones").asText("");
            String cat = n.path("categoriaPadre").asText("") + " " + n.path("categoria").asText("");
            Resolucion r = svc.resolverGuardado(n.path("marca").asText(), n.path("modelo").asText(), specs, cat);
            if (!r.esTelefono()) continue;
            telefonos++;
            if (r.resuelta()) {
                resueltos++;
                porClave.computeIfAbsent(r.clave(), k -> new ArrayList<>()).add(n.path("id").asText() + " " + n.path("modelo").asText());
            } else {
                r.motivos().forEach(m -> motivos.merge(m.replaceAll("\\d+", "N"), 1, Integer::sum));
                if (ejemplosRevision.size() < 12) ejemplosRevision.add(n.path("marca").asText() + " | " + n.path("modelo").asText() + " → " + r.motivos());
            }
        }
        System.out.println("\n=== Teléfonos: " + telefonos + " · resueltos " + resueltos + " · en revisión " + (telefonos - resueltos));
        motivos.forEach((m, c) -> System.out.println(c + "\t" + m));
        ejemplosRevision.forEach(e -> System.out.println("   " + e));
        System.out.println("\n=== Claves compartidas por más de un producto (entre todos los proveedores):");
        porClave.entrySet().stream().filter(e -> e.getValue().size() > 1).limit(15)
                .forEach(e -> System.out.println(e.getKey() + "  ←  " + e.getValue()));
    }
}
