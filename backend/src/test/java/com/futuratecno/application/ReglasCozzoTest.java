package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Con fragmentos de las listas reales de Cozzo del 26/10/2026. */
class ReglasCozzoTest {

    @Test
    void conCargadorEsUnComboYSinCargadorSeBorra() {
        String t = ReglasCozzo.texto("""
                - MOTO G06 4/64 — USD 106 **S/CARG**
                - MOTO G06 4/128 — USD 140 **C/CARG**
                📲 Samsung Galaxy A07 4G 128GB / 4GB RAM — USD 155 (Black / Green) C/CARG""");
        assertFalse(t.contains("S/CARG"));
        assertTrue(t.contains("MOTO G06 4/128 — USD 140 **+ Cargador**"));
        assertTrue(t.contains("(Black / Green) + Cargador"));
    }

    @Test
    void salteaAcrChYElBloqueDyson220v() {
        String t = ReglasCozzo.texto("""
                *💥🍎IPHONE 🆕 - 💻MACBOOK - 💇‍♀️PRODUCTOS DYSON (220V) CH SPE CS - 🧹ASPIRADORAS - 📺TV*
                🔥16E 256 ESIM *$650* BLACK
                🔥16E 256 CH *$650* WHITE
                🔥ACR 16 PRO MAX 256 ESIM *$1066* DESERT
                📲 Xiaomi Redmi A3 4G 128GB / 4GB RAM — USD 122 (Black) Importado
                ❇️AIRPODS 5TH W USB-C CHARGING CASE *$152*

                *PRODUCTOS DYSON (220V)* CH SPECS
                _SECADOR DE PELO CON ACCESORIOS_
                🔥DYSON HD18 HAIR DRYER R PROFESSIONAL (SIN ESTUCHE)
                  - VINCA Blue/Topaz *$557*

                *ASPIRADORAS*
                🔥DYSON V15 FLUFFY ASPIRADORA *$831*""");
        assertTrue(t.contains("16E 256 ESIM *$650* BLACK"));
        assertFalse(t.contains("16E 256 CH"));
        assertFalse(t.contains("ACR 16"));
        assertTrue(t.contains("Importado"));
        assertTrue(t.contains("CHARGING CASE"), "CH solo como palabra suelta");
        assertFalse(t.contains("HD18"));
        assertFalse(t.contains("VINCA"));
        assertTrue(t.contains("DYSON V15"));
        assertTrue(t.contains("💻MACBOOK"), "el índice de rubros es largo y no abre un bloque");
    }

    @Test
    void minimoDe50YAbreviaturasEnElBorrador() {
        assertNotNull(ReglasCozzo.motivo("Kieslect Earbuds", null, new BigDecimal("20")));
        assertNull(ReglasCozzo.motivo("Galaxy A07 4G 128GB + Cargador", null, new BigDecimal("155")));
        assertNotNull(ReglasCozzo.motivo("iPhone 16E 256GB CH", null, new BigDecimal("650")));
        assertNull(ReglasCozzo.motivo("Redmi A3 4G 128GB Importado", null, new BigDecimal("122")));
    }

    @Test
    void elTelefonoConCargadorNoEsUnCargador() {
        assertNull(ExclusionesCarga.motivo("Samsung", "Galaxy A07 + Cargador", null, null, null));
        assertNotNull(ExclusionesCarga.motivo("Apple", "Cargador 20W USB-C", null, null, null));
    }

    @Test
    void conYSinCargadorSonDosProductos() {
        var svc = new IdentidadProductoService();
        var sin = svc.resolver("Samsung", "Galaxy A07 4G 128GB", java.util.Map.of("ram", "4GB"), "Celulares");
        var con = svc.resolver("Samsung", "Galaxy A07 4G 128GB + Cargador", java.util.Map.of("ram", "4GB"), "Celulares");
        assertTrue(sin.resuelta() && con.resuelta());
        assertTrue(con.esTelefono());
        assertNotEquals(sin.clave(), con.clave());
    }
}
