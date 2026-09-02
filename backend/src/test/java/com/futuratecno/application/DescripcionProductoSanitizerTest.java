package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescripcionProductoSanitizerTest {

    @Test
    void limpiaTablaYEntidadesHtmlDeInvid() {
        String html = "<table><tbody><tr><td><strong>Categor&iacute;a:</strong></td>"
                + "<td>Cat 5e</td></tr><tr><td><strong>Material</strong></td>"
                + "<td>CCA (Aleaci&oacute;n de Cobre y Aluminio)</td></tr></tbody></table>";

        assertEquals("Categoría: · Cat 5e · Material · CCA (Aleación de Cobre y Aluminio)",
                DescripcionProductoSanitizer.limpiar(html));
    }

    @Test
    void conservaTextoPlano() {
        assertEquals("USB-C · 1 metro", DescripcionProductoSanitizer.limpiar("USB-C · 1 metro"));
    }
}
