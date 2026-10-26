package com.futuratecno.application;

import com.futuratecno.domain.Proveedor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de las listas de Cozzo (pedido del usuario, 2026-10-26). Cozzo reenvía en el día varias
 * listas de fuentes distintas, cada una con su formato, así que el mismo celular llega dos o tres
 * veces con precios distintos. Acordado:
 * <ul>
 *   <li>Se saltean los artículos con "ACR", "CH" (versión China) o del bloque "220V CH SPECS".
 *       "Importado" sí entra.</li>
 *   <li>"C/CARG" es OTRO producto que el mismo sin cargador: viaja como combo ("+ Cargador"), que la
 *       identidad distingue. "S/CARG" se borra: es el teléfono común, y escrito "sin cargador" la IA
 *       lo tomaría como condición especial y lo descartaría.</li>
 *   <li>El mismo artículo con dos precios: vale el MÁS ALTO, también entre listas del mismo día.</li>
 *   <li>Mínimo US$ 50. No se da de baja nada: ninguna lista es todo su stock.</li>
 * </ul>
 */
public final class ReglasCozzo {

    public static final BigDecimal PRECIO_MINIMO_USD = new BigDecimal("50");

    private static final Pattern PRECIO = Pattern.compile("(?i)(\\$|USD|US\\$|U\\$S)\\s?\\d");
    /** Abreviaturas que el usuario no quiere cargar. "CH" suelto: no atrapa "CHARGING" ni "CHERRY". */
    static final Pattern NO_CARGAR = Pattern.compile("(?i)(?<![\\p{L}\\d])(ACR|CH)(?![\\p{L}\\d])|220\\s?V");
    private static final Pattern CON_CARGADOR = Pattern.compile("(?i)\\bC\\s?/\\s?CARG(ADOR)?\\b\\.?");
    private static final Pattern SIN_CARGADOR = Pattern.compile("(?i)\\bS\\s?/\\s?CARG(ADOR)?\\b\\.?");

    private ReglasCozzo() {}

    public static boolean aplica(Proveedor proveedor) {
        return proveedor != null && proveedor.getNombre() != null
                && proveedor.getNombre().toUpperCase(java.util.Locale.ROOT).contains("COZZO");
    }

    /**
     * Prepara el texto antes de la IA. Un encabezado corto sin precio que dice "CH" o "220V" (el de
     * Dyson: "*PRODUCTOS DYSON (220V)* CH SPECS") saltea todo su bloque hasta la próxima línea en
     * blanco o el próximo encabezado con asteriscos: ahí el precio va en otra línea que la del nombre.
     * El índice de rubros del principio también nombra "CH SPE CS", pero es largo y no se toma.
     */
    public static String texto(String texto) {
        if (texto == null) return null;
        List<String> salida = new ArrayList<>();
        boolean salteando = false;
        for (String linea : texto.split("\\R", -1)) {
            String t = linea.strip();
            if (salteando) {
                if (t.isEmpty() || t.startsWith("*")) salteando = false;
                else continue;
            }
            boolean conPrecio = PRECIO.matcher(t).find();
            if (NO_CARGAR.matcher(t).find()) {
                if (!conPrecio && t.length() <= 60) salteando = true;
                if (conPrecio || salteando) continue;
            }
            String l = CON_CARGADOR.matcher(linea).replaceAll("+ Cargador");
            salida.add(SIN_CARGADOR.matcher(l).replaceAll(""));
        }
        return String.join("\n", salida);
    }

    /** Motivo para no cargar un artículo ya generado, o null. Red de seguridad por si la IA dejó pasar algo. */
    public static String motivo(String modelo, String modeloExacto, BigDecimal precio) {
        if (precio != null && precio.compareTo(PRECIO_MINIMO_USD) < 0)
            return "Cozzo: por debajo de US$ " + PRECIO_MINIMO_USD + ".";
        for (String s : new String[]{modelo, modeloExacto})
            if (s != null && NO_CARGAR.matcher(s).find()) return "Cozzo: no se cargan ACR ni versiones CH / 220V.";
        return null;
    }
}
