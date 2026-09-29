package com.futuratecno.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Atributos filtrables de un producto según su categoría, leídos del nombre y la ficha. No se
 * guardan: se calculan al armar el catálogo, así una regla nueva aplica a todo sin migrar datos.
 *
 * <p>Reglas acordadas con el usuario (2026-09-29): un producto sin el dato no aparece cuando se
 * filtra por ese atributo, pero siempre aparece sin filtro. El color es la excepción: si no se
 * sabe (no lo dice, o lista varios) es una opción más, "Color a consultar".
 */
public final class FiltrosCategoria {

    /** Grupo de filtros; se decide por la categoría del producto. */
    public enum Grupo { IPHONE, CELULAR }

    public record Definicion(String clave, String nombre) {}

    public static final String COLOR_A_CONSULTAR = "Color a consultar";

    public static final Map<Grupo, List<Definicion>> DEFINICIONES = Map.of(
            Grupo.IPHONE, List.of(new Definicion("generacion", "Modelo"), new Definicion("version", "Versión"),
                    new Definicion("capacidad", "Capacidad"), new Definicion("color", "Color")),
            Grupo.CELULAR, List.of(new Definicion("capacidad", "Capacidad"), new Definicion("ram", "Memoria RAM"),
                    new Definicion("red", "Red"), new Definicion("color", "Color")));

    /** Todas las claves de filtro de todos los grupos (las que acepta la URL). */
    public static final Set<String> CLAVES = DEFINICIONES.values().stream().flatMap(List::stream)
            .map(Definicion::clave).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final IdentidadProductoService IDENTIDAD = new IdentidadProductoService();

    private FiltrosCategoria() {}

    /** El grupo de una categoría por su ruta ("Apple > iPhone", "Celulares"); null si no tiene filtros. */
    public static Grupo grupoDe(String rutaCategoria) {
        if (rutaCategoria == null) return null;
        String r = rutaCategoria.trim().toLowerCase(Locale.ROOT);
        if (r.equals("apple > iphone")) return Grupo.IPHONE;
        if (r.equals("celulares")) return Grupo.CELULAR;
        return null;
    }

    /** Los atributos del producto para su grupo. Una clave ausente = el dato no se conoce. */
    public static Map<String, String> atributos(Grupo grupo, String marca, String modelo, String especificaciones) {
        Map<String, String> at = new LinkedHashMap<>();
        if (grupo == null || modelo == null) return at;
        String m = modelo.trim();
        if (grupo == Grupo.IPHONE) {
            Matcher g = GENERACION.matcher(m);
            if (g.find()) {
                at.put("generacion", "iPhone " + g.group(1));
                at.put("version", version(g.group(2)));
            } else if (AIR.matcher(m).find()) {
                // El iPhone Air salió con la línea 17 y no lleva número en el nombre.
                at.put("generacion", "iPhone 17");
                at.put("version", "Air");
            }
            String cap = capacidadIphone(m);
            if (cap != null) at.put("capacidad", cap);
        } else {
            var r = IDENTIDAD.resolverGuardado(marca == null ? "" : marca, m, especificaciones, "Celulares");
            String alm = r.atributos().get("almacenamiento_gb");
            String ram = r.atributos().get("ram_gb");
            // Samsung y Xiaomi escriben RAM+almacenamiento como "8+256", que la identidad no lee.
            Matcher mas = RAM_MAS_ALMACENAMIENTO.matcher(m);
            if (mas.find()) {
                if (ram == null) ram = mas.group(1);
                if (alm == null) alm = mas.group(2).equals("1") ? "1024" : mas.group(2);
            }
            if (alm != null) at.put("capacidad", gb(alm));
            if (ram != null) at.put("ram", gb(ram));
            String texto = (m + " " + (especificaciones == null ? "" : especificaciones)).toUpperCase(Locale.ROOT);
            if (CINCO_G.matcher(texto).find()) at.put("red", "5G");
            else if (CUATRO_G.matcher(texto).find()) at.put("red", "4G");
        }
        at.put("color", color(m, especificaciones));
        return at;
    }

    // ------------------------------------------------------------------ iPhone

    private static final Pattern GENERACION = Pattern.compile(
            "(?i)\\biphone\\s*(1\\d)\\s*(e\\b|pro\\s*max|pro|plus|air|mini)?");
    private static final Pattern AIR = Pattern.compile("(?i)\\biphone\\s+air\\b");

    private static String version(String v) {
        if (v == null) return "Estándar";
        String s = v.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return switch (s) {
            case "e" -> "e";
            case "pro max" -> "Pro Max";
            case "pro" -> "Pro";
            case "plus" -> "Plus";
            case "air" -> "Air";
            case "mini" -> "mini";
            default -> "Estándar";
        };
    }

    private static final Pattern CAPACIDAD = Pattern.compile("(?i)\\b(\\d{1,4})\\s?(GB|TB)\\b");
    /** "8+256", "12+512", "16+1TB": RAM + almacenamiento. */
    private static final Pattern RAM_MAS_ALMACENAMIENTO = Pattern.compile("(?i)\\b(\\d{1,2})\\s?\\+\\s?(\\d{1,4})(?:\\s?(?:GB|TB))?\\b");

    /** En un iPhone la capacidad es la única cifra en GB/TB del nombre (no se vende por RAM). */
    private static String capacidadIphone(String modelo) {
        Matcher c = CAPACIDAD.matcher(modelo);
        String out = null;
        while (c.find()) {
            int n = Integer.parseInt(c.group(1));
            boolean tb = c.group(2).equalsIgnoreCase("TB");
            if (tb || n >= 64) out = tb ? n + "TB" : n + "GB";
        }
        return out;
    }

    private static String gb(String valor) {
        int n = Integer.parseInt(valor);
        return n >= 1024 && n % 1024 == 0 ? (n / 1024) + "TB" : n + "GB";
    }

    private static final Pattern CINCO_G = Pattern.compile("\\b5G\\b");
    private static final Pattern CUATRO_G = Pattern.compile("\\b4G\\b|\\bLTE\\b");

    // ------------------------------------------------------------------ Color

    /** Lo que puede venir después de la capacidad y no es un color. */
    private static final Pattern NO_COLOR = Pattern.compile(
            "(?i)\\([^)]*\\)|\\b(e-?sim|dual\\s*sim|sim|nfc|magcharge|ram|nuevo|nueva|activado|sellado|libre|"
            + "version|versión|global|usa|original|con|de|y|ds|5g|4g|lte|wifi|\\+?cell)\\b|\\S*\\d\\S*|[\"”'/+]");

    /**
     * El color del producto, como lo nombra el fabricante ("Glacier", "Lily Pad", "Black"). Sale
     * de lo que sigue a la capacidad en el nombre; si ahí no hay nada, de los colores que el
     * vocabulario de la identidad reconoce en nombre + ficha. Un color en castellano se pasa al
     * inglés ("Negro" → "Black"). Si no dice o lista varios: {@link #COLOR_A_CONSULTAR}.
     */
    static String color(String modelo, String especificaciones) {
        String cola = colaDespuesDeCapacidad(modelo);
        if (cola != null) {
            if (cola.matches(".*(\\s-\\s|,).*")) return COLOR_A_CONSULTAR;   // "Black - Blue - Silver"
            String limpio = sinRepetir(NO_COLOR.matcher(cola).replaceAll(" ").replaceAll("[-·|]", " ")
                    .replaceAll("\\s+", " ").trim());
            if (!limpio.isEmpty() && limpio.split(" ").length <= 3) {
                Set<String> vocab = IdentidadProductoService.coloresDe(limpio);
                if (vocab.size() == 1 && IdentidadProductoService.sinColores(limpio).isBlank()) {
                    return IdentidadProductoService.nombreColorIngles(vocab.iterator().next());
                }
                return titulo(limpio);
            }
        }
        Set<String> vocab = IdentidadProductoService.coloresDe(modelo + " " + (especificaciones == null ? "" : especificaciones));
        return vocab.size() == 1 ? IdentidadProductoService.nombreColorIngles(vocab.iterator().next()) : COLOR_A_CONSULTAR;
    }

    /** Lo que viene después de la última capacidad del nombre ("256GB Cherry" → "Cherry"); null si no hay. */
    private static String colaDespuesDeCapacidad(String modelo) {
        Matcher c = CAPACIDAD.matcher(modelo);
        int fin = -1;
        while (c.find()) fin = c.end();
        Matcher mas = RAM_MAS_ALMACENAMIENTO.matcher(modelo);
        while (mas.find()) fin = Math.max(fin, mas.end());
        return fin < 0 ? null : modelo.substring(fin).trim();
    }

    /** "Glacier Glacier" → "Glacier"; "Jungle Breath Jungle Breath" → "Jungle Breath". */
    private static String sinRepetir(String s) {
        String[] w = s.split(" ");
        if (w.length % 2 == 0 && w.length > 0) {
            int h = w.length / 2;
            if (String.join(" ", java.util.Arrays.copyOfRange(w, 0, h))
                    .equalsIgnoreCase(String.join(" ", java.util.Arrays.copyOfRange(w, h, w.length)))) {
                return String.join(" ", java.util.Arrays.copyOfRange(w, 0, h));
            }
        }
        return s;
    }

    private static String titulo(String s) {
        StringBuilder out = new StringBuilder();
        for (String w : s.toLowerCase(Locale.ROOT).split(" ")) {
            if (w.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.toString();
    }
}
