package com.futuratecno.application;

import com.futuratecno.api.dto.CatalogoPaginaDTO;
import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.futuratecno.application.FiltrosCategoria.COLOR_A_CONSULTAR;
import static com.futuratecno.application.FiltrosCategoria.Grupo.CELULAR;
import static com.futuratecno.application.FiltrosCategoria.Grupo.IPHONE;
import static org.junit.jupiter.api.Assertions.*;

/** Filtros de iPhone y celulares: lo que se lee de los nombres reales y cómo filtra el catálogo. */
class FiltrosCategoriaTest {

    private static Map<String, String> at(FiltrosCategoria.Grupo g, String marca, String modelo) {
        return FiltrosCategoria.atributos(g, marca, modelo, null);
    }

    @Test
    void iphoneGeneracionVersionCapacidadYColor() {
        assertEquals(Map.of("generacion", "iPhone 18", "version", "Pro Max", "capacidad", "256GB", "color", "Black"),
                at(IPHONE, "Apple", "iPhone 18 Pro Max 256GB Black"));
        assertEquals("e", at(IPHONE, "Apple", "iPhone 16e 128GB White").get("version"));
        assertEquals("Estándar", at(IPHONE, "Apple", "iPhone 17 256GB Sage").get("version"));
        assertEquals("1TB", at(IPHONE, "Apple", "iPhone 18 Pro Max 1TB Black").get("capacidad"));
        // Un modelo viejo que agrupaba colores, o sin capacidad: sin esos datos, pero con modelo.
        var viejo = at(IPHONE, "Apple", "iPhone 17 Pro");
        assertEquals("iPhone 17", viejo.get("generacion"));
        assertNull(viejo.get("capacidad"));
        assertEquals(COLOR_A_CONSULTAR, viejo.get("color"));
    }

    @Test
    void colorComoLoNombraElFabricante() {
        assertEquals("Glacier", at(CELULAR, "Motorola", "Edge 60 256GB Glacier Glacier").get("color"));
        assertEquals("Lily Pad", at(CELULAR, "Motorola", "Edge 70 5G 8GB 256GB Lily Pad").get("color"));
        assertEquals("Teal", at(IPHONE, "Apple", "iPhone 16 128GB Activado Teal teal").get("color"));
        assertEquals("Desert", at(IPHONE, "Apple", "iPhone 16 Pro 256GB Esim Desert (ACR) Desert").get("color"));
        assertEquals("Black", at(CELULAR, "Samsung", "Galaxy S25 FE 8+512 5G Negro").get("color"));   // castellano → inglés
        assertEquals(COLOR_A_CONSULTAR, at(CELULAR, "Motorola", "Moto G04").get("color"));
        assertEquals("Glacier", at(IPHONE, "Apple", "iPhone 18 Pro 256GB Glaciar Glaciar").get("color"));
        var s26 = at(CELULAR, "Samsung", "Galaxy S26 FE 8/256 Blueberry blueberry");
        assertEquals("Blueberry", s26.get("color"));
        assertEquals("256GB", s26.get("capacidad"));
        assertEquals("8GB", s26.get("ram"));
        assertEquals(COLOR_A_CONSULTAR, at(IPHONE, "Apple", "iPhone 17 256GB - (Blue - Black - Sage)").get("color"));
    }

    @Test
    void celularCapacidadRamYRed() {
        var s26 = at(CELULAR, "Samsung", "S26 Ultra 12+512 5G White");
        assertEquals("512GB", s26.get("capacidad"));
        assertEquals("12GB", s26.get("ram"));
        assertEquals("5G", s26.get("red"));
        var redmi = at(CELULAR, "Xiaomi", "Redmi 15C 4G 128GB / 4GB RAM Green");
        assertEquals("128GB", redmi.get("capacidad"));
        assertEquals("4GB", redmi.get("ram"));
        assertEquals("4G", redmi.get("red"));
        assertEquals("Green", redmi.get("color"));
    }

    @Test
    void monitorPulgadasHzResolucionYPantalla() {
        var lg = at(FiltrosCategoria.Grupo.MONITOR, "LG", "MONITOR LG 45 ULTRAGEAR 45GR75DC CURVO ULTRAWIDE DQHD 200 Hz (II) (1755)");
        assertEquals(Map.of("pulgadas", "45\"", "hz", "200 Hz", "resolucion", "Dual QHD", "pantalla", "Curva"), lg);
        var teros = at(FiltrosCategoria.Grupo.MONITOR, "TEROS", "Monitor Teros TE-2415S Gaming 23.8” Plano IPS FHD (1920 x 1080) 120Hz 1ms");
        assertEquals("23.8\"", teros.get("pulgadas"));
        assertEquals("Full HD", teros.get("resolucion"));
        // Sin la palabra "curvo" es plano (acordado); el código de modelo "27GS60F" no son pulgadas.
        var sinComillas = at(FiltrosCategoria.Grupo.MONITOR, "LG", "MONITOR LG 27 ULTRAGEAR 27GS60F FULL HD 180 Hz");
        assertEquals("27\"", sinComillas.get("pulgadas"));
        assertEquals("Plana", sinComillas.get("pantalla"));
    }

    @Test
    void placaSerieChipYMemoria() {
        var asus = at(FiltrosCategoria.Grupo.PLACA_DE_VIDEO, "ASUS", "Placa de Video ASUS AMD Radeon PRIME RX9070 GRE O12G EVO");
        assertEquals(Map.of("serie", "RX 9000", "chip", "RX 9070 GRE", "vram", "12GB"), asus);
        var msi = at(FiltrosCategoria.Grupo.PLACA_DE_VIDEO, "MSI", "VGA MSI GeForce RTX 5060 Ti 16G VENTUS 2X OC");
        assertEquals("RTX 50", msi.get("serie"));
        assertEquals("RTX 5060 Ti", msi.get("chip"));
        assertEquals("16GB", msi.get("vram"));
        assertEquals("GT 710", at(FiltrosCategoria.Grupo.PLACA_DE_VIDEO, "MSI", "VGA MSI GeForce GT 710 2G LP DDR3").get("chip"));
        assertEquals("Profesional", at(FiltrosCategoria.Grupo.PLACA_DE_VIDEO, "PNY",
                "VGA PNY QUADRO RTX PRO 4000 Blackwell 24Gb GDDR7").get("serie"));
    }

    @Test
    void memoriaTipoFormatoCapacidadModulosVelocidadYRgb() {
        var kit = at(FiltrosCategoria.Grupo.MEMORIA, "CORSAIR", "Memoria Ram UDIMM CORSAIR VENGEANCE 32GB DDR5 6000MHz CL30 1.35V (2x16GB) RGB");
        assertEquals(Map.of("ddr", "DDR5", "formato", "PC", "capacidad", "32GB", "modulos", "Kit de 2",
                "mhz", "6000 MHz", "rgb", "Con RGB"), kit);
        var sodimm = at(FiltrosCategoria.Grupo.MEMORIA, "Kingston", "Memoria SODIMM DDR4 Kingston 32Gb 3200 MHz (0924)");
        assertEquals("Notebook (SODIMM)", sodimm.get("formato"));
        assertEquals("32GB", sodimm.get("capacidad"));
        assertEquals("1 módulo", sodimm.get("modulos"));
        // "16Gbit" es la densidad del chip, no la capacidad; DDR3L es DDR3.
        assertEquals("8GB", at(FiltrosCategoria.Grupo.MEMORIA, "Kingston", "Memoria DDR4 Kingston 8Gb 3200 MHz 16Gbit (1266)").get("capacidad"));
        assertEquals("DDR3", at(FiltrosCategoria.Grupo.MEMORIA, "ADATA", "Memoria Ram UDIMM ADATA 8GB DDR3L 1600MHz CL11").get("ddr"));
        // La palabra "kit" en la ficha no borra la capacidad (fichas reales de Elit y de Hiksemi).
        var elit = FiltrosCategoria.atributos(FiltrosCategoria.Grupo.MEMORIA, "LEXAR", "Memoria LEXAR UDIMM DDR4 8GB 3200MHz",
                "Factor de forma: UDIMM. Tipo de memoria RAM: DDR4. Tamaño de memoria RAM: 8GB. Unidades x kit: 1. Iluminación: No.");
        assertEquals("8GB", elit.get("capacidad"));
        assertEquals("1 módulo", elit.get("modulos"));
        assertEquals("Sin RGB", elit.get("rgb"));
        var hiksemi = FiltrosCategoria.atributos(FiltrosCategoria.Grupo.MEMORIA, "Hiksemi", "Memoria DDR4 HIKSEMI 8Gb 3200 MHz Future RGB",
                "Potente RGB · RAM individual y en kit disponibles para elegir");
        assertEquals("8GB", hiksemi.get("capacidad"));
        assertEquals("1 módulo", hiksemi.get("modulos"));
        assertEquals("Kit de 2", FiltrosCategoria.atributos(FiltrosCategoria.Grupo.MEMORIA, "X", "Memoria DDR5 32GB 6000MHz",
                "Unidades x kit: 2. Iluminación: Sí").get("modulos"));
    }

    @Test
    void grupoPorRutaDeCategoria() {
        assertEquals(FiltrosCategoria.Grupo.MONITOR, FiltrosCategoria.grupoDe("Monitores"));
        assertEquals(FiltrosCategoria.Grupo.MONITOR, FiltrosCategoria.grupoDe("Monitores > Monitor Gamer"));
        assertNull(FiltrosCategoria.grupoDe("Apple > Monitores"));   // los de Apple van en su árbol, sin estos filtros
        assertEquals(FiltrosCategoria.Grupo.PLACA_DE_VIDEO, FiltrosCategoria.grupoDe("Placas de video > Línea NVIDIA GEFORCE"));
        assertEquals(FiltrosCategoria.Grupo.MEMORIA, FiltrosCategoria.grupoDe("Memorias RAM > Memoria Sodimm"));
    }

    // ------------------------------------------------------------------ catálogo

    private static ProductoCatalogoDTO iphone(long id, String modelo) {
        var dto = new ProductoCatalogoDTO(id, "iPhone", "Apple", modelo, null,
                List.of(new VarianteCatalogoDTO(id, "", new BigDecimal("1000"), null)));
        dto.setCategoriaId(95L);
        dto.setFiltros(FiltrosCategoria.atributos(IPHONE, "Apple", modelo, null));
        return dto;
    }

    private final List<ProductoCatalogoDTO> catalogo = List.of(
            iphone(1, "iPhone 18 Pro 256GB Black"), iphone(2, "iPhone 18 Pro 512GB Black"),
            iphone(3, "iPhone 17 Pro 256GB Silver"), iphone(4, "iPhone 17 Pro"));

    private static CatalogoPaginaDTO buscar(List<ProductoCatalogoDTO> cat, Map<String, List<String>> f) {
        return BusquedaCatalogo.buscar(cat, new BusquedaCatalogo.Filtro(null, null, null, null, null,
                "relevancia", 1, 24, IPHONE, f));
    }

    private static Map<String, Integer> opciones(CatalogoPaginaDTO r, String clave) {
        var m = new java.util.LinkedHashMap<String, Integer>();
        r.getFiltros().stream().filter(x -> x.clave().equals(clave)).findFirst().orElseThrow()
                .opciones().forEach(o -> m.put(o.valor(), o.cantidad()));
        return m;
    }

    @Test
    void sinFiltroAparecenTodosYLasOpcionesTraenCantidad() {
        var r = buscar(catalogo, Map.of());
        assertEquals(4, r.getTotal());
        assertEquals(Map.of("iPhone 18", 2, "iPhone 17", 2), opciones(r, "generacion"));
        assertEquals(List.of("iPhone 18", "iPhone 17"), List.copyOf(opciones(r, "generacion").keySet()));   // lo nuevo primero
        assertEquals(List.of("256GB", "512GB"), List.copyOf(opciones(r, "capacidad").keySet()));
    }

    @Test
    void dentroDeUnFiltroEsOEntreFiltrosEsY() {
        var r = buscar(catalogo, Map.of("capacidad", List.of("256GB", "512GB"), "color", List.of("Black")));
        assertEquals(List.of(1L, 2L), r.getItems().stream().map(ProductoCatalogoDTO::getId).toList());
        // El producto sin capacidad no aparece al filtrar por capacidad…
        assertTrue(buscar(catalogo, Map.of("capacidad", List.of("256GB"))).getItems().stream().noneMatch(p -> p.getId() == 4L));
        // …pero la cantidad de cada color se cuenta con los OTROS filtros, no con el propio.
        assertEquals(Map.of("Black", 2, "Silver", 1), opciones(buscar(catalogo, Map.of(
                "capacidad", List.of("256GB", "512GB"), "color", List.of("Black"))), "color"));
    }

    @Test
    void colorAConsultarEsUnaOpcionYSeVaAlFinal() {
        var r = buscar(catalogo, Map.of());
        assertEquals(COLOR_A_CONSULTAR, List.copyOf(opciones(r, "color").keySet()).getLast());
        var soloConsultar = buscar(catalogo, Map.of("color", List.of(COLOR_A_CONSULTAR)));
        assertEquals(List.of(4L), soloConsultar.getItems().stream().map(ProductoCatalogoDTO::getId).toList());
    }

    @Test
    void sinGrupoNoHayFiltros() {
        var r = BusquedaCatalogo.buscar(catalogo, new BusquedaCatalogo.Filtro(null, null, null, null, null, null, 1, 24));
        assertTrue(r.getFiltros().isEmpty());
    }
}
