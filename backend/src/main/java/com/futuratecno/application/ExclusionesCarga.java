package com.futuratecno.application;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lo que no se carga en la tienda por ninguna vía (Cargar por JSON, Flor Qtech, Kadabra), por pedido
 * del usuario (2026-10-23): accesorios chicos, lavarropas y réplicas. Los ya publicados no se tocan.
 *
 * <p>Los accesorios se reconocen por cómo EMPIEZA el modelo (las primeras palabras), igual que
 * {@link ClasificadorPorNombre}: "Notebook … con cargador" o "iPhone … compatible MagSafe" mencionan
 * un accesorio pero no lo son. Los de valor (Apple Pencil, Magic Keyboard, joysticks, AirTag) se cargan.
 */
final class ExclusionesCarga {
    private ExclusionesCarga() {}

    private static final Pattern ACCESORIO = Pattern.compile(
            "^(ACCESORIOS? )?(FUNDAS?|CASES?|ESTUCHES?|CARCASAS?|COVER|VIDRIOS?|TEMPLADOS?|LAMINAS? PROTECTORAS?|MICAS?|PROTECTOR(ES)?|"
            + "CABLES?|CARGADOR(ES)?|ADAPTADOR(ES)?|ENCHUFES?|MAGSAFE|SOPORTES?|HOLDER|POPSOCKETS?|CORREAS?|MALLAS?|"
            + "LANYARD|COLGANTES?|STRAPS?|BANDS?)\\b");
    private static final Pattern LAVARROPAS = Pattern.compile("LAVARROPA|LAVASECARROPA|SECARROPA|LAVA ?Y ?SECA|LAVADORA|SECADORA DE ROPA");
    private static final Pattern REPLICA = Pattern.compile(
            "REPLICA|\\bCLON(E|ICO|ES)?\\b|\\bAAA\\+?(?=\\s|$)|\\b1\\s?[:.]\\s?1\\b|\\bCOPIA\\b|NO ORIGINAL|ALTERNATIV[OA]");

    /** El motivo para no cargarlo, o null si se carga. */
    static String motivo(String marca, String modelo, String modeloExacto, String listado) {
        String m = normalizar(modelo);
        String sinMarca = marca != null && m.startsWith(normalizar(marca) + " ") ? m.substring(normalizar(marca).length() + 1) : m;
        String encabezado = Arrays.stream(sinMarca.split(" ")).limit(4).collect(Collectors.joining(" "));
        String todo = Stream.of(marca, modelo, modeloExacto, listado).filter(Objects::nonNull)
                .map(ExclusionesCarga::normalizar).collect(Collectors.joining(" "));
        if (LAVARROPAS.matcher(todo).find()) return "No se cargan lavarropas.";
        if (REPLICA.matcher(todo).find()) return "No se cargan réplicas.";
        if (ACCESORIO.matcher(encabezado).find()) return "No se cargan accesorios chicos (fundas, vidrios, cables, cargadores…).";
        return null;
    }

    static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9+:./ ]", " ").replaceAll("\\s+", " ").trim();
    }
}
