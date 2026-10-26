package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KadabraImportServiceTest {

    @Test
    void preparaElTextoDeUnaPestana() {
        String csv = String.join("\n",
                "\"MONOPATINES\",\"\"",
                "\"🛴 KICKBOOT KB-FS (Autonomía 35 km)\",\"\"",
                "\"X1 370 USD 🔥\",\"\"",
                "\"X3 360 USD 🔥\",\"\"",
                "\"X5 345USD 🔥\",\"\"",
                "\"💻 MACBOOK AIR M2 REFURBISHED USD 600\",\"\"",
                "\"💻 IPHONE 15 USADO 90% BATERIA USD 500\",\"\"",
                "\"💻 MACBOOK NEO A18 PRO 13” 8GB, 256GB\",\"USD 835 🔥\"");
        String t = KadabraImportService.texto("MONOPATINES ", csv);
        assertTrue(t.startsWith("Pestaña: MONOPATINES\n"), t);
        assertTrue(t.contains("🛴 KICKBOOT KB-FS (Autonomía 35 km) USD 370"), t);
        assertFalse(t.contains("360") || t.contains("345"), t);
        assertFalse(t.contains("REFURBISHED") || t.contains("USADO"), t);
        assertTrue(t.contains("MACBOOK NEO A18 PRO 13” 8GB, 256GB USD 835"), t);
        assertFalse(t.contains("🔥"), t);
    }

    @Test
    void parteLasPestanasLargasArrastrandoElEncabezado() {
        StringBuilder sb = new StringBuilder("Pestaña: APPLE\n");
        for (int i = 0; i < 600; i++) sb.append("💻 MACBOOK AIR M5 13” 16GB, 512GB VARIANTE ").append(i).append(" USD 1.370\n");
        var partes = KadabraImportService.partes(sb.toString());
        assertTrue(partes.size() > 1);
        partes.forEach(p -> {
            assertTrue(p.startsWith("Pestaña: APPLE\n"));
            assertTrue(p.length() <= 18000);
        });
        assertEquals(600, partes.stream().mapToLong(p -> p.lines().filter(l -> l.startsWith("💻")).count()).sum());
    }
}
