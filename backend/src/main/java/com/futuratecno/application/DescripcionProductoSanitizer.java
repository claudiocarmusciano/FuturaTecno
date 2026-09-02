package com.futuratecno.application;

import org.springframework.web.util.HtmlUtils;

/** Convierte las fichas HTML de mayoristas en texto breve y legible para el catálogo. */
final class DescripcionProductoSanitizer {
    private DescripcionProductoSanitizer() {}

    static String limpiar(String valor) {
        if (valor == null || valor.isBlank()) return "";

        // Algunas respuestas llegan con entidades HTML; decodificar dos veces también cubre
        // contenido doblemente escapado (&amp;iacute;).
        String texto = HtmlUtils.htmlUnescape(HtmlUtils.htmlUnescape(valor));
        texto = texto
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(?:p|div|li|tr|table|ul|ol|h[1-6])\\s*>", "\n")
                .replaceAll("(?i)</(?:td|th)\\s*>", " · ")
                .replaceAll("<[^>]+>", " ")
                .replace('\u00a0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *· *(?:· *)+", " · ")
                .replaceAll("(?m)^\\s*·\\s*|\\s*·\\s*$", "")
                .replaceAll("\\n{2,}", "\n")
                .trim();

        // Las fichas en tabla quedan más claras en una sola línea dentro de una tarjeta.
        return texto.replaceAll("\\s*\\n\\s*", " · ").replaceAll("(?: · ){2,}", " · ").trim();
    }
}
