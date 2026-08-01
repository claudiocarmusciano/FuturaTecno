package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.domain.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corre el clasificador contra un volcado del catálogo real y reporta cobertura y precisión.
 * No forma parte de la suite normal: necesita el archivo y el catálogo cambia todo el tiempo.
 * Para correrlo: {@code mvn test -Dtest=ClasificadorPorNombreCatalogoRealTest -Dcatalogo=/ruta/catalogo.json}
 */
@EnabledIfSystemProperty(named = "catalogo", matches = ".+")
class ClasificadorPorNombreCatalogoRealTest {

    private final ClasificadorPorNombre clasificador = new ClasificadorPorNombre();

    @Test
    void reportarCoberturaYPrecision() throws Exception {
        JsonNode catalogo = new ObjectMapper().readTree(new File(System.getProperty("catalogo")));

        int sinCatTotal = 0, sinCatResueltos = 0;
        int conCatTotal = 0, aciertos = 0, errores = 0, noDispara = 0;
        Map<String, Integer> discrepancias = new HashMap<>();

        for (JsonNode n : catalogo) {
            Producto p = new Producto();
            p.setMarca(n.path("marca").asText(null));
            p.setModelo(n.path("modelo").asText(null));
            String predicho = clasificador.clasificar(p);

            if (!n.path("tieneCat").asBoolean()) {
                sinCatTotal++;
                if (predicho != null) sinCatResueltos++;
            } else {
                conCatTotal++;
                String actual = n.path("actual").asText(null);
                if (predicho == null) noDispara++;
                else if (predicho.equals(actual)) aciertos++;
                else {
                    errores++;
                    discrepancias.merge(actual + "  →  " + predicho, 1, Integer::sum);
                }
            }
        }

        int precision = (aciertos + errores) == 0 ? 0 : aciertos * 100 / (aciertos + errores);
        System.out.printf("%nOBJETIVO (sin categoría): resuelve %d/%d (%d%%)%n",
                sinCatResueltos, sinCatTotal, sinCatTotal == 0 ? 0 : sinCatResueltos * 100 / sinCatTotal);
        System.out.printf("CONTROL (ya categorizados, %d): acierta %d, falla %d, no dispara %d → precisión %d%%%n",
                conCatTotal, aciertos, errores, noDispara, precision);
        System.out.println("\nDiscrepancias:");
        discrepancias.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(15)
                .forEach(e -> System.out.printf("  %3d  %s%n", e.getValue(), e.getKey()));

        assertTrue(sinCatResueltos * 100 / Math.max(sinCatTotal, 1) >= 90,
                "La cobertura sobre los productos sin categoría no debería bajar del 90%");
        assertTrue(precision >= 80, "La precisión contra los ya categorizados no debería bajar del 80%");
    }
}
