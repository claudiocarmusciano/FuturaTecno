package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mide los filtros por categoría contra un volcado del catálogo real: cuántos productos quedan
 * con cada dato y qué valores salen. No corre en la suite normal.
 * {@code mvn test -Dtest=FiltrosCategoriaCatalogoRealTest -Dcatalogo=/ruta/catalogo.json}
 */
@EnabledIfSystemProperty(named = "catalogo", matches = ".+")
class FiltrosCategoriaCatalogoRealTest {

    @Test
    void reportar() throws Exception {
        JsonNode catalogo = new ObjectMapper().readTree(new File(System.getProperty("catalogo")));
        for (FiltrosCategoria.Grupo g : FiltrosCategoria.Grupo.values()) {
            int total = 0;
            Map<String, Integer> conDato = new TreeMap<>();
            Map<String, Map<String, Integer>> valores = new TreeMap<>();
            StringBuilder detalle = new StringBuilder();
            for (JsonNode n : catalogo) {
                if (FiltrosCategoria.grupoDe(n.path("actual").asText(null)) != g) continue;
                total++;
                var at = FiltrosCategoria.atributos(g, n.path("marca").asText(""), n.path("modelo").asText(""), null);
                at.forEach((k, v) -> {
                    if (!FiltrosCategoria.COLOR_A_CONSULTAR.equals(v)) conDato.merge(k, 1, Integer::sum);
                    valores.computeIfAbsent(k, x -> new TreeMap<>()).merge(v, 1, Integer::sum);
                });
                if (Boolean.getBoolean("detalle")) detalle.append(String.format("  %-60.60s %s%n", n.path("modelo").asText(), at));
            }
            System.out.printf("%n== %s: %d productos%n", g, total);
            for (var d : FiltrosCategoria.DEFINICIONES.get(g)) {
                System.out.printf("  %-11s %3d/%d con dato → %s%n", d.nombre(), conDato.getOrDefault(d.clave(), 0), total, valores.get(d.clave()));
            }
            System.out.print(detalle);
        }
    }
}
