package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.application.ComponentePcExtractor.Tipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Corre el extractor de "Armá tu PC" contra el catálogo público real ({@code GET /api/productos})
 * y reporta cuánto dato de compatibilidad se pudo leer, con ejemplos de lo que quedó en null.
 * No forma parte de la suite normal: necesita el archivo y el catálogo cambia todo el tiempo.
 * Para correrlo:
 * {@code curl -s --compressed https://www.futuratecno.com.ar/api/productos -o /tmp/catalogo.json}
 * y {@code mvn test -Dtest=ComponentePcExtractorCatalogoRealTest -Dcatalogo=/tmp/catalogo.json}
 */
@EnabledIfSystemProperty(named = "catalogo", matches = ".+")
class ComponentePcExtractorCatalogoRealTest {

    private record Item(Tipo tipo, String categoria, String modelo, String specs) {}

    @Test
    void reportarCobertura() throws Exception {
        JsonNode catalogo = new ObjectMapper().readTree(new File(System.getProperty("catalogo")));
        List<Item> items = new ArrayList<>();
        Map<String, Integer> tipoPorCategoria = new TreeMap<>();
        for (JsonNode n : catalogo) {
            String modelo = n.path("modelo").asText("");
            String padre = n.path("categoriaPadre").asText(null);
            String hoja = n.path("categoria").asText(null);
            Tipo tipo = ComponentePcExtractor.tipo(modelo, padre, hoja);
            if (tipo == null) continue;
            String specs = n.path("variantes").path(0).path("especificaciones").asText("");
            items.add(new Item(tipo, hoja, modelo, specs));
            tipoPorCategoria.merge(tipo + " ← " + padre + " > " + hoja, 1, Integer::sum);
        }

        System.out.println("\n=== Tipo asignado por categoría de origen ===");
        tipoPorCategoria.forEach((k, v) -> System.out.println(v + "\t" + k));

        reportar(items, Tipo.PROCESADOR, "socket", i -> ComponentePcExtractor.socket(i.tipo, i.modelo, i.specs));
        reportar(items, Tipo.PROCESADOR, "video", i -> ComponentePcExtractor.videoIntegrado(i.modelo, i.specs));
        reportar(items, Tipo.PROCESADOR, "cooler", i -> ComponentePcExtractor.incluyeCooler(i.modelo, i.specs));
        reportar(items, Tipo.MOTHER, "socket", i -> ComponentePcExtractor.socket(i.tipo, i.modelo, i.specs));
        reportar(items, Tipo.MOTHER, "ram", i -> ComponentePcExtractor.tipoRam(i.tipo, i.modelo, i.specs, i.categoria,
                ComponentePcExtractor.socket(i.tipo, i.modelo, i.specs)));
        reportar(items, Tipo.MOTHER, "formato", i -> ComponentePcExtractor.formato(i.tipo, i.modelo, i.specs));
        reportar(items, Tipo.MOTHER, "ranuras", i -> ComponentePcExtractor.ranurasRam(i.modelo, i.specs,
                ComponentePcExtractor.formato(i.tipo, i.modelo, i.specs)));
        reportar(items, Tipo.MEMORIA, "modulos", i -> ComponentePcExtractor.modulosPorUnidad(i.modelo));
        reportar(items, Tipo.MEMORIA, "ram", i -> ComponentePcExtractor.tipoRam(i.tipo, i.modelo, i.specs, i.categoria, null));
        reportar(items, Tipo.FUENTE, "watts", i -> ComponentePcExtractor.potenciaW(i.modelo, i.specs));
        reportar(items, Tipo.VIDEO, "fuenteRecomendada", i -> ComponentePcExtractor.fuenteRecomendadaW(i.modelo, i.specs));
        reportar(items, Tipo.GABINETE, "formato", i -> ComponentePcExtractor.formato(i.tipo, i.modelo, i.specs));
        items.stream().filter(i -> i.tipo == Tipo.COOLER || i.tipo == Tipo.ALMACENAMIENTO)
                .collect(java.util.stream.Collectors.groupingBy(Item::tipo, java.util.stream.Collectors.counting()))
                .forEach((t, c) -> System.out.println("\n" + t + ": " + c));
    }

    private void reportar(List<Item> items, Tipo tipo, String dato, Function<Item, Object> f) {
        List<Item> deTipo = items.stream().filter(i -> i.tipo == tipo).toList();
        Map<String, Integer> valores = new TreeMap<>();
        List<String> sinDato = new ArrayList<>();
        for (Item i : deTipo) {
            Object v = f.apply(i);
            valores.merge(String.valueOf(v), 1, Integer::sum);
            if (v == null && sinDato.size() < 6) sinDato.add(i.modelo);
        }
        System.out.println("\n=== " + tipo + " · " + dato + " (" + deTipo.size() + ") " + valores);
        sinDato.forEach(m -> System.out.println("   null: " + m));
    }
}
