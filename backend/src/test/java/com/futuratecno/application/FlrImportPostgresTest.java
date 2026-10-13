package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import com.sun.net.httpserver.HttpServer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flor Qtech contra PostgreSQL real: la planilla entra por la carga JSON, una segunda lectura no
 * duplica, y lo que deja de estar en la lista sale de la tienda y vuelve cuando reaparece.
 * Con -Dflr.csv=/ruta/lista.csv además carga la planilla real y deja el resultado en /tmp/flr-resultado.txt.
 */
@EnabledIfSystemProperty(named = "pg.it", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/futuratecno_it",
        "spring.datasource.username=futuratecno",
        "spring.datasource.password=futuratecno",
        "spring.flyway.clean-disabled=false",
        "anthropic.api-key=", "openai.api-key=", "deepseek.api-key=",
        "sync.enabled=false"
})
class FlrImportPostgresTest {

    @TestConfiguration
    static class BaseLimpia {
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

    @Autowired FlrImportService flr;
    @Autowired ProveedorRepository proveedores;
    @Autowired ProductoRepository productos;

    private HttpServer server;
    private volatile String csv = "";
    private Proveedor proveedor;

    @BeforeEach
    void preparar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/lista.csv", ex -> {
            byte[] b = csv.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
        Proveedor p = new Proveedor();
        p.setNombre("Flor Qtech " + System.nanoTime());
        p.setCodigo("FLR");
        p.setMargenPorcentaje(new BigDecimal("15"));
        p.setFletePorcentaje(BigDecimal.ZERO);
        p.setActivo(true);
        proveedor = proveedores.save(p);
        ReflectionTestUtils.setField(flr, "csvUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/lista.csv");
        ReflectionTestUtils.setField(flr, "proveedorId", proveedor.getId());
    }

    @AfterEach
    void cerrar() {
        server.stop(0);
    }

    private static String lista(boolean conS25) {
        StringBuilder sb = new StringBuilder();
        if (conS25) sb.append("CELULAR SAMSUNG S25 ULTRA 12/256GB BLACK,ANDROID,,805,Disponible,800,795,790\n");
        sb.append("APPLE EARPODS USB-C (MYQY3AM/A),APPLE PRODUCTS,,30,Disponible,28,27,25\n");
        sb.append("APPLE MAGIC MOUSE MK2E3AM/A SILVER OOO (OPEN BOX),APPLE PRODUCTS,,95,Disponible,,,\n");
        sb.append("PERFUME CREED AVENTUS 100ML,PERFUMES,,300,Disponible,,,\n");
        sb.append("\"APPLE MACBOOK AIR 13\"\" M1 8/256GB\",APPLE PRODUCTS,,Sin precio,Disponible,,,\n");
        for (int i = 1; i <= 20; i++) {
            sb.append("PARLANTE SPEAKER JBL MODELO").append(i).append(" BLACK,AUDIO,,").append(60 + i).append(",Disponible,,,\n");
        }
        return sb.toString();
    }

    @Test
    void cargaActualizaYSacaLoQueFalta() throws Exception {
        csv = lista(true);
        Map<String, Object> r1 = flr.sincronizar();
        assertEquals(21, r1.get("creados"), r1.get("mensaje").toString());
        Producto s25 = productos.findByProveedorIdAndActivo(proveedor.getId(), true).stream()
                .filter(p -> p.getModelo().startsWith("S25")).findFirst().orElseThrow();
        assertEquals("Samsung", s25.getMarca());

        Map<String, Object> r2 = flr.sincronizar();
        assertEquals(0, r2.get("creados"), r2.get("mensaje").toString());
        assertEquals(21, r2.get("actualizados"));

        csv = lista(false);
        Map<String, Object> r3 = flr.sincronizar();
        assertEquals(1, r3.get("dadosDeBaja"), r3.get("mensaje").toString());
        assertFalse(productos.findById(s25.getId()).orElseThrow().getActivo());

        csv = lista(true);
        Map<String, Object> r4 = flr.sincronizar();
        assertEquals(0, r4.get("creados"), r4.get("mensaje").toString());
        assertTrue(productos.findById(s25.getId()).orElseThrow().getActivo());

        // Una planilla rota (casi vacía) no vacía la tienda.
        csv = "CELULAR SAMSUNG S25 ULTRA 12/256GB BLACK,ANDROID,,805,Disponible,,,\n";
        assertEquals(0, flr.sincronizar().get("dadosDeBaja"));
    }

    @Test
    @EnabledIfSystemProperty(named = "flr.csv", matches = ".+")
    void planillaReal() throws Exception {
        csv = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("flr.csv")));
        Map<String, Object> r = flr.sincronizar();
        StringBuilder sb = new StringBuilder(r.get("mensaje").toString()).append('\n');
        for (Object o : (java.util.List<?>) r.get("items")) {
            var it = (com.futuratecno.api.dto.CargaJsonResponse.Item) o;
            sb.append(it.getEstado()).append(" | ").append(it.getProducto()).append(" | ")
              .append(it.getCategoria()).append(" | ").append(it.getMotivo() != null ? it.getMotivo() : "").append('\n');
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/flr-resultado.txt"), sb);
    }
}
