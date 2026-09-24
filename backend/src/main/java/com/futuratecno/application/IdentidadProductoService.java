package com.futuratecno.application;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identidad de un artículo, separada de su nombre visible (marca + modelo).
 *
 * <p>En una carga por JSON el modelo lo redacta la IA y las características se reparten entre el
 * modelo y las especificaciones: "G04 4G 64GB / 4GB RAM" y "Motorola G04 4G 64GB" con {@code ram:
 * "4GB"} son el mismo teléfono. La clave suelta de la V37 solo mira marca y modelo y los ve
 * distintos; aflojarla sacando RAM o capacidad fusionaría variantes distintas y pisaría precios.
 * Acá se extraen los atributos que distinguen de verdad y se comparan como atributos.
 *
 * <p>Reglas, y por qué:
 * <ul>
 *   <li><b>Deterministas y versionadas.</b> Ni IA ni similitud de texto deciden que dos artículos
 *       son el mismo. La versión viaja en la clave ("tel1|…"): si las reglas cambian, las claves
 *       viejas no se confunden con las nuevas.</li>
 *   <li><b>Teléfonos primero.</b> Lo que no se reconoce como teléfono usa la clave suelta de
 *       siempre ({@link #VERSION_GENERICA}), sin aplicarle reglas de teléfonos.</li>
 *   <li><b>Un dato desconocido no es un comodín.</b> Si falta algo que distingue variantes (la RAM
 *       de un Android, el almacenamiento) o el nombre y las especificaciones se contradicen, el
 *       resultado queda {@link Estado#REVISION}: no crea ni actualiza nada.</li>
 *   <li><b>4G no es 4GB</b> (conectividad vs. memoria) y <b>la RAM virtual no es RAM</b>: "8GB +
 *       8GB virtual" es un teléfono de 8GB.</li>
 * </ul>
 */
@Service
public class IdentidadProductoService {

    public static final String VERSION_TELEFONO = "telefono-v1";
    public static final String VERSION_GENERICA = "generico-v1";
    static final String PREFIJO_TELEFONO = "tel1";
    static final String PREFIJO_GENERICO = "gen1";

    /** Campos que componen la identidad de un teléfono, en el orden en que entran a la clave. */
    static final List<String> CAMPOS_TELEFONO = List.of(
            "conectividad", "almacenamiento_gb", "ram_gb", "sim", "color", "condicion", "combo", "region");

    public enum Estado { RESUELTA, REVISION }

    public enum Compatibilidad { IGUAL, DISTINTA, CONFLICTO }

    /**
     * @param clave     identidad completa; null si quedó en revisión
     * @param familia   marca + modelo base (sin capacidad, color, etc.); sirve para buscar
     *                  candidatos y detectar conflictos. Puede venir aunque esté en revisión.
     * @param atributos lo extraído y de dónde salió, para auditar la decisión
     * @param motivos   por qué quedó en revisión (vacío si se resolvió)
     */
    public record Resolucion(Estado estado, String version, String clave, String familia,
                             Map<String, String> atributos, List<String> motivos, boolean contradictoria) {
        public boolean resuelta() { return estado == Estado.RESUELTA; }
        public boolean esTelefono() { return VERSION_TELEFONO.equals(version); }
    }

    // ------------------------------------------------------------------ API

    /**
     * Resuelve la identidad de un artículo que entra por JSON.
     *
     * @param especificaciones el objeto {@code especificaciones} del JSON (claves como "ram",
     *                         "almacenamiento", "otros"); puede ser null
     * @param categoria        pista de categoría ("Celulares", "Smartphones > Android"); puede ser null
     */
    public Resolucion resolver(String marca, String modelo, Map<String, ?> especificaciones, String categoria) {
        return resolver(marca, modelo, especificaciones, null, categoria);
    }

    /**
     * Resuelve la identidad de un producto ya guardado, cuyas especificaciones son un texto libre
     * ("Octa-core · 4GB · 64GB · 6.5\"") y no un objeto con claves.
     */
    public Resolucion resolverGuardado(String marca, String modelo, String especificacionesTexto, String categoria) {
        return resolver(marca, modelo, null, especificacionesTexto, categoria);
    }

    /**
     * ¿Dos resoluciones son el mismo artículo?
     * <ul>
     *   <li>{@code IGUAL}: misma clave.</li>
     *   <li>{@code DISTINTA}: otra familia, o algún atributo conocido de los dos lados difiere
     *       (64GB vs. 128GB, G04 vs. G04s). Es seguro tratarlas como artículos distintos.</li>
     *   <li>{@code CONFLICTO}: no se puede afirmar ni lo uno ni lo otro — un lado no conoce un
     *       atributo que el otro sí, alguna quedó en revisión o las reglas son de otra versión. No
     *       se decide solo: va a revisión.</li>
     * </ul>
     */
    public Compatibilidad comparar(Resolucion a, Resolucion b) {
        if (a.resuelta() && b.resuelta() && Objects.equals(a.version(), b.version())
                && Objects.equals(a.clave(), b.clave())) {
            return Compatibilidad.IGUAL;
        }
        if (!Objects.equals(a.version(), b.version())) return Compatibilidad.CONFLICTO;
        if (!a.esTelefono()) {
            // Genérico: la clave ES la familia. Si no coincide, son artículos distintos.
            return Objects.equals(a.familia(), b.familia()) && !(a.resuelta() && b.resuelta())
                    ? Compatibilidad.CONFLICTO : Compatibilidad.DISTINTA;
        }
        if (a.familia() == null || b.familia() == null) return Compatibilidad.CONFLICTO;
        if (!a.familia().equals(b.familia())) return Compatibilidad.DISTINTA;
        for (String campo : CAMPOS_TELEFONO) {
            String va = a.atributos().get(campo), vb = b.atributos().get(campo);
            if (va != null && vb != null && !va.equals(vb)) {
                // Un lado contradictorio no permite afirmar nada, ni siquiera que es distinto.
                return a.contradictoria() || b.contradictoria() ? Compatibilidad.CONFLICTO : Compatibilidad.DISTINTA;
            }
        }
        return Compatibilidad.CONFLICTO;
    }

    // ------------------------------------------------------------------ resolución

    private Resolucion resolver(String marcaIn, String modeloIn, Map<String, ?> espMapa, String espTexto, String categoria) {
        String marca = norm(marcaIn).trim();
        String modelo = norm(modeloIn).trim();
        if (marca.isEmpty() || modelo.isEmpty()) {
            return new Resolucion(Estado.REVISION, VERSION_GENERICA, null, null, Map.of(),
                    List.of("Falta marca o modelo."), false);
        }
        // Redmi y POCO son líneas de Xiaomi: "Redmi" + "Note 14" = "Xiaomi" + "Redmi Note 14".
        if (marca.equals("REDMI") || marca.equals("POCO")) {
            modelo = marca + " " + modelo;
            marca = "XIAOMI";
        }
        if (!esTelefono(marca, modelo, categoria)) return generica(marcaIn, modeloIn);
        return telefono(marca, modelo, espMapa, espTexto);
    }

    private Resolucion generica(String marca, String modelo) {
        String clave = PREFIJO_GENERICO + "|" + ImagenManualService.clave(marca, modelo);
        Map<String, String> at = new LinkedHashMap<>();
        at.put("tipo", "generico");
        at.put("clave_suelta", ImagenManualService.clave(marca, modelo));
        return new Resolucion(Estado.RESUELTA, VERSION_GENERICA, clave, clave, at, List.of(), false);
    }

    /** Valor extraído de una fuente. */
    private static final class Extraido {
        final Map<String, String> valores = new LinkedHashMap<>();
        final List<String> problemas = new ArrayList<>();
        Integer ramVirtual;
        Integer capacidadAmbigua;   // un único "16GB" sin marca de RAM: no se sabe qué es

        void poner(String campo, String valor, String fuente) {
            if (valor == null) return;
            String previo = valores.get(campo);
            if (previo != null && !previo.equals(valor)) {
                problemas.add("El " + fuente + " informa dos valores de " + nombre(campo) + ": " + previo + " y " + valor + ".");
                return;
            }
            valores.put(campo, valor);
        }
    }

    private Resolucion telefono(String marca, String modelo, Map<String, ?> espMapa, String espTexto) {
        boolean esIphone = modelo.matches(".*\\bIPHONE\\b.*");

        Extraido delNombre = new Extraido();
        String base = extraerDelNombre(marca, modelo, delNombre);

        Extraido deEspecs = new Extraido();
        if (espMapa != null) extraerDeMapa(espMapa, deEspecs);
        if (espTexto != null) extraerDeTexto(norm(espTexto), deEspecs, true);

        Map<String, String> at = new LinkedHashMap<>();
        at.put("tipo", "telefono");
        at.put("marca", marca.toLowerCase(Locale.ROOT));
        at.put("base", base);
        List<String> motivos = new ArrayList<>();
        motivos.addAll(delNombre.problemas);
        motivos.addAll(deEspecs.problemas);
        boolean contradictoria = !motivos.isEmpty();

        // Una capacidad suelta y ambigua en el nombre ("16GB") solo se acepta si las
        // especificaciones la ubican como RAM o como almacenamiento.
        if (delNombre.capacidadAmbigua != null) {
            String v = String.valueOf(delNombre.capacidadAmbigua);
            if (v.equals(deEspecs.valores.get("ram_gb")) && !delNombre.valores.containsKey("ram_gb")) {
                delNombre.valores.put("ram_gb", v);
            } else if (v.equals(deEspecs.valores.get("almacenamiento_gb")) && !delNombre.valores.containsKey("almacenamiento_gb")) {
                delNombre.valores.put("almacenamiento_gb", v);
            } else {
                motivos.add("El nombre dice " + v + "GB sin aclarar si es RAM o almacenamiento.");
            }
        }

        for (String campo : CAMPOS_TELEFONO) {
            String n = delNombre.valores.get(campo), e = deEspecs.valores.get(campo);
            if (n != null && e != null && !n.equals(e)) {
                motivos.add("Contradicción en " + nombre(campo) + ": el nombre dice " + n
                        + " y las especificaciones " + e + ".");
                contradictoria = true;
                at.put(campo, n);
                at.put("fuente." + campo, "contradictorio");
            } else if (n != null || e != null) {
                at.put(campo, n != null ? n : e);
                at.put("fuente." + campo, n != null ? (e != null ? "nombre+especificaciones" : "nombre") : "especificaciones");
            }
        }
        Integer virtual = delNombre.ramVirtual != null ? delNombre.ramVirtual : deEspecs.ramVirtual;
        if (virtual != null) at.put("ram_virtual_gb", String.valueOf(virtual));

        // El iPhone no se vende por RAM: cada modelo+capacidad tiene una sola.
        if (esIphone) {
            at.remove("ram_gb");
            at.put("fuente.ram_gb", "no aplica (iPhone)");
        }
        // La condición casi nunca se escribe cuando es nuevo: sin mención, es nuevo.
        if (!at.containsKey("condicion")) {
            at.put("condicion", "nuevo");
            at.put("fuente.condicion", "por defecto");
        }

        if (base.isEmpty()) motivos.add("No quedó un modelo base reconocible.");
        if (!at.containsKey("almacenamiento_gb")) motivos.add("Falta el almacenamiento.");
        if (!esIphone && !at.containsKey("ram_gb")) {
            motivos.add(virtual != null ? "Solo se informó RAM virtual; falta la RAM física." : "Falta la RAM física.");
        }

        String familia = base.isEmpty() ? null : PREFIJO_TELEFONO + "|" + at.get("marca") + "|" + base;
        if (!motivos.isEmpty()) {
            return new Resolucion(Estado.REVISION, VERSION_TELEFONO, null, familia, at, List.copyOf(motivos), contradictoria);
        }
        StringBuilder clave = new StringBuilder(familia);
        for (String campo : CAMPOS_TELEFONO) {
            String v = campo.equals("ram_gb") && esIphone ? "na" : at.get(campo);
            clave.append('|').append(v != null ? v : "-");
        }
        return new Resolucion(Estado.RESUELTA, VERSION_TELEFONO, clave.toString(), familia, at, List.of(), false);
    }

    // ------------------------------------------------------------------ ¿es un teléfono?

    private static final Pattern NO_TELEFONO = Pattern.compile(
            "\\b(TAB|TABLET|PAD|IPAD|WATCH|BUDS|BOOK|NOTEBOOK|LAPTOP|TV|SMART\\s*TV|BAND|AURICULAR(ES)?|FUNDA|CASE|"
            + "CARGADOR|CABLE|FILM|VIDRIO|PROTECTOR|MONITOR|PARLANTE|SPEAKER|MOUSE|TECLADO|ROUTER|DRONE|CAMARA|"
            + "SOPORTE|ADAPTADOR|PENCIL|AIRPODS|MAC|IMAC|MACBOOK|CONSOLA|JOYSTICK|TAG|AIRTAG|SCOOTER|MONOPATIN|"
            + "ASPIRADORA|ROBOT|VACUUM|FREIDORA|PURIFICADOR|BALANZA|CEPILLO|AFEITADORA|SECADOR|LAMPARA|IMPRESORA)\\b");
    private static final Pattern CATEGORIA_TELEFONO = Pattern.compile("CELULAR|SMARTPHONE|TELEFON|IPHONE");
    private static final List<String> MARCAS_TELEFONO = List.of(
            "REALME", "HONOR", "OPPO", "VIVO", "NOKIA", "ZTE", "TCL", "TECNO", "INFINIX", "ALCATEL", "HUAWEI",
            "ONEPLUS", "NOTHING", "GOOGLE", "BLU");

    /**
     * Teléfono = la categoría lo dice, o el nombre sigue el patrón de una línea de teléfonos de la
     * marca. Lo que nombra otro tipo de producto (Tab, Watch, Buds, TV, funda) nunca lo es: Samsung
     * y Motorola venden muchas cosas que no son teléfonos.
     */
    boolean esTelefono(String marca, String modeloCompleto, String categoria) {
        // Lo que viene en el combo no dice qué es el artículo: "G04 + Funda" es un teléfono.
        String modelo = COMBO.matcher(modeloCompleto).replaceFirst(" ");
        if (NO_TELEFONO.matcher(modelo).find()) return false;
        if (categoria != null && CATEGORIA_TELEFONO.matcher(norm(categoria)).find()) return true;
        switch (marca) {
            case "APPLE": return modelo.matches(".*\\bIPHONE\\b.*");
            case "MOTOROLA": return modelo.matches("(?s).*\\b((MOTO\\s*)?[GE]\\s?\\d{1,3}[A-Z]?|EDGE|RAZR)\\b.*");
            // La línea S de teléfonos es S2x; "S90D" es un televisor.
            case "SAMSUNG": return modelo.matches("(?s).*\\b(GALAXY\\s*)?(A\\s?\\d{2}|S\\s?2\\d|M\\s?\\d{2}|F\\s?\\d{2}|Z\\s?(FLIP|FOLD)\\s?\\d?|NOTE\\s?\\d{1,2}|XCOVER\\s?\\d?)\\w*\\b.*");
            // Una línea de teléfonos con número: Xiaomi vende también monopatines y robots.
            case "XIAOMI": return modelo.matches("(?s).*\\b(REDMI\\s*(NOTE\\s*)?[A-Z]?\\d{1,2}|POCO\\s*[A-Z]\\d{1,2}|XIAOMI\\s*\\d{2}|MI\\s*\\d{1,2})\\w*\\b.*|^\\d{2}[A-Z]?\\b.*");
            default:
                return MARCAS_TELEFONO.contains(marca) && modelo.matches("(?s).*\\d+\\s*(GB|TB)\\b.*");
        }
    }

    // ------------------------------------------------------------------ extracción

    private static final Pattern CONDICION = Pattern.compile(
            "\\b(REACONDICIONADO|REACONDICIONADA|REFURBISHED|REFURB|USADO|USADA|SEMINUEVO|SEMI\\s*NUEVO|OPEN\\s*BOX|"
            + "EXHIBICION|CPO|GRADO\\s*[ABC]|NUEVO|NUEVA|SELLADO|SELLADA|NEW)\\b");
    private static final Pattern SIM_Y_ESIM = Pattern.compile("\\b(NANO\\s*)?SIM\\s*\\+\\s*ESIM\\b|\\bESIM\\s*\\+\\s*(NANO\\s*)?SIM\\b");
    private static final Pattern SOLO_ESIM = Pattern.compile("\\b(SOLO\\s*)?ESIM(\\s*ONLY)?\\b");
    private static final Pattern DUAL_SIM = Pattern.compile("\\b(DUALSIM|DUAL\\s*CHIP|DS)\\b");
    private static final Pattern SIM_FISICA = Pattern.compile("\\b(SIM\\s*FISIC[AO]|NANO\\s*SIM|CHIP\\s*FISICO|PHYSICAL\\s*SIM)\\b");
    // RAM virtual: primero las formas combinadas ("8GB+8GB RAM", "(4+4)GB RAM") que traen la física adelante.
    private static final Pattern RAM_FISICA_MAS_VIRTUAL = Pattern.compile(
            "(?<![\\d.])\\(?(\\d{1,2})\\s*(?:GB)?\\s*\\+\\s*(\\d{1,2})\\s*\\)?\\s*GB\\s*(?:DE\\s*)?RAM\\b(?:\\s*(?:VIRTUAL|EXTENDIDA|EXPANDIBLE|DINAMICA))?");
    private static final Pattern RAM_VIRTUAL = Pattern.compile(
            "\\+?\\s*(\\d{1,2})\\s*GB\\s*(?:DE\\s*)?(?:RAM\\s*)?(?:VIRTUAL|EXTENDIDA|EXPANDIBLE|DINAMICA|ADICIONAL)\\b"
            + "|\\b(?:RAM\\s*)?(?:VIRTUAL|EXTENDIDA|EXPANDIBLE|DINAMICA)\\s*:?\\s*(?:DE\\s*)?(?:HASTA\\s*)?\\+?\\s*(\\d{1,2})\\s*GB");
    private static final Pattern RAM_EXPLICITA = Pattern.compile(
            "(?<![\\d.])(\\d{1,2})\\s*GB\\s*(?:DE\\s*)?RAM\\b|\\bRAM\\s*:?\\s*(?:DE\\s*)?(\\d{1,2})\\s*GB\\b");
    private static final Pattern PAR_RAM_ALMACENAMIENTO = Pattern.compile(
            "(?<![\\d.])(\\d{1,2})\\s*(?:GB)?\\s*/\\s*(\\d{2,4})\\s*(GB|TB)?(?![\\d.])");
    private static final Pattern PAR_ALMACENAMIENTO_RAM = Pattern.compile(
            "(?<![\\d.])(\\d{2,4})\\s*GB\\s*/\\s*(\\d{1,2})\\s*GB(?![\\d.])");
    private static final Pattern CAPACIDAD = Pattern.compile("(?<![\\d.,])(\\d+)\\s*(GB|TB)\\b");
    private static final Pattern CONECTIVIDAD = Pattern.compile("(?<![A-Z0-9])([345])G(?![A-Z0-9])|\\bLTE\\b");
    private static final Pattern REGION = Pattern.compile("\\b(GLOBAL|CHINA|USA|LATAM|EUROPA|EUROPEA|INDIA|JAPON|HONG\\s*KONG)\\b");
    private static final Pattern COMBO = Pattern.compile("\\b(COMBO|KIT|BUNDLE|PACK)\\b(.*)$|\\s\\+\\s+(?![\\d(])([A-Z].*)$");

    /** Colores con sinónimos en castellano e inglés. Las frases compuestas van primero. */
    private static final List<String[]> COLORES = List.of(
            new String[]{"TITANIO NATURAL|NATURAL TITANIUM", "titanio-natural"},
            new String[]{"TITANIO NEGRO|BLACK TITANIUM", "titanio-negro"},
            new String[]{"TITANIO BLANCO|WHITE TITANIUM", "titanio-blanco"},
            new String[]{"TITANIO AZUL|BLUE TITANIUM", "titanio-azul"},
            new String[]{"TITANIO DESIERTO|DESERT TITANIUM", "titanio-desierto"},
            new String[]{"AZUL CIELO|SKY BLUE|CELESTE|LIGHT BLUE", "celeste"},
            new String[]{"MEDIANOCHE|MIDNIGHT", "medianoche"},
            new String[]{"BLANCO ESTELAR|STARLIGHT", "blanco-estelar"},
            new String[]{"NEGRO|NEGRA|BLACK", "negro"},
            new String[]{"BLANCO|BLANCA|WHITE", "blanco"},
            new String[]{"AZUL|BLUE|NAVY", "azul"},
            new String[]{"VERDE|GREEN|MINT|MENTA", "verde"},
            // "Red" no: en castellano es casi siempre "red 4G", no el color.
            new String[]{"ROJO|ROJA|\\(?PRODUCT\\)?\\s*RED", "rojo"},
            new String[]{"ROSA|ROSADO|ROSADA|PINK", "rosa"},
            new String[]{"VIOLETA|PURPURA|PURPLE|MORADO|LILA|LAVANDA|LAVENDER", "violeta"},
            new String[]{"GRIS|GRAY|GREY|GRAFITO|GRAPHITE", "gris"},
            new String[]{"PLATA|PLATEADO|PLATEADA|SILVER", "plata"},
            new String[]{"DORADO|DORADA|GOLD", "dorado"},
            new String[]{"AMARILLO|AMARILLA|YELLOW", "amarillo"},
            new String[]{"NARANJA|ORANGE", "naranja"},
            new String[]{"CREMA|CREAM|BEIGE", "crema"},
            new String[]{"TITANIO|TITANIUM", "titanio"});

    private static final Pattern RUIDO = Pattern.compile(
            "\\b(RAM|GB|TB|DE|CON|Y|VERSION|LIBERADO|LIBERADA|LIBRE|SMARTPHONE|CELULAR|TELEFONO|MOVIL|INTERNO|"
            + "INTERNA|ALMACENAMIENTO|MEMORIA|COLOR|RED|CHIP|SIM)\\b");

    /**
     * Saca del nombre lo que es atributo y devuelve lo que queda como modelo base ("g04",
     * "galaxy a16" → "a16", "iphone 17 pro"). Cada atributo encontrado se borra del texto, así no
     * se lo vuelve a leer como parte del modelo ni como otro atributo.
     */
    private String extraerDelNombre(String marca, String modelo, Extraido ex) {
        String t = " " + modelo + " ";
        // La marca repetida adelante no es parte del modelo ("Motorola" + "Motorola G04").
        t = t.replaceAll("\\b" + Pattern.quote(marca) + "\\b", " ");
        if (marca.equals("MOTOROLA")) t = t.replaceAll("\\bMOTO\\b", " ");
        if (marca.equals("SAMSUNG")) t = t.replaceAll("\\bGALAXY\\b", " ");
        if (marca.equals("APPLE")) t = t.replaceAll("\\bAPPLE\\b", " ");

        t = extraerComunes(t, ex, "nombre");

        // "Pro+" no es "Pro": el "+" pegado a una palabra es parte del modelo.
        t = t.replaceAll("([A-Z0-9])\\+", "$1 PLUS ");

        Matcher m = PAR_RAM_ALMACENAMIENTO.matcher(t);
        while (m.find()) {
            int ram = Integer.parseInt(m.group(1));
            int alm = gb(m.group(2), m.group(3));
            if (ram < alm) {
                ex.poner("ram_gb", String.valueOf(ram), "nombre");
                ex.poner("almacenamiento_gb", String.valueOf(alm), "nombre");
                t = t.replace(m.group(), " ");
                m = PAR_RAM_ALMACENAMIENTO.matcher(t);
            }
        }
        m = PAR_ALMACENAMIENTO_RAM.matcher(t);
        if (m.find() && Integer.parseInt(m.group(2)) < Integer.parseInt(m.group(1))) {
            ex.poner("almacenamiento_gb", m.group(1), "nombre");
            ex.poner("ram_gb", m.group(2), "nombre");
            t = t.replace(m.group(), " ");
        }
        t = capacidadesSueltas(t, ex, "nombre");

        m = COMBO.matcher(t);
        if (m.find()) {
            String resto = m.group(2) != null ? m.group(2) : m.group(3);
            String combo = alnum(resto == null || resto.isBlank() ? m.group(1) : resto);
            ex.poner("combo", combo.isEmpty() ? "combo" : combo, "nombre");
            t = t.substring(0, m.start()) + " ";
        }

        t = RUIDO.matcher(t).replaceAll(" ");
        return String.join(" ", t.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")).trim();
    }

    /** Lo que se lee igual del nombre que de un texto libre de especificaciones. */
    private String extraerComunes(String t, Extraido ex, String fuente) {
        Matcher m = CONDICION.matcher(t);
        while (m.find()) ex.poner("condicion", condicion(m.group(1)), fuente);
        t = CONDICION.matcher(t).replaceAll(" ");

        TreeSet<String> sims = new TreeSet<>();
        if (SIM_Y_ESIM.matcher(t).find()) { sims.add("sim+esim"); t = SIM_Y_ESIM.matcher(t).replaceAll(" "); }
        if (SOLO_ESIM.matcher(t).find()) { sims.add("esim"); t = SOLO_ESIM.matcher(t).replaceAll(" "); }
        if (DUAL_SIM.matcher(t).find()) { sims.add("dualsim"); t = DUAL_SIM.matcher(t).replaceAll(" "); }
        if (SIM_FISICA.matcher(t).find()) { sims.add("sim"); t = SIM_FISICA.matcher(t).replaceAll(" "); }
        if (!sims.isEmpty()) ex.poner("sim", String.join("+", sims), fuente);

        // "8GB+8GB RAM" es 8 físicos + 8 virtuales; "64GB + 4GB RAM" es almacenamiento + RAM y no
        // se toca acá: la RAM física de un teléfono no pasa de 24GB.
        m = RAM_FISICA_MAS_VIRTUAL.matcher(t);
        StringBuilder sinVirtual = new StringBuilder();
        while (m.find()) {
            if (Integer.parseInt(m.group(1)) > 24) continue;
            ex.poner("ram_gb", m.group(1), fuente);
            ex.ramVirtual = Integer.parseInt(m.group(2));
            m.appendReplacement(sinVirtual, " ");
        }
        m.appendTail(sinVirtual);
        t = sinVirtual.toString();
        m = RAM_VIRTUAL.matcher(t);
        while (m.find()) ex.ramVirtual = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
        t = RAM_VIRTUAL.matcher(t).replaceAll(" ");
        m = RAM_EXPLICITA.matcher(t);
        while (m.find()) ex.poner("ram_gb", m.group(1) != null ? m.group(1) : m.group(2), fuente);
        t = RAM_EXPLICITA.matcher(t).replaceAll(" ");

        String red = conectividadMaxima(t);
        if (red != null) ex.poner("conectividad", red, fuente);
        t = CONECTIVIDAD.matcher(t).replaceAll(" ");

        m = REGION.matcher(t);
        while (m.find()) ex.poner("region", m.group(1).replaceAll("\\s+", "").toLowerCase(Locale.ROOT)
                .replace("europea", "europa"), fuente);
        t = REGION.matcher(t).replaceAll(" ");

        TreeSet<String> colores = new TreeSet<>();
        t = sacarColores(t, colores);
        // En el nombre, varias palabras de color son UN color ("Midnight Black"). En las
        // especificaciones suelen ser la lista de colores disponibles ("Azul, Blanco, Negro"), que no
        // dice cuál es este artículo: ahí solo cuenta si hay uno.
        if (!colores.isEmpty() && (fuente.equals("nombre") || colores.size() == 1)) {
            ex.poner("color", String.join("-", colores), fuente);
        }
        return t;
    }

    /** Colores del vocabulario que aparecen en el texto; los devuelve en {@code colores} y los saca del texto. */
    private static String sacarColores(String t, TreeSet<String> colores) {
        for (String[] c : COLORES) {
            Pattern p = Pattern.compile("\\b(" + c[0] + ")\\b");
            if (p.matcher(t).find()) {
                colores.add(c[1]);
                t = p.matcher(t).replaceAll(" ");
            }
        }
        return t;
    }

    /**
     * La generación más alta que se menciona: un teléfono 5G también soporta 4G, y las fichas
     * suelen listar todas las redes ("5G, 4G LTE, 3G"). Leerlo como contradicción mandaba a
     * revisión a una de cada diez fichas del catálogo real.
     */
    private static String conectividadMaxima(String t) {
        Matcher m = CONECTIVIDAD.matcher(t);
        int max = 0;
        while (m.find()) max = Math.max(max, m.group(1) != null ? Integer.parseInt(m.group(1)) : 4);
        return max == 0 ? null : max + "g";
    }

    /**
     * Capacidades sin la palabra RAM. Si ya se sabe la RAM, lo que queda es almacenamiento. Si no:
     * dos valores con el chico ≤ 24 y el grande ≥ 32 son RAM y almacenamiento (es como se escriben
     * siempre); un único valor ≥ 32 es almacenamiento; uno solo ≤ 24 es ambiguo.
     */
    private String capacidadesSueltas(String t, Extraido ex, String fuente) {
        List<Integer> valores = new ArrayList<>();
        Matcher m = CAPACIDAD.matcher(t);
        while (m.find()) {
            int v = gb(m.group(1), m.group(2));
            if (!valores.contains(v)) valores.add(v);
        }
        t = CAPACIDAD.matcher(t).replaceAll(" ");
        if (valores.isEmpty()) return t;
        if (ex.valores.containsKey("ram_gb") || ex.valores.containsKey("almacenamiento_gb")) {
            for (int v : valores) {
                if (String.valueOf(v).equals(ex.valores.get("ram_gb"))) continue;
                ex.poner("almacenamiento_gb", String.valueOf(v), fuente);
            }
            return t;
        }
        valores.sort(Integer::compare);
        if (valores.size() == 1) {
            if (valores.get(0) >= 32) ex.poner("almacenamiento_gb", String.valueOf(valores.get(0)), fuente);
            else ex.capacidadAmbigua = valores.get(0);
        } else if (valores.size() == 2 && valores.get(0) <= 24 && valores.get(1) >= 32) {
            ex.poner("ram_gb", String.valueOf(valores.get(0)), fuente);
            ex.poner("almacenamiento_gb", String.valueOf(valores.get(1)), fuente);
        } else {
            ex.problemas.add("El " + fuente + " tiene capacidades que no se pueden ubicar: " + valores + " GB.");
        }
        return t;
    }

    private static final Pattern NUMERO = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(GB|TB)?");

    /** El objeto `especificaciones` del JSON: las claves conocidas mandan, el resto se lee como texto. */
    private void extraerDeMapa(Map<String, ?> esp, Extraido ex) {
        StringBuilder libre = new StringBuilder();
        for (Map.Entry<String, ?> e : esp.entrySet()) {
            if (e.getValue() == null) continue;
            String clave = norm(e.getKey()).replaceAll("[^A-Z]", "");
            String valor = norm(String.valueOf(e.getValue()));
            if (valor.isBlank()) continue;
            switch (clave) {
                case "RAM", "MEMORIARAM", "RAMGB" -> {
                    Extraido tmp = new Extraido();
                    String resto = extraerComunes(" " + valor + " ", tmp, "especificaciones");
                    if (tmp.ramVirtual != null) ex.ramVirtual = tmp.ramVirtual;
                    if (tmp.valores.containsKey("ram_gb")) {
                        ex.poner("ram_gb", tmp.valores.get("ram_gb"), "especificaciones");
                    } else {
                        Matcher m = NUMERO.matcher(resto);
                        if (m.find()) ex.poner("ram_gb", String.valueOf(gb(m.group(1), m.group(2))), "especificaciones");
                    }
                }
                case "ALMACENAMIENTO", "CAPACIDAD", "ROM", "MEMORIAINTERNA", "ALMACENAMIENTOGB", "STORAGE" -> {
                    Matcher m = NUMERO.matcher(valor);
                    if (m.find()) ex.poner("almacenamiento_gb", String.valueOf(gb(m.group(1), m.group(2))), "especificaciones");
                }
                case "CONECTIVIDAD", "RED", "REDES", "NETWORK", "CONEXION" ->
                        ex.poner("conectividad", conectividadMaxima(valor), "especificaciones");
                case "COLOR", "COLORES" -> {
                    // Uno reconocido: ese. Ninguno: el texto tal cual ("Azul Glaciar" sin vocabulario
                    // sigue distinguiendo). Varios: es la lista de disponibles y no dice cuál es este.
                    TreeSet<String> colores = new TreeSet<>();
                    sacarColores(" " + valor + " ", colores);
                    if (colores.size() == 1) ex.poner("color", colores.first(), "especificaciones");
                    else if (colores.isEmpty()) ex.poner("color", alnum(valor), "especificaciones");
                }
                case "SIM", "CHIP", "TIPOSIM" -> {
                    Extraido tmp = new Extraido();
                    extraerComunes(" " + valor + " ", tmp, "especificaciones");
                    if (tmp.valores.containsKey("sim")) ex.poner("sim", tmp.valores.get("sim"), "especificaciones");
                }
                case "CONDICION", "ESTADO", "CONDITION" -> {
                    Matcher m = CONDICION.matcher(valor);
                    if (m.find()) ex.poner("condicion", condicion(m.group(1)), "especificaciones");
                }
                default -> libre.append(' ').append(valor);
            }
        }
        if (!libre.isEmpty()) extraerDeTexto(libre.toString(), ex, false);
    }

    /**
     * Texto libre de especificaciones. Solo se leen datos con marca propia (4G, eSIM, "8GB RAM",
     * un color). Las capacidades sueltas se leen únicamente si {@code capacidades} es true — para
     * el texto guardado de un producto, que es la única fuente que tiene ("4GB · 64GB"); en el
     * campo "otros" del JSON un "128GB" puede ser la microSD que acepta.
     */
    private void extraerDeTexto(String texto, Extraido ex, boolean capacidades) {
        String t = extraerComunes(" " + texto + " ", ex, "especificaciones");
        if (capacidades) capacidadesSueltas(t, ex, "especificaciones");
    }

    // ------------------------------------------------------------------ utilidades

    static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.toUpperCase(Locale.ROOT).replaceAll("[®™]", "");
        t = t.replaceAll("\\bE[-\\s]?SIM\\b", "ESIM").replaceAll("\\bDUAL[-\\s]?SIM\\b", "DUALSIM");
        return t.replaceAll("\\s+", " ");
    }

    private static String alnum(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static int gb(String numero, String unidad) {
        double v = Double.parseDouble(numero.replace(',', '.'));
        return (int) Math.round("TB".equals(unidad) ? v * 1024 : v);
    }

    private static String condicion(String palabra) {
        String p = palabra.replaceAll("\\s+", "");
        if (p.startsWith("NUEV") || p.startsWith("SELLAD") || p.equals("NEW")) return "nuevo";
        if (p.startsWith("USAD") || p.startsWith("SEMI")) return "usado";
        if (p.startsWith("OPENBOX") || p.equals("EXHIBICION")) return "openbox";
        if (p.startsWith("GRADO")) return "reacondicionado-" + p.substring(5).toLowerCase(Locale.ROOT);
        return "reacondicionado";
    }

    private static String nombre(String campo) {
        return switch (campo) {
            case "almacenamiento_gb" -> "almacenamiento";
            case "ram_gb" -> "RAM";
            case "condicion" -> "condición";
            case "region" -> "región";
            default -> campo;
        };
    }
}
