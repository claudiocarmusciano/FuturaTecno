package com.futuratecno.application;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee del nombre y la ficha de un componente lo que "Armá tu PC" necesita para decidir
 * compatibilidad: socket, tipo de RAM, formato, watts. Ningún mayorista lo manda en campos
 * separados — Elit lo escribe en la ficha ("Socket: AM5."), Invid en el nombre ("AM5", "S1700").
 *
 * <p>Cuando el texto no lo dice, se deduce de lo que sí dice: el chipset de un mother fija el
 * socket (B650 → AM5) y la generación de un procesador también (Ryzen 7600 → AM5, i5-12400 →
 * LGA1700). Ante la duda devuelve null a propósito: el armador trata null como "no sé" y avisa
 * en vez de bloquear, que es mejor que afirmar una compatibilidad falsa.
 */
public final class ComponentePcExtractor {

    public enum Tipo { PROCESADOR, MOTHER, MEMORIA, VIDEO, ALMACENAMIENTO, FUENTE, GABINETE, COOLER }

    private ComponentePcExtractor() {}

    /** Mayúsculas y sin tildes, para que las regex no tengan que contemplar variantes. */
    static String normalizar(String s) {
        if (s == null) return "";
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replaceAll("[®™]", "");
    }

    // ---------------------------------------------------------------- tipo

    private static final List<String> CATEGORIAS_DE_COMPONENTES = List.of(
            "MICROPROCESADORES", "MOTHERS", "MEMORIAS RAM", "PLACAS DE VIDEO",
            "DISCOS RIGIDOS / SSD", "GABINETES Y FUENTES", "COOLERS");

    private static final Pattern NOMBRE_EXCLUIDO = Pattern.compile(
            "\\b(SODIMM|NOTEBOOK|EXTERNO|PORTATIL|PORTABLE|NAS|FAN|FANS|KIT GABINETE|BASE|TARJETA DE MEMORIA|MICRO ?SD)\\b");

    /**
     * El tipo sale primero del ENCABEZADO del nombre (las primeras palabras, donde el mayorista
     * pone qué es) y recién después de la categoría: la categoría "Coolers > Fans" tiene fuentes
     * y gabinetes de Cooler Master adentro, y confiar en ella los ofrecería como cooler.
     */
    public static Tipo tipo(String modelo, String categoriaPadre, String categoriaHoja) {
        String nombre = normalizar(modelo).trim();
        String[] palabras = nombre.split("\\s+");
        String encabezado = String.join(" ", List.of(palabras).subList(0, Math.min(3, palabras.length)));
        String padre = normalizar(categoriaPadre);
        String hoja = normalizar(categoriaHoja);

        // Solo las categorías de componentes: un "AIO" puede ser una computadora All in One.
        if (!CATEGORIAS_DE_COMPONENTES.contains(padre)) return null;

        Tipo porNombre = tipoPorEncabezado(encabezado);
        Tipo tipo = porNombre != null ? porNombre : tipoPorCategoria(padre, hoja);
        if (tipo == null) return null;

        // Lo que no se monta dentro de un gabinete de escritorio no va al armador. Hay tarjetas
        // SD categorizadas como "Memoria DDR4": sin esto se ofrecían como RAM.
        if (NOMBRE_EXCLUIDO.matcher(encabezado).find()) return null;
        if (tipo == Tipo.MEMORIA && (hoja.contains("SODIMM") || !hoja.matches(".*DDR[45].*") && !nombre.matches(".*DDR[45].*"))) return null;
        if (tipo == Tipo.ALMACENAMIENTO && (hoja.contains("EXTERNO") || hoja.contains("NAS") || hoja.contains("NOTEBOOK") || hoja.contains("CARRY"))) return null;
        return tipo;
    }

    private static Tipo tipoPorEncabezado(String e) {
        // Antes que PROCESADOR: "CPU Cooler …" es un cooler.
        if (e.matches("^(AIR COOLER|CPU COOLER|COOLER CPU|DISIPADOR|WATER ?COOLER|WATER ?COOLING|WATERCOOLER|REFRIGERACION LIQUIDA|AIO)\\b.*")) return Tipo.COOLER;
        if (e.matches("^(PROCESADOR|PROCES\\.?|MICROPROCESADOR|CPU)\\b.*")) return Tipo.PROCESADOR;
        if (e.matches("^(MOTHER|MOTHERBOARD|PLACA MADRE|MB)\\b.*")) return Tipo.MOTHER;
        if (e.matches("^(MEMORIA|MEM\\.?)\\b.*")) return Tipo.MEMORIA;
        if (e.matches("^(PLACA DE VIDEO|TARJETA DE VIDEO|VIDEO|PLACA VIDEO)\\b.*")) return Tipo.VIDEO;
        if (e.matches("^(DISCO|SSD|HDD)\\b.*")) return Tipo.ALMACENAMIENTO;
        if (e.matches("^FUENTE\\b.*")) return Tipo.FUENTE;
        if (e.matches("^GABINETE\\b.*")) return Tipo.GABINETE;
        return null;
    }

    private static Tipo tipoPorCategoria(String padre, String hoja) {
        if (padre.equals("MICROPROCESADORES")) return Tipo.PROCESADOR;
        if (padre.equals("MOTHERS")) return Tipo.MOTHER;
        if (padre.equals("MEMORIAS RAM")) return Tipo.MEMORIA;
        if (padre.equals("PLACAS DE VIDEO")) return Tipo.VIDEO;
        if (hoja.equals("DISCO SSD") || hoja.equals("DISCO SSD M2") || hoja.equals("DISCO RIGIDO SATA")) return Tipo.ALMACENAMIENTO;
        if (hoja.equals("FUENTES DE ALIMENTACION")) return Tipo.FUENTE;
        if (hoja.startsWith("GABINETES SIN FUENTE")) return Tipo.GABINETE;
        // "Fans" NO: son ventiladores de gabinete salvo que el nombre diga otra cosa.
        if (hoja.equals("WATERCOOLERS")) return Tipo.COOLER;
        return null;
    }

    // ---------------------------------------------------------------- socket

    private static final Pattern SOCKET_AMD = Pattern.compile("\\bAM([45])\\b");
    private static final Pattern SOCKET_INTEL = Pattern.compile("\\b(?:LGA|S|SOCKET:?)\\s*-?\\s*(1200|1700|1851)\\b");

    private static final List<Object[]> CHIPSETS = List.of(
            new Object[]{Pattern.compile("\\b(A320|A520|B350|B450|B550|X370|X470|X570)(?!\\d)"), "AM4"},
            new Object[]{Pattern.compile("\\b(A620|B650|B840|B850|X670|X870)(?!\\d)"), "AM5"},
            new Object[]{Pattern.compile("\\b(H610|B660|H670|Z690|B760|H770|Z790)(?!\\d)"), "LGA1700"},
            new Object[]{Pattern.compile("\\b(H810|B860|Z890)(?!\\d)"), "LGA1851"},
            new Object[]{Pattern.compile("\\b(H410|B460|H470|Z490|H510|B560|H570|Z590)(?!\\d)"), "LGA1200"});

    private static final Pattern RYZEN = Pattern.compile("\\bRYZEN\\s*[3579]\\s*(?:PRO\\s*)?(\\d)(\\d{3})([A-Z0-9]*)");
    private static final Pattern INTEL_CORE = Pattern.compile("\\bI[3579]\\s*-?\\s*(1[0-4])(\\d{3})([A-Z]*)");
    private static final Pattern INTEL_PENTIUM = Pattern.compile("\\b(?:PENTIUM|CELERON)(?:\\s+GOLD)?\\s+G\\d{4}([A-Z]*)");
    private static final Pattern INTEL_ULTRA = Pattern.compile("\\bULTRA\\s*[3579]\\s*-?\\s*(2\\d{2})([A-Z]*)");

    /** AM4 | AM5 | LGA1200 | LGA1700 | LGA1851, o null si no se puede afirmar. */
    public static String socket(Tipo tipo, String modelo, String especificaciones) {
        String t = normalizar(modelo) + " " + normalizar(especificaciones);
        Matcher m = SOCKET_AMD.matcher(t);
        if (m.find()) return "AM" + m.group(1);
        m = SOCKET_INTEL.matcher(t);
        if (m.find()) return "LGA" + m.group(1);

        if (tipo == Tipo.MOTHER) return socketPorChipset(t);
        if (tipo == Tipo.PROCESADOR) return socketPorProcesador(t);
        return null;
    }

    static String socketPorChipset(String t) {
        for (Object[] c : CHIPSETS) {
            if (((Pattern) c[0]).matcher(t).find()) return (String) c[1];
        }
        return null;
    }

    static String socketPorProcesador(String t) {
        Matcher m = RYZEN.matcher(t);
        if (m.find()) {
            int serie = Integer.parseInt(m.group(1));
            if (serie >= 7) return "AM5";   // 7000, 8000 (8600G/8700G de escritorio), 9000
            if (serie >= 1) return "AM4";
        }
        m = INTEL_CORE.matcher(t);
        if (m.find()) {
            int gen = Integer.parseInt(m.group(1));
            return gen >= 12 ? "LGA1700" : "LGA1200";
        }
        if (INTEL_ULTRA.matcher(t).find()) return "LGA1851";
        return null;
    }

    // ---------------------------------------------------------------- RAM

    private static final Pattern DDR = Pattern.compile("\\bDDR([45])\\b|[-\\s]D([45])\\b");

    /**
     * DDR4 | DDR5. En un mother, si el texto nombra las dos (pasa en fichas de LGA1700 que
     * describen la plataforma) no se afirma ninguna. Si no nombra ninguna, el socket la fija en
     * todas las plataformas salvo LGA1700, que salió en las dos versiones.
     */
    public static String tipoRam(Tipo tipo, String modelo, String especificaciones, String categoriaHoja, String socket) {
        if (tipo == Tipo.MEMORIA) {
            String h = normalizar(categoriaHoja);
            if (h.contains("DDR5")) return "DDR5";
            if (h.contains("DDR4")) return "DDR4";
        }
        String enNombre = ddrUnico(normalizar(modelo));
        if (enNombre != null) return enNombre;
        String enFicha = ddrUnico(normalizar(especificaciones));
        if (enFicha != null) return enFicha;
        if (tipo == Tipo.MOTHER && socket != null) {
            switch (socket) {
                case "AM5", "LGA1851": return "DDR5";
                case "AM4", "LGA1200": return "DDR4";
                default: return null;
            }
        }
        return null;
    }

    private static String ddrUnico(String t) {
        Matcher m = DDR.matcher(t);
        String encontrado = null;
        while (m.find()) {
            String v = "DDR" + (m.group(1) != null ? m.group(1) : m.group(2));
            if (encontrado != null && !encontrado.equals(v)) return null;
            encontrado = v;
        }
        return encontrado;
    }

    // ---------------------------------------------------------------- formato

    /** De chico a grande: un gabinete que admite uno admite todos los anteriores. */
    public static final List<String> FORMATOS = List.of("ITX", "MATX", "ATX", "EATX");

    private static final Pattern F_EATX = Pattern.compile("\\bE-?ATX\\b");
    private static final Pattern F_MATX = Pattern.compile("\\b(MICRO[\\s-]?ATX|M-?ATX|U-?ATX)\\b");
    private static final Pattern F_ITX = Pattern.compile("\\b(MINI[\\s-]?ITX|ITX)\\b");
    private static final Pattern F_ATX = Pattern.compile("(?<![-\\w])ATX\\b");
    private static final Pattern CHIPSET_CON_SUFIJO = Pattern.compile(
            "\\b(?:A320|A520|B350|B450|B550|X370|X470|X570|A620|B650|B840|B850|X670|X870|H610|B660|H670|Z690|B760|H770|Z790"
            + "|H810|B860|Z890|H410|B460|H470|Z490|H510|B560|H570|Z590)E?(M|I)\\b");
    private static final Pattern MINI_TOWER = Pattern.compile("\\bMINI[\\s-]?TOWER\\b");
    private static final Pattern TORRE_ATX = Pattern.compile("\\b(MID|FULL)[\\s-]?TOWER\\b");

    /**
     * Mother: el formato de la placa. Gabinete: el formato MÁS GRANDE que admite. Primero el
     * nombre, después la ficha: la ficha de un gabinete suele listar todo lo que entra
     * ("ATX / mATX / ITX") y ahí vale el mayor.
     */
    public static String formato(Tipo tipo, String modelo, String especificaciones) {
        String nombre = normalizar(modelo);
        String ficha = normalizar(especificaciones);
        if (tipo == Tipo.GABINETE) {
            String f = mayorFormato(nombre);
            if (f == null) f = mayorFormato(ficha);
            if (f == null && TORRE_ATX.matcher(nombre + " " + ficha).find()) f = "ATX";
            if (f == null && MINI_TOWER.matcher(nombre + " " + ficha).find()) f = "MATX";
            return f;
        }
        if (tipo == Tipo.MOTHER) {
            String f = primerFormato(nombre);
            if (f != null) return f;
            Matcher m = CHIPSET_CON_SUFIJO.matcher(nombre);
            if (m.find()) return m.group(1).equals("M") ? "MATX" : "ITX";
            return primerFormato(ficha);
        }
        return null;
    }

    private static String primerFormato(String t) {
        if (F_EATX.matcher(t).find()) return "EATX";
        if (F_MATX.matcher(t).find()) return "MATX";
        if (F_ITX.matcher(t).find()) return "ITX";
        if (F_ATX.matcher(t).find()) return "ATX";
        return null;
    }

    private static String mayorFormato(String t) {
        if (F_EATX.matcher(t).find()) return "EATX";
        if (F_ATX.matcher(t).find()) return "ATX";
        if (F_MATX.matcher(t).find()) return "MATX";
        if (F_ITX.matcher(t).find()) return "ITX";
        return null;
    }

    /** ¿Un mother de formato {@code placa} entra en un gabinete que admite hasta {@code gabinete}? */
    public static boolean entra(String placa, String gabinete) {
        return FORMATOS.indexOf(placa) <= FORMATOS.indexOf(gabinete);
    }

    // ---------------------------------------------------------------- ranuras de memoria

    private static final List<Pattern> RANURAS = List.of(
            // Elit: "Cantidad de slots de memoria RAM: 4."
            Pattern.compile("CANTIDAD DE (?:SLOTS|RANURAS|ZOCALOS) DE MEMORIA(?: RAM)?\\s*:\\s*(\\d)"),
            Pattern.compile("\\b(\\d)\\s*[X×]\\s*(?:DDR[45]\\s*)?(?:U?DIMM|SLOTS?|RANURAS?|ZOCALOS?)\\b"),
            Pattern.compile("\\b(\\d)\\s*(?:SLOTS?|RANURAS?|ZOCALOS?|DIMMS?)\\b"));

    /**
     * Cuántos módulos de RAM entran en un mother. Si la ficha no lo dice, el formato lo fija en
     * los extremos (ATX = 4, ITX = 2 — medido: los 34 ATX del catálogo con dato tienen 4), pero
     * NO en Micro-ATX, que salió con 2 y con 4 en partes parecidas: ahí queda null.
     */
    public static Integer ranurasRam(String modelo, String especificaciones, String formato) {
        String t = normalizar(modelo) + " " + normalizar(especificaciones);
        for (Pattern p : RANURAS) {
            Matcher m = p.matcher(t);
            if (m.find()) {
                int n = Integer.parseInt(m.group(1));
                if (n == 1 || n == 2 || n == 4 || n == 8) return n;
            }
        }
        if ("ATX".equals(formato) || "EATX".equals(formato)) return 4;
        if ("ITX".equals(formato)) return 2;
        return null;
    }

    private static final Pattern KIT = Pattern.compile("\\b([248])\\s*[X×]\\s*\\d+\\s*GB\\b");

    /** Módulos que trae una unidad de memoria: un kit "2x16GB" ocupa dos ranuras. */
    public static int modulosPorUnidad(String modelo) {
        Matcher m = KIT.matcher(normalizar(modelo));
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    // ---------------------------------------------------------------- watts

    private static final Pattern WATTS = Pattern.compile("\\b(\\d{3,4})\\s?W\\b");

    /** Potencia nominal de una fuente. */
    public static Integer potenciaW(String modelo, String especificaciones) {
        Integer w = primerosWatts(normalizar(modelo));
        return w != null ? w : primerosWatts(normalizar(especificaciones));
    }

    private static Integer primerosWatts(String t) {
        Matcher m = WATTS.matcher(t);
        while (m.find()) {
            int w = Integer.parseInt(m.group(1));
            if (w >= 200 && w <= 2000) return w;
        }
        return null;
    }

    /**
     * Fuente recomendada por el fabricante del chip, no el consumo de la placa: es el número que
     * sirve para comparar contra la potencia de la fuente, porque ya incluye al resto del equipo.
     * La tabla va de lo más específico a lo menos ("5070 TI" antes que "5070").
     */
    private static final List<Object[]> FUENTE_POR_GPU = List.of(
            gpu("RTX\\s*5090", 1000), gpu("RTX\\s*4090", 1000), gpu("RTX\\s*3090", 850),
            gpu("RTX\\s*5080", 850), gpu("RTX\\s*4080", 850), gpu("RTX\\s*3080", 750),
            gpu("RTX\\s*5070\\s*TI", 750), gpu("RTX\\s*4070\\s*TI", 700), gpu("RTX\\s*5070", 650),
            gpu("RTX\\s*4070", 650), gpu("RTX\\s*3070", 650),
            gpu("RTX\\s*5060\\s*TI", 600), gpu("RTX\\s*4060\\s*TI", 550), gpu("RTX\\s*5060", 550),
            gpu("RTX\\s*5050", 550), gpu("RTX\\s*4060", 550), gpu("RTX\\s*3060", 550),
            gpu("RTX\\s*3050", 450), gpu("RTX\\s*2060", 500),
            gpu("GTX\\s*16[5-6]0", 350), gpu("GTX\\s*1630", 300), gpu("GT\\s*(210|710|730|1030)", 300),
            gpu("RX\\s*9070\\s*XT", 750), gpu("RX\\s*9070", 650), gpu("RX\\s*9060", 550),
            gpu("RX\\s*7900", 850), gpu("RX\\s*7800", 700), gpu("RX\\s*7700", 700), gpu("RX\\s*7600", 550),
            gpu("RX\\s*6[89]\\d0", 850), gpu("RX\\s*67[05]0", 650), gpu("RX\\s*66[05]0", 500),
            gpu("RX\\s*6[45]00", 400), gpu("RX\\s*5[5-8]0", 450),
            gpu("ARC\\s*B5[78]0", 550), gpu("ARC\\s*A7[57]0", 600), gpu("ARC\\s*A3[18]0", 350));

    private static Object[] gpu(String regex, int watts) {
        return new Object[]{Pattern.compile("\\b" + regex + "\\b"), watts};
    }

    public static Integer fuenteRecomendadaW(String modelo, String especificaciones) {
        String t = normalizarGpu(modelo, especificaciones);
        for (Object[] g : FUENTE_POR_GPU) {
            if (((Pattern) g[0]).matcher(t).find()) return (Integer) g[1];
        }
        return null;
    }

    // ---------------------------------------------------------------- gama (cuellos de botella)

    /**
     * Gama de 1 (entrada) a 5 (tope) de un procesador, para avisar cuellos de botella con la placa
     * de video. Es una guía para juegos, no un benchmark: la línea (Ryzen 5, i7) da la base y la
     * generación la corrige — un Ryzen 5 3600 rinde claramente menos que un Ryzen 5 7600.
     */
    public static Integer gamaProcesador(String modelo, String especificaciones) {
        String t = normalizar(modelo) + " " + normalizar(especificaciones);
        Matcher m = RYZEN_LINEA.matcher(t);
        if (m.find()) {
            int linea = Integer.parseInt(m.group(1));
            int serie = Integer.parseInt(m.group(2));
            String sufijo = m.group(3);
            int gama = switch (linea) { case 3 -> 1; case 5 -> 3; case 7 -> 4; default -> 5; };
            if (serie <= 3) gama--;                  // Zen/Zen+/Zen 2 (1000–3000)
            if (sufijo.contains("X3D")) gama++;      // caché 3D: lo mejor para juegos de su línea
            return Math.max(1, Math.min(5, gama));
        }
        m = INTEL_LINEA.matcher(t);
        if (m.find()) {
            int linea = Integer.parseInt(m.group(1));
            int gen = Integer.parseInt(m.group(2));
            int gama = switch (linea) { case 3 -> 2; case 5 -> 3; case 7 -> 4; default -> 5; };
            if (gen < 12) gama--;
            return Math.max(1, gama);
        }
        m = INTEL_ULTRA.matcher(t);
        if (m.find()) {
            Matcher l = Pattern.compile("\\bULTRA\\s*([3579])").matcher(t);
            if (l.find()) return switch (Integer.parseInt(l.group(1))) { case 3 -> 2; case 5 -> 3; case 7 -> 4; default -> 5; };
        }
        if (t.matches(".*\\b(ATHLON|PENTIUM|CELERON)\\b.*")) return 1;
        return null;
    }

    private static final Pattern RYZEN_LINEA = Pattern.compile("\\bRYZEN\\s*([3579])\\s*(?:PRO\\s*)?(\\d)\\d{3}([A-Z0-9]*)");
    private static final Pattern INTEL_LINEA = Pattern.compile("\\bI([3579])\\s*-?\\s*(1[0-4]|[4-9])\\d{3}");

    /** Gama de 1 a 5 de una placa de video, por chip. De lo más específico a lo menos. */
    private static final List<Object[]> GAMA_POR_GPU = List.of(
            gpu("RTX\\s*(5090|4090|5080|4080|3090)", 5), gpu("RX\\s*7900\\s*XTX", 5),
            gpu("RTX\\s*(5070|4070|3080|3070)", 4), gpu("RX\\s*(9070|7900|7800|7700|6[89]\\d0)", 4),
            gpu("RTX\\s*(5060|4060|3060|2060|2070)", 3), gpu("RX\\s*(9060|7600|67[05]0|66[05]0)", 3),
            gpu("ARC\\s*(B5[78]0|A7[57]0)", 3),
            gpu("RTX\\s*(3050|5050)", 2), gpu("GTX\\s*16[56]0", 2), gpu("RX\\s*(6[45]00|5[5-8]0|9050)", 2),
            gpu("ARC\\s*A3[18]0", 2),
            gpu("GTX\\s*1630", 1), gpu("GT\\s*(210|710|730|1030)", 1));

    public static Integer gamaVideo(String modelo, String especificaciones) {
        String t = normalizarGpu(modelo, especificaciones);
        for (Object[] g : GAMA_POR_GPU) {
            if (((Pattern) g[0]).matcher(t).find()) return (Integer) g[1];
        }
        return null;
    }

    /** "RTX5060TI", "GeForce 5060 Ti" y "RTX 5060 Ti" tienen que leerse igual. */
    private static String normalizarGpu(String modelo, String especificaciones) {
        String t = normalizar(modelo) + " " + normalizar(especificaciones);
        return t.replaceAll("GEFORCE\\s+([2-5]0\\d0)\\b", "GEFORCE RTX $1")
                .replaceAll("(RTX|GTX|RX|GT|ARC)(\\d)", "$1 $2")
                .replaceAll("(\\d)(TI|XT)\\b", "$1 $2");
    }

    // ---------------------------------------------------------------- procesador

    /**
     * ¿Trae video integrado? Invid lo escribe ("CON VIDEO" / "SIN VIDEO"); si no, el sufijo del
     * modelo: Intel "F" = sin video; Ryzen "G" = con video, y los Ryzen 7000/9000 traen uno
     * básico salvo los "F". Los Ryzen AM4 sin "G" no tienen.
     */
    public static Boolean videoIntegrado(String modelo, String especificaciones) {
        String t = normalizar(modelo) + " " + normalizar(especificaciones);
        if (t.contains("SIN VIDEO")) return false;
        if (t.contains("CON VIDEO")) return true;
        Matcher m = RYZEN.matcher(t);
        if (m.find()) {
            String sufijo = m.group(3);
            if (sufijo.contains("G")) return true;
            if (sufijo.contains("F")) return false;
            return Integer.parseInt(m.group(1)) >= 7;
        }
        m = INTEL_CORE.matcher(t);
        if (m.find()) return !m.group(3).contains("F");
        m = INTEL_ULTRA.matcher(t);
        if (m.find()) return !m.group(2).contains("F");
        m = INTEL_PENTIUM.matcher(t);
        if (m.find()) return !m.group(1).contains("F");
        return null;
    }

    /**
     * ¿Viene con cooler en la caja? Invid lo escribe; si no, los desbloqueados (Intel "K",
     * Ryzen "X" de AM5 y todos los X3D) se venden sin cooler. El resto queda en null: no se
     * sabe, y el armador lo sugiere sin exigirlo.
     */
    public static Boolean incluyeCooler(String modelo, String especificaciones) {
        String t = normalizar(modelo) + " " + normalizar(especificaciones);
        if (t.contains("SIN COOLER")) return false;
        if (t.contains("CON COOLER")) return true;
        Matcher m = RYZEN.matcher(t);
        if (m.find()) {
            String sufijo = m.group(3);
            if (sufijo.contains("X3D")) return false;
            if (sufijo.startsWith("X") && Integer.parseInt(m.group(1)) >= 7) return false;
            return null;
        }
        m = INTEL_CORE.matcher(t);
        if (m.find() && m.group(3).startsWith("K")) return false;
        m = INTEL_ULTRA.matcher(t);
        if (m.find() && m.group(2).startsWith("K")) return false;
        return null;
    }
}
