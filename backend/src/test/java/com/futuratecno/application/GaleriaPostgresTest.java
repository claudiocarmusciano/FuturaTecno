package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
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

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Fotos de galería cargadas a mano: sobreviven a una nueva carga del producto (V46). */
@EnabledIfSystemProperty(named = "pg.it", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/futuratecno_it",
        "spring.datasource.username=futuratecno",
        "spring.datasource.password=futuratecno",
        "spring.flyway.clean-disabled=false",
        "anthropic.api-key=", "openai.api-key=", "deepseek.api-key=",
        "sync.enabled=false"
})
class GaleriaPostgresTest {

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

    // PNG de 1x1 válido: el validador exige la firma real de una imagen.
    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Autowired GaleriaService galeria;
    @Autowired CargaJsonService carga;
    @Autowired ProveedorRepository proveedores;
    @Autowired ProductoRepository productos;

    private HttpServer server;
    private String base;

    @BeforeEach
    void servidor() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/foto", ex -> {
            ex.getResponseHeaders().add("Content-Type", "image/png");
            ex.sendResponseHeaders(200, PNG.length);
            ex.getResponseBody().write(PNG);
            ex.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void cerrar() {
        server.stop(0);
    }

    private ArticuloJsonDTO articulo(String... imagenes) {
        ArticuloJsonDTO a = new ArticuloJsonDTO();
        a.setMarca("Raptor");
        a.setModelo("Escritorio Prueba Galeria");
        a.setPrecioUsd(new BigDecimal("200"));
        a.setImagenes(List.of(imagenes));
        return a;
    }

    @Test
    void laFotoManualSobreviveAUnaNuevaCarga() {
        Proveedor p = new Proveedor();
        p.setNombre("Proveedor galería " + System.nanoTime());
        p.setMargenPorcentaje(new BigDecimal("15"));
        p.setFletePorcentaje(BigDecimal.ZERO);
        p.setActivo(true);
        p = proveedores.save(p);

        carga.cargar(p.getId(), List.of(articulo(base + "/foto?a", base + "/foto?b")));
        Long id = productos.findByProveedorIdAndActivo(p.getId(), true).get(0).getId();

        List<Map<String, Object>> fotos = galeria.agregar(id, base + "/foto?manual");
        assertTrue(fotos.stream().anyMatch(f -> "manual".equals(f.get("origen"))), fotos.toString());

        // Repetida y sin https:// se rechazan. (Una dirección rota no se puede probar acá: el validador
        // no consulta direcciones locales, por seguridad, y las toma como dudosas.)
        assertThrows(IllegalArgumentException.class, () -> galeria.agregar(id, base + "/foto?manual"));
        assertThrows(IllegalArgumentException.class, () -> galeria.agregar(id, "foto.jpg"));

        // Una nueva carga reemplaza las fotos del proveedor, pero no la manual.
        carga.cargar(p.getId(), List.of(articulo(base + "/foto?c")));
        List<Map<String, Object>> despues = galeria.listar(id);
        assertTrue(despues.stream().anyMatch(f -> (base + "/foto?manual").equals(f.get("url"))), despues.toString());
        assertTrue(despues.stream().noneMatch(f -> (base + "/foto?b").equals(f.get("url"))), despues.toString());

        // Solo se pueden quitar las manuales.
        Long delMayorista = despues.stream().filter(f -> "mayorista".equals(f.get("origen")))
                .map(f -> (Long) f.get("id")).findFirst().orElse(null);
        if (delMayorista != null) {
            assertThrows(IllegalArgumentException.class, () -> galeria.quitar(id, delMayorista));
        }
        Long manual = despues.stream().filter(f -> "manual".equals(f.get("origen")))
                .map(f -> (Long) f.get("id")).findFirst().orElseThrow();
        assertTrue(galeria.quitar(id, manual).stream().noneMatch(f -> "manual".equals(f.get("origen"))));
    }
}
