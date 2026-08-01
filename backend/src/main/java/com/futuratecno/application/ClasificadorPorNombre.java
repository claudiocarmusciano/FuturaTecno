package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Deduce la categoría de un producto a partir de su <b>nombre</b>, sin IA.
 *
 * <p>Existe porque el mapeo por categoría cruda no cubre a Elit: su vocabulario no calza con
 * nuestro árbol y quedaban 940 productos (60% del catálogo) sin categoría, y por lo tanto sin
 * peso resoluble y sin cotización de envío.
 *
 * <p><b>Solo mira el encabezado del modelo</b> (las primeras palabras), no el nombre completo.
 * Elit nombra "&lt;Tipo de producto&gt; &lt;marca&gt; &lt;especificaciones...&gt;", y las
 * especificaciones mienten: "Procesador AMD Ryzen 5 con cooler" no es un cooler y una notebook
 * con "16GB SODIMM" no es una memoria. Medido sobre el catálogo real, mirar el nombre entero da
 * 31% de clasificaciones equivocadas; mirar solo el encabezado lo baja a 7%.
 *
 * <p>La subcategoría sí se decide con el texto completo, porque ahí viven los datos que la
 * distinguen (el chipset de un motherboard, los Hz de un monitor, si un switch es administrable).
 *
 * <p>Ante la duda devuelve null: un producto sin categoría se ve en Admin → Productos con el
 * filtro "solo sin categoría" y se corrige a mano, mientras que uno mal categorizado hereda el
 * peso equivocado y se cotiza mal el envío sin que nadie se entere.
 */
@Component
public class ClasificadorPorNombre {

    /** Cuántas palabras del modelo se consideran "el tipo de producto". */
    private static final int PALABRAS_ENCABEZADO = 4;

    private record Regla(Pattern patron, String path) {
        Regla(String regex, String path) {
            this(Pattern.compile(regex), path);
        }
    }

    /**
     * Orden = prioridad: la primera que matchea gana. Por eso lo específico va antes que lo
     * genérico ("water cooler" antes que "cooler", "mousepad" antes que "mouse").
     */
    private static final List<Regla> REGLAS = construirReglas();

    private static List<Regla> construirReglas() {
        List<Regla> r = new ArrayList<>();
        // Refrigeración — "water cooler" primero: si no, todo cooler líquido cae en Fans.
        r.add(new Regla("water ?cooler|refrigeracion liquida", "Coolers > Watercoolers"));
        r.add(new Regla("\\bcooler\\b|\\bfan\\b|ventilador|disipador", "Coolers > Fans"));
        // Almacenamiento
        r.add(new Regla("pen ?drive", "Almacenamiento > Pen Drive"));
        r.add(new Regla("tarjeta (de )?(memoria|sd)|micro ?sd|sdxc|sdhc", "Almacenamiento > Tarjetas de memoria"));
        r.add(new Regla("carry disk", "Discos Rígidos / SSD > Carry Disk"));
        r.add(new Regla("disco (rigido )?externo|disco externo", "Discos Rígidos / SSD > Disco Rígido Externo"));
        r.add(new Regla("\\bnas\\b", "Discos Rígidos / SSD > Disco Rígido NAS"));
        // Periféricos — "mousepad" y "combo" antes que "mouse"/"teclado".
        r.add(new Regla("mouse ?pad|pad mouse", "Periféricos > Mousepads"));
        r.add(new Regla("combo|kit teclado|teclado y mouse", "Periféricos > Teclado + Mouse"));
        r.add(new Regla("teclado", "Periféricos > Teclados"));
        r.add(new Regla("mouses?\\b", "Periféricos > Mouse"));
        r.add(new Regla("auricular|headset|vincha", "Periféricos > Auriculares"));
        r.add(new Regla("microfono", "Periféricos > Micrófonos"));
        r.add(new Regla("parlante|speaker|barra de sonido|soundbar", "Periféricos > Parlantes"));
        r.add(new Regla("web ?cam|camara web", "Periféricos > Web Cam"));
        r.add(new Regla("power ?bank|power klip|\\bm ?ah\\b", "Periféricos > Power Banks"));
        // Impresión
        r.add(new Regla("scanner|escaner", "Scanners"));
        r.add(new Regla("plotter", "Impresoras > Ink Jet"));
        r.add(new Regla("proyector", "Proyectores"));
        // Energía
        r.add(new Regla("\\bups\\b", "Energía > UPS"));
        r.add(new Regla("estabilizador|protector (de )?tension|panel solar|generador", "Energía > Estabilizadores"));
        // Gabinetes
        r.add(new Regla("fuente", "Gabinetes y Fuentes > Fuentes de Alimentación"));
        r.add(new Regla("gabinete", "Gabinetes y Fuentes > Gabinetes sin Fuente"));
        // Conectividad — "access point" tolera el typo "acces point" que manda Elit.
        r.add(new Regla("acce?ss? ?point|extensor de rango|repetidor|\\bmesh\\b",
                "Conectividad > Access Point y Extensores de Rango"));
        r.add(new Regla("modem|\\bgpon\\b|\\bonu\\b", "Conectividad > Modem ADSL y GPON"));
        r.add(new Regla("camara", "Conectividad > Cámaras IP"));
        r.add(new Regla("\\bcable\\b|patch cord", "Conectividad > Cables"));
        r.add(new Regla("placa de red.*(wifi|inalambric).*usb|adaptador usb wifi",
                "Conectividad > Placas de Red WiFi USB"));
        r.add(new Regla("placa de red.*(wifi|inalambric)", "Conectividad > Placas de Red WiFi PCI"));
        r.add(new Regla("placa de red|adaptador de red|\\bhub\\b|adaptador|docking|conversor",
                "Conectividad > Placas de Red Ethernet y Adaptadores USB"));
        r.add(new Regla("\\bpoe\\b", "Conectividad > POE (Power Over Ethernet)"));
        r.add(new Regla("bluetooth", "Conectividad > Accesorios Bluetooth"));
        r.add(new Regla("smart home|domotica|enchufe inteligente|timbre", "Conectividad > Smart Home"));
        // Computadoras
        r.add(new Regla("all in one", "Computadoras > All in One"));
        r.add(new Regla("mini ?pc|\\bpc\\b.*\\bmini\\b", "Computadoras > Mini PC"));
        r.add(new Regla("\\bpc\\b|desktop", "Computadoras > PC"));
        r.add(new Regla("tablet", "Tablets"));
        // Mobiliario
        r.add(new Regla("silla", "Sillas y escritorios > Sillas"));
        r.add(new Regla("escritorio", "Sillas y escritorios > Escritorios"));
        // Accesorios varios: cierra la cola de cosas que no son de ningún rubro técnico.
        r.add(new Regla("joystick|volantes?\\b|gamepad|mochila|morral|maleta|pedales|consola"
                + "|tripode|soporte|presentador|lampara|luz led|destructora", "Accesorios"));
        return r;
    }

    /** Devuelve el path de la hoja ("Padre > Hoja") o null si no se puede afirmar. */
    public String clasificar(Producto producto) {
        if (producto == null) return null;
        String encabezado = encabezado(producto.getModelo());
        String completo = normalizar((producto.getMarca() == null ? "" : producto.getMarca())
                + " " + (producto.getModelo() == null ? "" : producto.getModelo()));
        if (encabezado.isBlank()) return null;

        String porSubtipo = resolverSubtipo(encabezado, completo);
        if (porSubtipo != null) return porSubtipo;

        for (Regla regla : REGLAS) {
            if (regla.patron().matcher(encabezado).find()) return regla.path();
        }
        return null;
    }

    /**
     * Familias donde el tipo se reconoce en el encabezado pero la hoja depende de las
     * especificaciones (que están en el resto del nombre).
     */
    private String resolverSubtipo(String cab, String full) {
        if (contiene(cab, "motherboard|\\bmother\\b|placa madre")) {
            return contiene(full, "\\bamd\\b|\\bam[45]\\b|\\bb[456]50\\b|\\bx[456]70\\b|\\ba520\\b")
                    ? "Mothers > Plataforma AMD" : "Mothers > Plataforma Intel";
        }
        if (contiene(cab, "placa de video|\\bvga\\b|placa.*(radeon|geforce)")) {
            if (contiene(full, "radeon pro|quadro")) return "Placas de video > Línea Quadro/Radeon Pro";
            if (contiene(full, "radeon|\\brx ?\\d{4}")) return "Placas de video > Línea AMD RADEON";
            if (contiene(full, "\\barc\\b")) return "Placas de video > Línea Intel Arc";
            return "Placas de video > Línea NVIDIA GEFORCE";
        }
        if (contiene(cab, "memoria|sodimm")) {
            if (contiene(cab, "pen ?drive|\\busb\\b")) return "Almacenamiento > Pen Drive";
            if (contiene(full, "sodimm|notebook")) return "Memorias RAM > Memoria Sodimm";
            for (String d : new String[]{"5", "4", "3", "2"}) {
                if (contiene(full, "\\bddr" + d + "\\b")) return "Memorias RAM > Memoria DDR" + d;
            }
            return "Memorias RAM > Memoria DDR4";
        }
        if (contiene(cab, "procesador|\\bcpu\\b")) {
            return contiene(full, "\\bamd\\b|ryzen|athlon")
                    ? "Microprocesadores > AMD" : "Microprocesadores > Intel";
        }
        // Los consumibles de HP tienen hoja propia en el árbol, separada del resto.
        // Ojo con el orden: "Cartucho de Tinta" contiene "tinta" pero es un cartucho, no una
        // botella de recarga — el tipo se decide por la primera palabra, no por mencionar tinta.
        if (contiene(cab, "cartucho|\\btoner\\b|tambor|\\bdrum\\b|botella de tinta|cabezal|\\btinta\\b")) {
            if (contiene(full, "\\bhp\\b")) return "Consumibles > Consumibles HP";
            if (contiene(cab, "cartucho|\\btoner\\b|tambor|\\bdrum\\b|cabezal")) return "Consumibles > Cartuchos";
            return "Consumibles > Tintas";
        }
        if (contiene(cab, "disco|\\bssd\\b|\\bhdd\\b")) {
            if (contiene(full, "\\bm\\.?2\\b|nvme")) return "Discos Rígidos / SSD > Disco SSD M2";
            if (contiene(full, "\\bssd\\b")) return "Discos Rígidos / SSD > Disco SSD";
            if (contiene(full, "notebook|2\\.5")) return "Discos Rígidos / SSD > Disco Rígido Notebook";
            return "Discos Rígidos / SSD > Disco Rígido SATA";
        }
        if (contiene(cab, "monitor")) {
            return contiene(full, "gamer|gaming|\\b1[4-9]\\d ?hz|\\b[2-3]\\d\\d ?hz|curvo")
                    ? "Monitores > Monitor Gamer" : "Monitores > Monitor Consumo";
        }
        if (contiene(cab, "notebook|laptop")) {
            return contiene(full, "gamer|gaming|\\brtx\\b|\\bgtx\\b")
                    ? "Notebooks > Gamer" : "Notebooks > Consumo";
        }
        if (contiene(cab, "switch")) {
            return contiene(full, "administrable|managed|\\bsmart\\b|\\bl[23]\\b")
                    ? "Conectividad > Switches Administrables" : "Conectividad > Switches No Administrables";
        }
        if (contiene(cab, "router")) {
            return contiene(full, "wireless|wi-?fi|inalambric")
                    ? "Conectividad > Router Wireless" : "Conectividad > Router";
        }
        if (contiene(cab, "impresora|multifuncion")) {
            if (contiene(cab, "multifuncion") || contiene(full, "multifuncion")) return "Impresoras > Multifunción";
            return contiene(full, "laser") ? "Impresoras > Laser" : "Impresoras > Ink Jet";
        }
        return null;
    }

    /** Las primeras palabras del modelo, donde el mayorista pone el tipo de producto. */
    private String encabezado(String modelo) {
        if (modelo == null) return "";
        String[] palabras = normalizar(modelo).split(" ");
        return String.join(" ", java.util.Arrays.copyOfRange(
                palabras, 0, Math.min(PALABRAS_ENCABEZADO, palabras.length)));
    }

    private boolean contiene(String texto, String regex) {
        return Pattern.compile(regex).matcher(texto).find();
    }

    /** Minúsculas, sin tildes y con espacios colapsados, para que las reglas sean simples. */
    private String normalizar(String s) {
        if (s == null) return "";
        String sinTildes = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.replaceAll("\\s+", " ").trim();
    }
}
