package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.api.dto.ProductoEditDTO;
import com.futuratecno.api.dto.VarianteEditDTO;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Carga por JSON con identidad (V42) contra un PostgreSQL de verdad: el índice único parcial, el
 * bloqueo por familia y la concurrencia no se pueden probar con mocks.
 *
 * <p>Usa una base DESCARTABLE que se limpia y se vuelve a migrar (V1–V42) en cada corrida. No
 * forma parte de la suite normal. Para correrlo, con el Postgres de docker-compose levantado:
 * <pre>
 * docker exec futuratecno-postgres psql -U futuratecno -d postgres -c "CREATE DATABASE futuratecno_it"
 * mvn -Dnet.bytebuddy.experimental=true test -Dtest=CargaJsonIdentidadPostgresTest -Dpg.it=true
 * </pre>
 */
@EnabledIfSystemProperty(named = "pg.it", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/futuratecno_it",
        "spring.datasource.username=futuratecno",
        "spring.datasource.password=futuratecno",
        "spring.flyway.clean-disabled=false",
        // Nada sale a la red: sin claves de IA (la clasificación de categoría devuelve null en vez
        // de gastar crédito) y sin sincronización.
        "anthropic.api-key=", "openai.api-key=", "deepseek.api-key=",
        "sync.enabled=false"
})
class CargaJsonIdentidadPostgresTest {

    @TestConfiguration
    static class BaseLimpia {
        /** Limpia y migra SOLO la base de pruebas; se niega a tocar cualquier otra. */
        @Bean
        FlywayMigrationStrategy limpiarYMigrar() {
            return (Flyway flyway) -> {
                String url;
                try (var c = flyway.getConfiguration().getDataSource().getConnection()) {
                    url = c.getMetaData().getURL();
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                }
                if (url == null || !url.endsWith("/futuratecno_it")) {
                    throw new IllegalStateException("Este test solo limpia futuratecno_it, no " + url);
                }
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    @Autowired CargaJsonService carga;
    @Autowired IdentidadTransicionService transicion;
    @Autowired ProductoRepository productos;
    @Autowired VarianteRepository variantes;
    @Autowired ProveedorRepository proveedores;
    @Autowired DescripcionManualService descripciones;
    @Autowired ImagenManualService imagenes;
    @Autowired AtributosManualService atributos;
    @Autowired MargenManualService margenes;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductoAdminService admin;
    @Autowired org.springframework.transaction.support.TransactionTemplate tx;

    private Long prov;

    @BeforeEach
    void proveedorNuevo() {
        prov = nuevoProveedor();
    }

    private Long nuevoProveedor() {
        Proveedor p = new Proveedor();
        p.setNombre("IT " + System.nanoTime());
        p.setCodigo("T" + (System.nanoTime() % 100000));
        p.setMargenPorcentaje(new BigDecimal("15"));
        p.setFletePorcentaje(new BigDecimal("5"));
        p.setActivo(true);
        return proveedores.save(p).getId();
    }

    private static ArticuloJsonDTO art(String marca, String modelo, String precio, Map<String, Object> esp) {
        ArticuloJsonDTO a = new ArticuloJsonDTO();
        a.setMarca(marca);
        a.setModelo(modelo);
        a.setPrecioUsd(new BigDecimal(precio));
        a.setEspecificaciones(esp);
        return a;
    }

    private static Map<String, Object> esp(String almacenamiento, String ram) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (almacenamiento != null) m.put("almacenamiento", almacenamiento);
        if (ram != null) m.put("ram", ram);
        return m;
    }

    private CargaJsonResponse.Item cargarUno(Long proveedorId, ArticuloJsonDTO a) {
        CargaJsonResponse r = carga.cargar(proveedorId, List.of(a));
        assertEquals(1, r.getItems().size());
        return r.getItems().get(0);
    }

    private BigDecimal costo(Long productoId) {
        return variantes.findByProductoIdAndActivo(productoId, true).get(0).getCostoUsd();
    }

    private int productosDelProveedor(Long proveedorId) {
        return jdbc.queryForObject("SELECT count(*) FROM productos WHERE proveedor_id = ?", Integer.class, proveedorId);
    }

    // ------------------------------------------------------------------ convergencia y separación

    @Test
    void losDosEjemplosDeMotorolaActualizanElMismoProductoSinCambiarleElNombre() {
        CargaJsonResponse.Item a = cargarUno(prov, art("Motorola", "G04 4G 64GB / 4GB RAM", "100", esp("64GB", "4GB")));
        CargaJsonResponse.Item b = cargarUno(prov, art("Motorola", "Motorola G04 4G 64GB", "110", esp("64GB", "4GB")));

        assertEquals("creado", a.getEstado());
        assertEquals("actualizado", b.getEstado());
        assertEquals(a.getProductoId(), b.getProductoId());
        assertEquals(1, productosDelProveedor(prov));
        assertEquals(0, new BigDecimal("110.00").compareTo(costo(a.getProductoId())));
        Producto p = productos.findById(a.getProductoId()).orElseThrow();
        assertEquals("G04 4G 64GB / 4GB RAM", p.getModelo());       // el nombre visible no se pisa
        assertNotNull(p.getIdentidadClave());
        assertEquals(IdentidadProductoService.VERSION_TELEFONO, p.getIdentidadVersion());
        assertTrue(p.getIdentidadAtributos().contains("\"ram_gb\":\"4\""));
        // Proyección en la variante, con los mismos valores que la identidad.
        Variante v = variantes.findByProductoIdAndActivo(p.getId(), true).get(0);
        assertEquals(4, v.getRamGb());
        assertEquals(64, v.getAlmacenamientoGb());
    }

    @Test
    void variantesDistintasQuedanEnProductosDistintos() {
        Long base = cargarUno(prov, art("Motorola", "G04 4G 64GB", "100", esp("64GB", "4GB"))).getProductoId();
        CargaJsonResponse.Item alm = cargarUno(prov, art("Motorola", "G04 4G 128GB", "130", esp("128GB", "4GB")));
        CargaJsonResponse.Item ram = cargarUno(prov, art("Motorola", "G04 4G 64GB", "120", esp("64GB", "8GB")));
        CargaJsonResponse.Item sufijo = cargarUno(prov, art("Motorola", "G04s 4G 64GB", "105", esp("64GB", "4GB")));

        for (CargaJsonResponse.Item i : List.of(alm, ram, sufijo)) {
            assertEquals("creado", i.getEstado(), i.getProducto());
            assertNotEquals(base, i.getProductoId());
        }
        assertEquals(4, productosDelProveedor(prov));
        assertEquals(0, new BigDecimal("100.00").compareTo(costo(base)));   // nadie pisó el precio del 4/64
    }

    /** El JSON de la captura del 24/9: marca repetida en el modelo y el color solo en modelo_exacto. */
    @Test
    void elJsonDeLaCapturaCreaUnNombreLimpioConColorYDespuesActualiza() {
        ArticuloJsonDTO a = art("Motorola", "Motorola G04 4G 64GB", "106",
                new LinkedHashMap<>(Map.of("ram", "4GB RAM", "almacenamiento", "64GB", "otros", "4G")));
        a.setModeloExacto("Motorola G04 4G 64GB / 4GB RAM (Green)");
        CargaJsonResponse.Item creado = cargarUno(prov, a);
        assertEquals("creado", creado.getEstado());
        assertEquals("G04 4G 64GB Verde", productos.findById(creado.getProductoId()).orElseThrow().getModelo());

        // Recargarlo, o cargarlo con el color ya en el nombre, actualiza el mismo producto.
        assertEquals(creado.getProductoId(), cargarUno(prov, a).getProductoId());
        assertEquals(creado.getProductoId(), cargarUno(prov,
                art("Motorola", "G04 4G 64GB Verde", "107", esp("64GB", "4GB"))).getProductoId());
        // Otro color es otro producto.
        CargaJsonResponse.Item negro = cargarUno(prov, art("Motorola", "Motorola G04 4G 64GB Negro", "106", esp("64GB", "4GB")));
        assertEquals("creado", negro.getEstado());
        assertEquals("G04 4G 64GB Negro", productos.findById(negro.getProductoId()).orElseThrow().getModelo());
    }

    // ------------------------------------------------------------------ revisión sin sobrescritura

    @Test
    void datosContradictoriosOIncompletosQuedanEnRevisionSinTocarNada() {
        Long id = cargarUno(prov, art("Motorola", "G04 4G 64GB", "100", esp("64GB", "4GB"))).getProductoId();

        CargaJsonResponse r = carga.cargar(prov, List.of(
                art("Motorola", "G04 4G 64GB", "50", esp("128GB", "4GB")),   // contradicción
                art("Motorola", "G04 4G 64GB", "50", esp("64GB", null))));    // falta RAM
        assertEquals(2, r.getRevision());
        assertEquals(0, r.getCreados() + r.getActualizados());
        r.getItems().forEach(i -> {
            assertEquals("revision", i.getEstado());
            assertNotNull(i.getMotivo());
            assertNull(i.getProductoId());
        });
        assertEquals(1, productosDelProveedor(prov));
        assertEquals(0, new BigDecimal("100.00").compareTo(costo(id)));
    }

    @Test
    void unCandidatoQueNoSePuedeCompararVaARevision() {
        // Producto viejo (anterior a la V42), sin RAM en ningún lado: ¿es el 4GB o el 8GB?
        Long viejo = productoViejo(prov, "Motorola", "G04 4G 64GB", "Octa-core · 64GB", "90");
        CargaJsonResponse.Item i = cargarUno(prov, art("Motorola", "Motorola G04 4G 64GB", "100", esp("64GB", "4GB")));
        assertEquals("revision", i.getEstado());
        assertEquals(List.of(viejo), i.getCandidatos());
        assertEquals(0, new BigDecimal("90.00").compareTo(costo(viejo)));
        assertEquals(1, productosDelProveedor(prov));
    }

    @Test
    void unProductoViejoQueCoincideSinAmbiguedadSeAdoptaYConservaSuId() {
        Long viejo = productoViejo(prov, "Motorola", "G04 4G 64GB / 4GB RAM", "Octa-core · 4GB · 64GB", "90");
        CargaJsonResponse.Item i = cargarUno(prov, art("Motorola", "Motorola G04 4G 64GB", "100", esp("64GB", "4GB")));
        assertEquals("actualizado", i.getEstado());
        assertEquals(viejo, i.getProductoId());
        assertNotNull(productos.findById(viejo).orElseThrow().getIdentidadClave());
    }

    @Test
    void elMismoArticuloDosVecesEnUnaCargaNoSeActualizaDosVeces() {
        CargaJsonResponse r = carga.cargar(prov, List.of(
                art("Motorola", "G04 4G 64GB / 4GB RAM", "100", esp("64GB", "4GB")),
                art("Motorola", "Moto G04 4G 64GB", "95", esp("64GB", "4GB"))));
        assertEquals(1, r.getCreados());
        assertEquals(1, r.getRevision());
        assertEquals(0, new BigDecimal("100.00").compareTo(costo(r.getItems().get(0).getProductoId())));
    }

    // ------------------------------------------------------------------ recarga y reactivación

    @Test
    void recargarYReactivarConservanElId() {
        Long id = cargarUno(prov, art("Motorola", "G04 4G 64GB", "100", esp("64GB", "4GB"))).getProductoId();
        assertEquals(id, cargarUno(prov, art("Motorola", "G04 4G 64GB", "101", esp("64GB", "4GB"))).getProductoId());

        jdbc.update("UPDATE productos SET activo = false WHERE id = ?", id);
        CargaJsonResponse.Item r = cargarUno(prov, art("Motorola", "Moto G04 4G 64GB", "102", esp("64GB", "4GB")));
        assertEquals("actualizado", r.getEstado());
        assertEquals(id, r.getProductoId());
        assertTrue(productos.findById(id).orElseThrow().getActivo());
        assertEquals(1, productosDelProveedor(prov));
    }

    // ------------------------------------------------------------------ concurrencia y unicidad

    @Test
    void dosCargasSimultaneasNoCreanDuplicados() throws Exception {
        for (int ronda = 0; ronda < 5; ronda++) {
            Long p = nuevoProveedor();
            CountDownLatch largada = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            List<Future<CargaJsonResponse>> fs = new ArrayList<>();
            fs.add(pool.submit(() -> { largada.await(); return carga.cargar(p, List.of(art("Motorola", "G04 4G 64GB / 4GB RAM", "100", esp("64GB", "4GB")))); }));
            fs.add(pool.submit(() -> { largada.await(); return carga.cargar(p, List.of(art("Motorola", "Motorola G04 4G 64GB", "110", esp("64GB", "4GB")))); }));
            largada.countDown();
            List<String> estados = new ArrayList<>();
            for (Future<CargaJsonResponse> f : fs) estados.add(f.get(60, TimeUnit.SECONDS).getItems().get(0).getEstado());
            pool.shutdown();

            assertEquals(1, productosDelProveedor(p), "ronda " + ronda);
            assertTrue(estados.contains("creado") && estados.contains("actualizado"), estados.toString());
        }
    }

    @Test
    void laBaseRechazaDosProductosConLaMismaIdentidadAunqueUnoEsteInactivo() {
        Long id = cargarUno(prov, art("Motorola", "G04 4G 64GB", "100", esp("64GB", "4GB"))).getProductoId();
        jdbc.update("UPDATE productos SET activo = false WHERE id = ?", id);
        String clave = productos.findById(id).orElseThrow().getIdentidadClave();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO productos (proveedor_id, marca, modelo, activo, created_at, updated_at, identidad_clave)
                VALUES (?, 'Motorola', 'otra redacción', true, now(), now(), ?)
                """, prov, clave));
        // En otro proveedor la misma identidad es válida: la unicidad es por proveedor.
        Long otro = nuevoProveedor();
        jdbc.update("""
                INSERT INTO productos (proveedor_id, marca, modelo, activo, created_at, updated_at, identidad_clave)
                VALUES (?, 'Motorola', 'G04', true, now(), now(), ?)
                """, otro, clave);
    }

    // ------------------------------------------------------------------ datos manuales

    @Test
    void losDatosManualesSobrevivenAOtraRedaccionYAOtroProveedor() {
        Long id = cargarUno(prov, art("Motorola", "G04 4G 64GB / 4GB RAM", "100", esp("64GB", "4GB"))).getProductoId();
        Producto p = productos.findById(id).orElseThrow();
        // Lo que el admin carga a mano queda recordado con el nombre del producto.
        descripciones.guardar(p.getMarca(), p.getModelo(), "Descripción corregida a mano");
        imagenes.guardar(p.getMarca(), p.getModelo(), "https://img.test/g04-manual.jpg");
        p.setPesoGramos(321);
        p.setMargenPorcentaje(new BigDecimal("30"));
        productos.save(p);
        atributos.recordar(p);
        margenes.recordar(p);

        // Misma identidad, otra redacción, mismo proveedor.
        ArticuloJsonDTO otra = art("Motorola", "Motorola G04 4G 64GB", "105", esp("64GB", "4GB"));
        otra.setImagenes(List.of("https://img.test/no-deberia-usarse.jpg"));
        assertEquals(id, cargarUno(prov, otra).getProductoId());
        Producto actualizado = productos.findById(id).orElseThrow();
        assertEquals("https://img.test/g04-manual.jpg", actualizado.getImagenUrl());
        assertEquals("Descripción corregida a mano", variantes.findByProductoIdAndActivo(id, true).get(0).getEspecificaciones());
        assertEquals(0, new BigDecimal("30").compareTo(actualizado.getMargenPorcentaje()));

        // El mismo artículo en otro proveedor, con otra redacción: hereda foto, descripción y
        // peso; el margen NO, porque es de lo que cobra cada proveedor.
        Long otroProv = nuevoProveedor();
        Long otroId = cargarUno(otroProv, art("Motorola", "Moto G04 4G 64GB", "99", esp("64GB", "4GB"))).getProductoId();
        Producto enOtro = productos.findById(otroId).orElseThrow();
        assertEquals("https://img.test/g04-manual.jpg", enOtro.getImagenUrl());
        assertEquals("Descripción corregida a mano", variantes.findByProductoIdAndActivo(otroId, true).get(0).getEspecificaciones());
        assertEquals(321, enOtro.getPesoGramos());
        assertNull(enOtro.getMargenPorcentaje());
    }

    // ------------------------------------------------------------------ otras vías

    @Test
    void loQueNoEsTelefonoSigueDeduplicandoPorClaveSuelta() {
        Long id = cargarUno(prov, art("JBL", "Flip 6 Black", "80", null)).getProductoId();
        CargaJsonResponse.Item i = cargarUno(prov, art("JBL", "JBL Flip-6 (Black)", "85", null));
        assertEquals("actualizado", i.getEstado());
        assertEquals(id, i.getProductoId());
        assertTrue(productos.findById(id).orElseThrow().getIdentidadClave().startsWith("gen1|"));
    }

    @Test
    void unProductoDeMayoristaNoSeToca() {
        // Un producto de Elit (codigo_externo) en otro proveedor, mismo nombre.
        Long elit = nuevoProveedor();
        Long idElit = productoViejo(elit, "Motorola", "G04 4G 64GB", "4GB · 64GB", "70");
        jdbc.update("UPDATE productos SET codigo_externo = 'ELIT-123', fuente = 'ELIT' WHERE id = ?", idElit);

        cargarUno(prov, art("Motorola", "G04 4G 64GB", "100", esp("64GB", "4GB")));
        Producto p = productos.findById(idElit).orElseThrow();
        assertNull(p.getIdentidadClave());
        assertEquals(0, new BigDecimal("70.00").compareTo(costo(idElit)));
        assertEquals(idElit, productos.findByProveedorIdAndCodigoExterno(elit, "ELIT-123").orElseThrow().getId());
    }

    // ------------------------------------------------------------------ transición

    @Test
    void laTransicionInformaYAsignaSoloLoInequivoco() {
        Long dup1 = productoViejo(prov, "Samsung", "Galaxy A16 4/128GB", "4GB · 128GB", "150");
        Long dup2 = productoViejo(prov, "Samsung", "A16 128GB 4GB RAM", "", "155");
        Long unico = productoViejo(prov, "Samsung", "Galaxy A26 6/128GB", "6GB · 128GB", "200");
        Long ambiguo = productoViejo(prov, "Samsung", "Galaxy A36 256GB", "", "250");   // sin RAM

        IdentidadTransicionService.Reporte rep = transicion.analizar();
        assertTrue(rep.idsInequivocos().contains(unico));
        assertTrue(rep.duplicados().stream().anyMatch(d -> d.get("productos").toString().contains("id=" + dup1)
                && d.get("productos").toString().contains("id=" + dup2)));
        assertTrue(rep.ambiguos().stream().anyMatch(a -> ambiguo.equals(a.get("id"))));

        int asignados = transicion.asignarInequivocos();
        assertTrue(asignados >= 1);
        assertEquals(0, transicion.asignarInequivocos());   // idempotente
        assertNotNull(productos.findById(unico).orElseThrow().getIdentidadClave());
        for (Long id : List.of(dup1, dup2, ambiguo)) {
            Producto p = productos.findById(id).orElseThrow();
            assertNull(p.getIdentidadClave(), "no se asigna a duplicados ni ambiguos: " + id);
            assertTrue(p.getActivo(), "no se da de baja nada: " + id);
        }
        // Y la carga no elige sola entre duplicados: los manda a revisión.
        assertEquals("revision", cargarUno(prov, art("Samsung", "Galaxy A16 128GB", "160", esp("128GB", "4GB"))).getEstado());
    }

    // ------------------------------------------------------------------ edición manual en el admin

    /** Edita solo las especificaciones de la variante, sin mandar precio ni stock (quedan como están). */
    private void editarEspecificaciones(Long productoId, java.util.function.UnaryOperator<String> cambio) {
        // En producción la sesión la abre el request (open-in-view); acá hay que abrirla a mano.
        ProductoEditDTO dto = tx.execute(st -> admin.obtenerParaEditar(productoId));
        Variante v = variantes.findByProductoIdAndActivo(productoId, true).get(0);
        VarianteEditDTO ve = new VarianteEditDTO();
        ve.setId(v.getId());
        ve.setEspecificaciones(cambio.apply(v.getEspecificaciones()));
        dto.setVariantes(List.of(ve));
        admin.actualizarProducto(productoId, dto);
    }

    @Test
    void completarElColorEnElAdminAlineaLaIdentidadYLaCargaDejaDeMandarlaARevision() {
        // El caso del 8127: nació sin color y después llega el mismo teléfono en azul.
        Long id = cargarUno(prov, art("Motorola", "G06 4G 64GB", "107", esp("64GB", "4GB"))).getProductoId();
        ArticuloJsonDTO azul = art("Motorola", "G06 4G 64GB / 4GB RAM", "107", esp("64GB", "4GB"));
        azul.getEspecificaciones().put("color", "Azul");
        assertEquals("revision", cargarUno(prov, azul).getEstado());
        BigDecimal costoAntes = costo(id);

        editarEspecificaciones(id, e -> e + " · Azul");

        Producto p = productos.findById(id).orElseThrow();
        assertEquals("tel1|motorola|g06|4g|64|4|-|azul|nuevo|-|-", p.getIdentidadClave());
        assertEquals("G06 4G 64GB", p.getModelo());
        assertEquals("azul", variantes.findByProductoIdAndActivo(id, true).get(0).getColor());
        assertEquals(0, costoAntes.compareTo(costo(id)));
        CargaJsonResponse.Item otraVez = cargarUno(prov, azul);
        assertEquals("actualizado", otraVez.getEstado());
        assertEquals(id, otraVez.getProductoId());

        // Una edición que pierde un dato conocido no degrada la identidad.
        editarEspecificaciones(id, e -> e.replace(" · Azul", ""));
        assertEquals("tel1|motorola|g06|4g|64|4|-|azul|nuevo|-|-", productos.findById(id).orElseThrow().getIdentidadClave());
    }

    @Test
    void unaEdicionQueLoConvierteEnOtroProductoExistenteSeRechazaYNoGuardaNada() {
        Long verde = cargarUno(prov, art("Motorola", "G06 4G 64GB Verde", "107", esp("64GB", "4GB"))).getProductoId();
        Long viejo = productoViejo(prov, "Motorola", "G06 4G 64GB", "4GB RAM · 64GB · 4G", "107");

        var e = assertThrows(IdentidadTransicionService.IdentidadEnUsoException.class,
                () -> editarEspecificaciones(viejo, s -> s + " · Verde"));
        assertTrue(e.getMessage().contains(String.valueOf(verde)));
        assertNull(productos.findById(viejo).orElseThrow().getIdentidadClave());
        assertEquals("4GB RAM · 64GB · 4G", variantes.findByProductoIdAndActivo(viejo, true).get(0).getEspecificaciones());
    }

    /** Un producto como los que hay antes de la V42: sin identidad, con su variante. */
    private Long productoViejo(Long proveedorId, String marca, String modelo, String specs, String costo) {
        Long id = jdbc.queryForObject("""
                INSERT INTO productos (proveedor_id, marca, modelo, activo, created_at, updated_at, fuente)
                VALUES (?, ?, ?, true, now(), now(), 'JSON') RETURNING id
                """, Long.class, proveedorId, marca, modelo);
        jdbc.update("""
                INSERT INTO variantes (producto_id, especificaciones, costo_usd, stock, activo, created_at, updated_at)
                VALUES (?, ?, ?, 0, true, now(), now())
                """, id, specs, new BigDecimal(costo));
        return id;
    }

    @Test
    void reemplazoSeparaColoresSinFotosYLaRecargaEsIdempotente() {
        var agrupado = art("Apple", "iPhone 17 256GB", "975", esp("256GB", null));
        Long anterior = cargarUno(prov, agrupado).getProductoId();
        var negro = art("Apple", "iPhone 17 256GB Negro", "975", Map.of("almacenamiento", "256GB", "color", "Negro"));
        var azul = art("Apple", "iPhone 17 256GB Azul", "975", Map.of("almacenamiento", "256GB", "color", "Azul"));
        imagenes.guardar("Apple", negro.getModelo(), "https://example.com/no-copiar.jpg");
        descripciones.guardar("Apple", negro.getModelo(), "1TB · descripción anterior incompatible");
        CargaJsonResponse r = carga.cargar(prov, List.of(negro, azul), List.of(anterior));
        assertEquals(2, r.getCreados());
        assertTrue(r.getMensaje().contains("Reemplazo: dados de baja los productos anteriores [" + anterior + "]"), r.getMensaje());
        assertEquals(false, productos.findById(anterior).orElseThrow().getActivo());
        assertTrue(variantes.findByProductoIdAndActivo(anterior, true).isEmpty());
        for (var item : r.getItems()) {
            assertNull(productos.findById(item.getProductoId()).orElseThrow().getImagenUrl());
            assertTrue(variantes.findByProductoIdAndActivo(item.getProductoId(), true).get(0).getEspecificaciones().contains("256GB"));
        }
        assertEquals("revision", cargarUno(prov, agrupado).getEstado());
        assertEquals(false, productos.findById(anterior).orElseThrow().getActivo());
        // Recarga normal: el dueño de cada identidad prevalece sobre el agrupado retirado.
        CargaJsonResponse otra = carga.cargar(prov, List.of(negro, azul));
        assertEquals(2, otra.getActualizados());
        assertEquals(0, otra.getCreados());
        assertEquals(3, productosDelProveedor(prov));
        assertEquals(false, productos.findById(anterior).orElseThrow().getActivo());
    }

    @Test
    void reemplazoConArticuloInvalidoRevierteCreacionPreviaYConservaAgrupado() {
        Long anterior = cargarUno(prov, art("Apple", "iPhone 17 256GB", "975", esp("256GB", null))).getProductoId();
        var negro = art("Apple", "iPhone 17 256GB Negro", "975", Map.of("color", "Negro"));
        var azul = art("Apple", "iPhone 17 256GB Azul", "0", Map.of("color", "Azul"));
        var e = assertThrows(IllegalArgumentException.class, () -> carga.cargar(prov, List.of(negro, azul), List.of(anterior)));
        assertTrue(e.getMessage().contains("iPhone 17 256GB Azul"), "el error dice qué artículo frenó el lote: " + e.getMessage());
        assertEquals(1, productosDelProveedor(prov));
        assertTrue(productos.findById(anterior).orElseThrow().getActivo());
        assertEquals(1, variantes.findByProductoIdAndActivo(anterior, true).size());
    }

    @Test
    void reemplazoRechazaOtroProveedorYFotosYDuplicados() {
        Long anterior = cargarUno(prov, art("Apple", "iPhone 17 256GB", "975", esp("256GB", null))).getProductoId();
        var negro = art("Apple", "iPhone 17 256GB Negro", "975", Map.of("color", "Negro"));
        Long otro = nuevoProveedor();
        assertThrows(IllegalArgumentException.class, () -> carga.cargar(otro, List.of(negro), List.of(anterior)));
        assertThrows(IllegalArgumentException.class, () -> carga.cargar(prov, List.of(negro, negro), List.of(anterior)));
        negro.setImagenes(List.of("https://example.com/no.jpg"));
        assertThrows(IllegalArgumentException.class, () -> carga.cargar(prov, List.of(negro), List.of(anterior)));
        assertTrue(productos.findById(anterior).orElseThrow().getActivo());
        assertEquals(1, productosDelProveedor(prov));
    }

    @Test
    void sageEsColorYNoOtroModelo() {
        var a = art("Apple", "iPhone 17 256GB Sage", "975", Map.of("almacenamiento", "256GB", "color", "Sage"));
        var vista = carga.identidades(List.of(a)).get(0);
        assertEquals("tel1|apple|iphone 17|-|256|na|-|sage|nuevo|-|-", vista.get("identidad"));
    }
}
