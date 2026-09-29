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
    public enum Grupo { IPHONE, CELULAR, MONITOR, PLACA_DE_VIDEO, MEMORIA }

    public record Definicion(String clave, String nombre) {}

    public static final String COLOR_A_CONSULTAR = "Color a consultar";

    public static final Map<Grupo, List<Definicion>> DEFINICIONES = Map.of(
            Grupo.IPHONE, List.of(new Definicion("generacion", "Modelo"), new Definicion("version", "Versión"),
                    new Definicion("capacidad", "Capacidad"), new Definicion("color", "Color")),
            Grupo.CELULAR, List.of(new Definicion("capacidad", "Capacidad"), new Definicion("ram", "Memoria RAM"),
                    new Definicion("red", "Red"), new Definicion("color", "Color")),
            Grupo.MONITOR, List.of(new Definicion("pulgadas", "Pulgadas"), new Definicion("hz", "Frecuencia"),
                    new Definicion("resolucion", "Resolución"), new Definicion("pantalla", "Pantalla")),
            Grupo.PLACA_DE_VIDEO, List.of(new Definicion("serie", "Serie"), new Definicion("chip", "Chip"),
                    new Definicion("vram", "Memoria")),
            Grupo.MEMORIA, List.of(new Definicion("ddr", "Tipo"), new Definicion("formato", "Formato"),
                    new Definicion("capacidad", "Capacidad"), new Definicion("modulos", "Módulos"),
                    new Definicion("mhz", "Velocidad"), new Definicion("rgb", "Iluminación")));

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
        // Monitores y placas: la categoría entera y cada subcategoría. "Apple > Monitores" no entra.
        if (r.equals("monitores") || r.startsWith("monitores >")) return Grupo.MONITOR;
        if (r.equals("placas de video") || r.startsWith("placas de video >")) return Grupo.PLACA_DE_VIDEO;
        if (r.equals("memorias ram") || r.startsWith("memorias ram >")) return Grupo.MEMORIA;
        return null;
    }

    /** Los atributos del producto para su grupo. Una clave ausente = el dato no se conoce. */
    public static Map<String, String> atributos(Grupo grupo, String marca, String modelo, String especificaciones) {
        Map<String, String> at = new LinkedHashMap<>();
        if (grupo == null || modelo == null) return at;
        String m = modelo.trim();
        if (grupo == Grupo.MONITOR) return monitor(m, especificaciones);
        if (grupo == Grupo.PLACA_DE_VIDEO) return placaDeVideo(m, especificaciones);
        if (grupo == Grupo.MEMORIA) return memoria(m, especificaciones);
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

    // ------------------------------------------------------------------ Monitores

    /** 23.8", 27'', 24 pulgadas, 27 inch. */
    private static final Pattern PULGADAS = Pattern.compile(
            "(?i)\\b(\\d{2}(?:[.,]\\d{1,2})?)\\s*(?:\"|”|''|'|´|pulg(?:adas)?\\b|inch(?:es)?\\b|in\\b)");
    /**
     * LG y Raptor escriben el tamaño sin comillas ("MONITOR LG 27 ULTRAGEAR", "Hawk Eye 24"): una
     * cifra suelta de 15 a 65, que no esté pegada a letras (así "27GS60F" no cuenta) ni sea Hz/ms.
     */
    private static final Pattern PULGADAS_SUELTAS = Pattern.compile(
            "(?i)(?<![\\w.-])(\\d{2}(?:[.,]\\d)?)(?![\\w.,]|\\s?(?:hz|ms|gb|%))");
    private static final Pattern HZ = Pattern.compile("(?i)\\b(\\d{2,3})\\s?hz\\b");

    private static Map<String, String> monitor(String modelo, String especificaciones) {
        Map<String, String> at = new LinkedHashMap<>();
        String texto = modelo + " " + (especificaciones == null ? "" : especificaciones);
        for (Pattern patron : List.of(PULGADAS, PULGADAS_SUELTAS)) {
            Matcher p = patron.matcher(patron == PULGADAS ? texto : modelo);
            while (p.find() && !at.containsKey("pulgadas")) {
                double v = Double.parseDouble(p.group(1).replace(',', '.'));
                if (v >= 15 && v <= 65) at.put("pulgadas", formatoPulgadas(v));
            }
            if (at.containsKey("pulgadas")) break;
        }
        Matcher h = HZ.matcher(texto);
        int hz = 0;
        while (h.find()) hz = Math.max(hz, Integer.parseInt(h.group(1)));   // "60Hz / 75Hz": el máximo
        if (hz >= 50) at.put("hz", hz + " Hz");
        String res = resolucion(texto);
        if (res != null) at.put("resolucion", res);
        // Acordado con el usuario: si no dice "curvo", es plano.
        at.put("pantalla", texto.toLowerCase(Locale.ROOT).matches("(?s).*\\bcurv.*") ? "Curva" : "Plana");
        return at;
    }

    private static String formatoPulgadas(double v) {
        return (v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v)) + "\"";
    }

    private static String resolucion(String texto) {
        String t = texto.toUpperCase(Locale.ROOT);
        if (t.matches("(?s).*(\\b(4K|UHD|2160P)\\b|3840\\s?X\\s?2160).*")) return "4K";
        if (t.matches("(?s).*(5120\\s?X\\s?1440|\\bDQHD\\b).*")) return "Dual QHD";
        if (t.matches("(?s).*(3440\\s?X\\s?1440|\\bUWQHD\\b|\\bWQHD\\+?\\s?ULTRA).*")) return "UltraWide QHD";
        if (t.matches("(?s).*(\\b(QHD|WQHD|2K|1440P)\\b|2560\\s?X\\s?1440).*")) return "2K (QHD)";
        if (t.matches("(?s).*(\\bWFHD\\b|2560\\s?X\\s?1080).*")) return "UltraWide Full HD";
        if (t.matches("(?s).*(\\b(FHD|FULL\\s?HD|1080P)\\b|1920\\s?X\\s?1080).*")) return "Full HD";
        if (t.matches("(?s).*(\\bHD\\b|1366\\s?X\\s?768|1600\\s?X\\s?900|720P).*")) return "HD";
        return null;
    }

    // ------------------------------------------------------------------ Placas de video

    private static final Pattern NVIDIA = Pattern.compile(
            "(?i)\\b(RTX|GTX|GT)\\s?-?\\s?(\\d{3,4})\\s?(TI\\s?SUPER|TI|SUPER)?\\b");
    private static final Pattern RTX_PRO = Pattern.compile("(?i)\\bRTX\\s?(PRO\\s?\\d{4}|A\\d{3,4})\\b");
    private static final Pattern AMD = Pattern.compile("(?i)\\bRX\\s?-?\\s?(\\d{3,4})\\s?(XTX|XT|GRE)?\\b");
    private static final Pattern ARC = Pattern.compile("(?i)\\bARC\\s?([AB]\\d{3})\\b");
    /** 8GB, 8G, 2GD3 (MSI), 24Gb GDDR7. */
    private static final Pattern VRAM = Pattern.compile("(?i)\\bO?(\\d{1,2})\\s?G(?:B)?(?=\\s|D\\d|DDR|\\b)");   // "O8GB": ASUS antepone la O de OC
    private static final Set<Integer> VRAM_VALIDAS = Set.of(1, 2, 3, 4, 6, 8, 10, 11, 12, 16, 20, 24, 32, 48);

    private static Map<String, String> placaDeVideo(String modelo, String especificaciones) {
        Map<String, String> at = new LinkedHashMap<>();
        Matcher pro = RTX_PRO.matcher(modelo);
        Matcher n = NVIDIA.matcher(modelo);
        Matcher a = AMD.matcher(modelo);
        Matcher arc = ARC.matcher(modelo);
        if (pro.find()) {
            at.put("serie", "Profesional");
            at.put("chip", "RTX " + pro.group(1).toUpperCase(Locale.ROOT).replaceAll("\\s+", " "));
        } else if (modelo.toUpperCase(Locale.ROOT).contains("QUADRO") || modelo.toUpperCase(Locale.ROOT).contains("RADEON PRO")) {
            at.put("serie", "Profesional");
        } else if (n.find()) {
            String linea = n.group(1).toUpperCase(Locale.ROOT);
            String num = n.group(2);
            String suf = n.group(3) == null ? "" : " " + titulo(n.group(3).replaceAll("\\s+", " "));
            at.put("serie", linea.equals("GT") ? "GT" : linea + " " + num.substring(0, num.length() - 2));
            at.put("chip", linea + " " + num + suf.replace("Ti Super", "Ti SUPER").replace("Super", "SUPER"));
        } else if (a.find()) {
            String num = a.group(1);
            at.put("serie", "RX " + num.charAt(0) + "000".substring(0, num.length() - 1));
            at.put("chip", "RX " + num + (a.group(2) == null ? "" : " " + a.group(2).toUpperCase(Locale.ROOT)));
        } else if (arc.find()) {
            at.put("serie", "Arc " + arc.group(1).toUpperCase(Locale.ROOT).charAt(0));
            at.put("chip", "Arc " + arc.group(1).toUpperCase(Locale.ROOT));
        }
        Matcher v = VRAM.matcher(modelo + " " + (especificaciones == null ? "" : especificaciones));
        while (v.find()) {
            int gb = Integer.parseInt(v.group(1));
            if (VRAM_VALIDAS.contains(gb)) { at.put("vram", gb + "GB"); break; }
        }
        return at;
    }

    // ------------------------------------------------------------------ Memorias RAM

    private static final Pattern DDR = Pattern.compile("(?i)\\bDDR\\s?([2-5])L?\\b");
    /** "2x16GB", "(2 x 8GB)", "Kit 2x8". */
    private static final Pattern KIT = Pattern.compile("(?i)\\b(\\d)\\s?x\\s?(\\d{1,3})\\s?G(?:B)?\\b");
    /** "16GB", "16 Gb"; no "16Gbit", que es la densidad del chip. */
    private static final Pattern GB_MODULO = Pattern.compile("(?i)\\b(\\d{1,3})\\s?GB?\\b(?!it)");
    /** Ficha de Elit: "Unidades x kit: 2". */
    private static final Pattern UNIDADES_KIT = Pattern.compile("(?i)unidades\\s*x\\s*kit\\s*:\\s*(\\d)");
    private static final Pattern MHZ = Pattern.compile("(?i)\\b(\\d{4})\\s?(?:MHZ|MT/?S)?\\b");

    private static Map<String, String> memoria(String modelo, String especificaciones) {
        Map<String, String> at = new LinkedHashMap<>();
        String texto = modelo + " " + (especificaciones == null ? "" : especificaciones);
        Matcher d = DDR.matcher(texto);
        if (d.find()) at.put("ddr", "DDR" + d.group(1));
        at.put("formato", texto.toUpperCase(Locale.ROOT).matches("(?s).*(SO-?DIMM|NOTEBOOK|LAPTOP).*") ? "Notebook (SODIMM)" : "PC");
        Matcher k = KIT.matcher(texto);
        Matcher u = UNIDADES_KIT.matcher(texto);
        int modulos, capacidad = 0;
        if (k.find()) {   // "(2x16GB)": lo más preciso
            modulos = Integer.parseInt(k.group(1));
            capacidad = modulos * Integer.parseInt(k.group(2));
        } else {
            // Sin "NxM", la cifra del nombre es el TOTAL (así se vende: "32GB" en un kit de 2x16).
            Matcher g = GB_MODULO.matcher(modelo);   // del nombre: la ficha repite datos del chip
            while (g.find()) {
                int v = Integer.parseInt(g.group(1));
                if (v >= 2 && v <= 256 && Integer.bitCount(v) <= 2) { capacidad = v; break; }
            }
            if (u.find()) modulos = Integer.parseInt(u.group(1));
            // "Kit" en el NOMBRE sin detalle: no se sabe de cuántos. En la ficha no cuenta: Hiksemi
            // dice "RAM individual y en kit disponibles" en todas.
            else modulos = modelo.toUpperCase(Locale.ROOT).matches("(?s).*\\b(KIT|DUAL\\s?CHANNEL)\\b.*") ? 0 : 1;
        }
        if (capacidad > 0) at.put("capacidad", capacidad + "GB");
        if (modulos == 1) at.put("modulos", "1 módulo");
        else if (modulos > 1) at.put("modulos", "Kit de " + modulos);
        Matcher m = MHZ.matcher(texto);
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 1066 && v <= 9600) { at.put("mhz", v + " MHz"); break; }
        }
        boolean rgb = texto.toUpperCase(Locale.ROOT).matches("(?s).*(\\bRGB\\b|ILUMINACI[OÓ]N:\\s*S[IÍ]\\b).*");
        at.put("rgb", rgb ? "Con RGB" : "Sin RGB");
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
    /** "8+256", "8/256", "12+512", "16+1TB": RAM + almacenamiento. */
    private static final Pattern RAM_MAS_ALMACENAMIENTO = Pattern.compile("(?i)\\b(\\d{1,2})\\s?[+/]\\s?(\\d{1,4})(?:\\s?(?:GB|TB))?\\b");

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
                return nombreFabricante(titulo(limpio));
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

    /** Nombres de fabricante que a veces llegan traducidos: se muestran como los llama la marca. */
    private static final Map<String, String> EN_INGLES = Map.of("Glaciar", "Glacier");

    private static String nombreFabricante(String color) {
        return EN_INGLES.getOrDefault(color, color);
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
