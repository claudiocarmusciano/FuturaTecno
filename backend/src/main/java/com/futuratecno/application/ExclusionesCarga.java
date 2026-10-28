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
 * del usuario (2026-10-23): accesorios chicos, lavarropas y réplicas; electrodomésticos (2026-10-27);
 * cuidado personal, la marca Dyson y los teclados de iPad (2026-10-28).
 * Los ya publicados no se tocan.
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
    /**
     * Electrodomésticos, por cómo empieza el modelo (como los accesorios: "Smart TV … con horno" no
     * existe, pero "Notebook … ventilador" sí). El ventilador de PC no cuenta.
     */
    private static final Pattern ELECTRODOMESTICO = Pattern.compile(
            "^(SMART |MI |MINI |DIGITAL |ELECTRIC[AO]S? )*(HELADERAS?|FREEZERS?|MICROONDAS|HORNOS?|ANAFES?|COCINAS?|FREIDORAS?|AIR ?FRYERS?|CAFETERAS?|LICUADORAS?|"
            + "BATIDORAS?|PROCESADORAS?|MIXERS?|MINIPIMERS?|TOSTADORAS?|SANDWICHERAS?|WAFLERAS?|PAVAS?|JARRAS? ELECTRICAS?|"
            + "HERVIDOR(ES)?|EXPRIMIDOR(ES)?|JUGUERAS?|ARROCERAS?|PANIFICADORAS?|YOGURTERAS?|OLLAS? ELECTRICAS?|ASPIRADORAS?|"
            + "ROBOTS? ASPIRADORE?S?|LAVAVAJILLAS|LAVAPLATOS|AIRES? ACONDICIONADOS?|ACONDICIONADOR(ES)?|CALEFACTOR(ES)?|"
            + "CALOVENTOR(ES)?|ESTUFAS?|TERMOTANQUES?|CALEFONES?|PURIFICADOR(ES)?|HUMIDIFICADOR(ES)?|DESHUMIDIFICADOR(ES)?|"
            + "PLANCHAS?( A VAPOR| DE ROPA)?|ROBOT VACUUM|VACUUM|KETTLE|BLENDER|COFFEE MAKER|AIR PURIFIER|HEATER|EXTRACTOR(ES)?|CAMPANAS?|DISPENSERS? DE AGUA|"
            + "VENTILADOR(ES)?(?!.*\\b(PC|CPU|GABINETE|RGB|ARGB|\\d+ ?MM)\\b))\\b");
    /** Cuidado personal: secadores y planchitas de pelo, afeitadoras, cortadoras, depiladoras. */
    private static final Pattern CUIDADO_PERSONAL = Pattern.compile(
            "^(SMART |MINI |DIGITAL |ELECTRIC[AO]S? |PROFESIONAL(ES)? )*(SECADOR(ES)?\\b(?! DE ROPA)|SECADORAS? DE (PELO|CABELLO)|PLANCHITAS?|"
            + "PLANCHAS? (DE|PARA) (PELO|CABELLO)|ALISADOR(ES|AS?)?|RIZADOR(ES|AS?)?|BUCLERAS?|ONDULADOR(ES|AS?)?|AFEITADORAS?|"
            + "MAQUINAS? (DE|PARA) (AFEITAR|CORTAR)|CORTADORAS? (DE|PARA) (PELO|CABELLO|BARBA)|CORTAPELOS?|RECORTADORAS?|"
            + "TRIMMERS?|DEPILADORAS?|CEPILLOS? (SECADOR|ALISADOR|ELECTRICO|DENTAL|DE DIENTES)|HAIR ?(DRYER|STRAIGHTENER|CLIPPER)S?|"
            + "SHAVERS?|GROOMERS?|EPILATORS?)");
    /** Teclados para iPad (Magic Keyboard, Smart Keyboard Folio…); los de Mac se siguen cargando. */
    private static final Pattern TECLADO = Pattern.compile("^(APPLE )?(MAGIC |SMART )?(KEYBOARD|TECLADO)\\b");
    private static final Pattern REPLICA = Pattern.compile(
            "REPLICA|\\bCLON(E|ICO|ES)?\\b|\\bAAA\\+?(?=\\s|$)|\\b1\\s?[:.]\\s?1\\b|\\bCOPIA\\b|NO ORIGINAL|ALTERNATIV[OA]");

    /** El motivo para no cargarlo, o null si se carga. */
    static String motivo(String marca, String modelo, String modeloExacto, String listado, String categoria) {
        String m = normalizar(modelo);
        String sinMarca = marca != null && m.startsWith(normalizar(marca) + " ") ? m.substring(normalizar(marca).length() + 1) : m;
        // Lo que viene en el combo no dice qué es el artículo: "A07 + Cargador" es un teléfono.
        String encabezado = Arrays.stream(sinMarca.split(" \\+ ")[0].split(" ")).limit(4).collect(Collectors.joining(" "));
        String todo = Stream.of(marca, modelo, modeloExacto, listado).filter(Objects::nonNull)
                .map(ExclusionesCarga::normalizar).collect(Collectors.joining(" "));
        if (LAVARROPAS.matcher(todo).find()) return "No se cargan lavarropas.";
        if (REPLICA.matcher(todo).find()) return "No se cargan réplicas.";
        if (todo.matches(".*\\bDYSON\\b.*")) return "No se carga la marca Dyson.";
        if (CUIDADO_PERSONAL.matcher(encabezado).find()) return "No se carga cuidado personal.";
        if (TECLADO.matcher(encabezado).find() && todo.matches(".*\\bIPAD.*")) return "No se cargan teclados de iPad.";
        if (normalizar(categoria).startsWith("ELECTRODOMESTICOS") || ELECTRODOMESTICO.matcher(encabezado).find())
            return "No se cargan electrodomésticos.";
        if (ACCESORIO.matcher(encabezado).find()) return "No se cargan accesorios chicos (fundas, vidrios, cables, cargadores…).";
        return null;
    }

    static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9+:./ ]", " ").replaceAll("\\s+", " ").trim();
    }
}
