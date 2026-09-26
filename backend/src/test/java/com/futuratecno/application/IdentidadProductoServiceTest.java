package com.futuratecno.application;

import com.futuratecno.application.IdentidadProductoService.Compatibilidad;
import com.futuratecno.application.IdentidadProductoService.Estado;
import com.futuratecno.application.IdentidadProductoService.Resolucion;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentidadProductoServiceTest {

    private final IdentidadProductoService svc = new IdentidadProductoService();

    private Resolucion tel(String marca, String modelo, Map<String, ?> esp) {
        return svc.resolver(marca, modelo, esp, null);
    }

    private static final Map<String, String> G04_4_64 = Map.of("almacenamiento", "64GB", "ram", "4GB");

    @Test
    void losDosEjemplosDeMotorolaConvergenCuandoLosAtributosCoinciden() {
        Resolucion a = tel("Motorola", "G04 4G 64GB / 4GB RAM", G04_4_64);
        Resolucion b = tel("Motorola", "Motorola G04 4G 64GB", G04_4_64);

        assertEquals(Estado.RESUELTA, a.estado(), a.motivos().toString());
        assertEquals(Estado.RESUELTA, b.estado(), b.motivos().toString());
        assertEquals(a.clave(), b.clave());
        assertEquals(Compatibilidad.IGUAL, svc.comparar(a, b));
        assertEquals("g04", a.atributos().get("base"));
        assertEquals("4g", a.atributos().get("conectividad"));
        assertEquals("64", a.atributos().get("almacenamiento_gb"));
        assertEquals("4", a.atributos().get("ram_gb"));
        assertTrue(a.clave().startsWith("tel1|motorola|g04|"));
    }

    @Test
    void almacenamientoDistintoQuedaSeparado() {
        Resolucion a = tel("Motorola", "G04 4G 64GB", G04_4_64);
        Resolucion b = tel("Motorola", "G04 4G 128GB", Map.of("almacenamiento", "128GB", "ram", "4GB"));
        assertNotEquals(a.clave(), b.clave());
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(a, b));
    }

    @Test
    void ramDistintaQuedaSeparada() {
        Resolucion a = tel("Motorola", "G04 4G 64GB", G04_4_64);
        Resolucion b = tel("Motorola", "G04 4G 64GB", Map.of("almacenamiento", "64GB", "ram", "8GB"));
        assertNotEquals(a.clave(), b.clave());
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(a, b));
    }

    @Test
    void unSufijoDeModeloQuedaSeparado() {
        Resolucion g04 = tel("Motorola", "G04 4G 64GB", G04_4_64);
        Resolucion g04s = tel("Motorola", "G04s 4G 64GB", G04_4_64);
        assertEquals("g04s", g04s.atributos().get("base"));
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(g04, g04s));

        // "Pro+" no es "Pro": el + pegado es parte del modelo.
        Resolucion pro = tel("Xiaomi", "Redmi Note 15 Pro 8/256GB", null);
        Resolucion proPlus = tel("Xiaomi", "Redmi Note 15 Pro+ 8/256GB", null);
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(pro, proPlus));
    }

    @Test
    void conectividad4GNoSeConfundeConRam4GB() {
        // "4G" solo: es conectividad, así que sin RAM informada queda en revisión (no se inventa ram=4).
        Resolucion soloRed = tel("Motorola", "G04 4G 64GB", Map.of("almacenamiento", "64GB"));
        assertEquals(Estado.REVISION, soloRed.estado());
        assertEquals("4g", soloRed.atributos().get("conectividad"));
        assertNull(soloRed.atributos().get("ram_gb"));

        // "4GB" sin "4G": es RAM, y la conectividad queda sin informar.
        Resolucion soloRam = tel("Motorola", "G04 4GB 64GB", null);
        assertEquals(Estado.RESUELTA, soloRam.estado(), soloRam.motivos().toString());
        assertEquals("4", soloRam.atributos().get("ram_gb"));
        assertNull(soloRam.atributos().get("conectividad"));

        // Mismo teléfono con y sin conectividad declarada: no es IGUAL, pero tampoco se afirma
        // que sea otro. Un dato desconocido no es un comodín → conflicto (revisión).
        Resolucion conRed = tel("Motorola", "G04 4G 64GB", G04_4_64);
        assertEquals(Compatibilidad.CONFLICTO, svc.comparar(conRed, soloRam));
    }

    @Test
    void laRamVirtualNoEsRamFisica() {
        Resolucion a = tel("Xiaomi", "Redmi 15C 8GB+8GB RAM 256GB", null);
        assertEquals("8", a.atributos().get("ram_gb"));
        assertEquals("8", a.atributos().get("ram_virtual_gb"));
        assertEquals("256", a.atributos().get("almacenamiento_gb"));

        Resolucion b = tel("Xiaomi", "Redmi 15C 256GB", Map.of("ram", "8GB + 8GB virtual"));
        assertEquals(a.clave(), b.clave());

        Resolucion soloVirtual = tel("Xiaomi", "Redmi 15C 256GB", Map.of("otros", "RAM virtual 8GB"));
        assertEquals(Estado.REVISION, soloVirtual.estado());
        assertTrue(soloVirtual.motivos().get(0).contains("RAM virtual"));
    }

    @Test
    void almacenamientoMasRamNoSeLeeComoRamVirtual() {
        Resolucion r = tel("Motorola", "G04 64GB + 4GB RAM", null);
        assertEquals("4", r.atributos().get("ram_gb"));
        assertEquals("64", r.atributos().get("almacenamiento_gb"));
        assertNull(r.atributos().get("ram_virtual_gb"));
    }

    @Test
    void datosContradictoriosQuedanParaRevision() {
        Resolucion alm = tel("Motorola", "G04 4G 64GB", Map.of("almacenamiento", "128GB", "ram", "4GB"));
        assertEquals(Estado.REVISION, alm.estado());
        assertNull(alm.clave());
        assertTrue(alm.contradictoria());
        assertTrue(alm.motivos().get(0).contains("almacenamiento"));

        Resolucion ram = tel("Motorola", "G04 4G 64GB / 4GB RAM", Map.of("ram", "8GB"));
        assertEquals(Estado.REVISION, ram.estado());

        Resolucion red = tel("Motorola", "G04 4G 64GB", Map.of("ram", "4GB", "conectividad", "5G"));
        assertEquals(Estado.REVISION, red.estado());
    }

    @Test
    void datosIncompletosQuedanParaRevision() {
        assertEquals(Estado.REVISION, tel("Samsung", "Galaxy A16 128GB", null).estado());      // falta RAM
        assertEquals(Estado.REVISION, tel("Motorola", "G04", Map.of("ram", "4GB")).estado()); // falta almacenamiento
        Resolucion ambigua = tel("Motorola", "G04 16GB", null);                                 // ¿RAM o almacenamiento?
        assertEquals(Estado.REVISION, ambigua.estado());
    }

    @Test
    void lasEspecificacionesCompletanLoQueElNombreNoDice() {
        Resolucion r = tel("Samsung", "Galaxy A16", Map.of("almacenamiento", "128 GB", "ram", "4 GB",
                "color", "Negro", "otros", "Dual SIM, 5G"));
        assertEquals(Estado.RESUELTA, r.estado(), r.motivos().toString());
        assertEquals("a16", r.atributos().get("base"));
        assertEquals("5g", r.atributos().get("conectividad"));
        assertEquals("dualsim", r.atributos().get("sim"));
        assertEquals("negro", r.atributos().get("color"));
        assertEquals("especificaciones", r.atributos().get("fuente.ram_gb"));
    }

    @Test
    void conservaSimColorCondicionYCombo() {
        Resolucion base = tel("Apple", "iPhone 16 128GB", null);
        assertEquals(Estado.RESUELTA, base.estado(), base.motivos().toString());  // el iPhone no exige RAM
        Resolucion esim = tel("Apple", "iPhone 16 128GB eSIM", null);
        assertEquals("esim", esim.atributos().get("sim"));
        assertEquals(Compatibilidad.CONFLICTO, svc.comparar(base, esim));  // uno no dice la SIM
        Resolucion sim = tel("Apple", "iPhone 16 128GB SIM física", null);
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(esim, sim));

        Resolucion negro = tel("Motorola", "G04 4G 64GB Negro", G04_4_64);
        Resolucion black = tel("Motorola", "Moto G04 4G 64GB Black", G04_4_64);
        Resolucion azul = tel("Motorola", "G04 4G 64GB Azul", G04_4_64);
        assertEquals(negro.clave(), black.clave());
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(negro, azul));

        Resolucion nuevo = tel("Motorola", "G04 4G 64GB", G04_4_64);
        Resolucion reac = tel("Motorola", "G04 4G 64GB Reacondicionado", G04_4_64);
        assertEquals("nuevo", nuevo.atributos().get("condicion"));
        assertEquals(Compatibilidad.DISTINTA, svc.comparar(nuevo, reac));

        Resolucion combo = tel("Motorola", "G04 4G 64GB + Funda", G04_4_64);
        assertEquals("funda", combo.atributos().get("combo"));
        assertEquals("g04", combo.atributos().get("base"));
        assertNotEquals(nuevo.clave(), combo.clave());
    }

    @Test
    void laListaDeRedesYDeColoresDisponiblesNoContradiceAlNombre() {
        // Casos del catálogo real: la ficha lista las redes soportadas y los colores en stock.
        Resolucion r = tel("Samsung", "Galaxy A37 5G 8/256 GB", Map.of("otros", "Redes 5G, 4G LTE, 3G",
                "colores", "Azul, Blanco, Negro"));
        assertEquals(Estado.RESUELTA, r.estado(), r.motivos().toString());
        assertEquals("5g", r.atributos().get("conectividad"));
        assertNull(r.atributos().get("color"));
        assertFalse(svc.resolver("Samsung", "S90D 65\" OLED", null, null).esTelefono());
    }

    @Test
    void redmiYPocoSonXiaomi() {
        Resolucion a = tel("Redmi", "Note 14 8/256GB", null);
        Resolucion b = tel("Xiaomi", "Redmi Note 14 8GB 256GB", null);
        assertEquals(a.clave(), b.clave());
    }

    @Test
    void loQueNoEsTelefonoConservaLaClaveSuelta() {
        Resolucion jbl = svc.resolver("JBL", "Flip 6 Black", null, null);
        assertEquals(IdentidadProductoService.VERSION_GENERICA, jbl.version());
        assertEquals("gen1|" + ImagenManualService.clave("JBL", "Flip 6 Black"), jbl.clave());

        // Samsung y Motorola venden más que teléfonos, y Xiaomi también monopatines.
        assertFalse(svc.resolver("Samsung", "Galaxy Tab S9 128GB", null, null).esTelefono());
        assertFalse(svc.resolver("Motorola", "Moto Buds 600", null, null).esTelefono());
        assertFalse(svc.resolver("Xiaomi", "Electric Scooter 4 Pro", null, null).esTelefono());
        assertFalse(svc.resolver("Xiaomi", "Mi Robot Vacuum S10", null, null).esTelefono());
        // La categoría alcanza para reconocer un teléfono de una marca no listada.
        assertTrue(svc.resolver("Blackview", "A55 4/64GB", null, "Celulares").esTelefono());
    }

    @Test
    void unProductoGuardadoSeResuelveDesdeSuTextoDeEspecificaciones() {
        Resolucion guardado = svc.resolverGuardado("Motorola", "G04 4G 64GB / 4GB RAM",
                "Octa-core · 4GB · 64GB · 6.56\" · Android 14", null);
        Resolucion entrante = tel("Motorola", "Motorola G04 4G 64GB", G04_4_64);
        assertEquals(Compatibilidad.IGUAL, svc.comparar(guardado, entrante));
    }

    @Test
    void esDeterministaYVersionada() {
        Resolucion a = tel("Motorola", "G04 4G 64GB", G04_4_64);
        Resolucion b = tel("Motorola", "G04 4G 64GB", G04_4_64);
        assertEquals(a, b);
        assertEquals(IdentidadProductoService.VERSION_TELEFONO, a.version());
    }

    @Test
    void elColorCanonicoSeMuestraEnCastellano() {
        assertEquals("Verde", IdentidadProductoService.nombreVisibleColor("verde"));
        assertEquals("Titanio Natural", IdentidadProductoService.nombreVisibleColor("titanio-natural"));
        assertEquals("Medianoche Negro", IdentidadProductoService.nombreVisibleColor("medianoche-negro"));
        assertNull(IdentidadProductoService.nombreVisibleColor("azulglaciar"));   // texto crudo: no se agrega
        assertNull(IdentidadProductoService.nombreVisibleColor(null));
    }

    /** Lo que muestra Admin → Imágenes: el color del artículo con el vocabulario de la identidad. */
    @Test void coloresVisiblesReconoceUnoOVariosYNoInventa() {
        assertEquals(java.util.List.of("Celeste"), IdentidadProductoService.coloresVisibles("Galaxy S26 Ultra 12/256GB · Sky Blue"));
        assertEquals(3, IdentidadProductoService.coloresVisibles("12GB · 256GB · Color Black / Blue / White").size());
        assertTrue(IdentidadProductoService.coloresVisibles("MacBook Air 13 16GB 512GB SSD").isEmpty());
    }
}
