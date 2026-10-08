package com.futuratecno.application;

import com.futuratecno.api.dto.CrearPedidoManualRequest;
import com.futuratecno.api.dto.PedidoDTO;
import com.futuratecno.domain.EstadoPago;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ProveedorRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Órdenes de venta manuales (V45) contra PostgreSQL: pedido sin usuario, sin vencimiento, con
 * precio cambiado a mano y la restricción de que solo una orden MANUAL puede no tener usuario.
 * Misma base descartable que {@link CargaJsonIdentidadPostgresTest}.
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
class PedidoManualPostgresTest {

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

    @MockBean CotizacionService cotizacion;   // nada sale a la red
    @Autowired PedidoService pedidos;
    @Autowired ProveedorRepository proveedores;
    @Autowired JdbcTemplate jdbc;

    /** Variante activa con costo US$ 100: con flete 5% y margen 15% se vende a US$ 120,75. */
    private Long variante(String modelo) {
        Proveedor p = new Proveedor();
        p.setNombre("IT " + System.nanoTime());
        p.setCodigo("M" + (System.nanoTime() % 100000));
        p.setMargenPorcentaje(new BigDecimal("15"));
        p.setFletePorcentaje(new BigDecimal("5"));
        p.setActivo(true);
        Long prov = proveedores.save(p).getId();
        Long prod = jdbc.queryForObject("""
                INSERT INTO productos (proveedor_id, marca, modelo, activo, created_at, updated_at, fuente)
                VALUES (?, 'Raptor', ?, true, now(), now(), 'JSON') RETURNING id""", Long.class, prov, modelo);
        return jdbc.queryForObject("""
                INSERT INTO variantes (producto_id, especificaciones, costo_usd, stock, activo, created_at, updated_at)
                VALUES (?, '', 100, 0, true, now(), now()) RETURNING id""", Long.class, prod);
    }

    private static CrearPedidoManualRequest.Item item(Long varianteId, int cantidad, String precio) {
        var i = new CrearPedidoManualRequest.Item();
        i.setVarianteId(varianteId);
        i.setCantidad(cantidad);
        i.setPrecioUnitarioUsd(precio == null ? null : new BigDecimal(precio));
        return i;
    }

    @Test
    void ordenSinCuentaConPrecioAcordadoNoVenceNiExigeMinimoYSeEncuentraEnLaBandeja() {
        when(cotizacion.obtenerCotizacionUsdArs()).thenReturn(new BigDecimal("1500"));
        Long auris = variante("Auricular Inferno PRO X " + System.nanoTime());
        Long cable = variante("Cable DisplayPort " + System.nanoTime());

        var req = new CrearPedidoManualRequest();
        req.setNombre("Cliente del local");
        req.setTelefono("2284 55-1234");
        req.setEmail("nadie-registrado@example.com");
        req.setMedioPago("EFECTIVO");
        req.setItems(List.of(item(auris, 1, "110"), item(cable, 2, null)));
        PedidoDTO p = pedidos.crearManual(req, "admin@example.com");

        assertEquals("MANUAL", p.getOrigen());
        assertNull(p.getVenceEn(), "una orden manual no vence a las 06:30");
        assertNull(p.getUsuarioEmail(), "el email no corresponde a ninguna cuenta");
        assertEquals("nadie-registrado@example.com", p.getEmailContacto());
        // 110 (acordado) + 2 × 120,75 (catálogo): por debajo de US$ 250 igual se acepta.
        assertEquals(0, new BigDecimal("351.50").compareTo(p.getTotalUsd()));
        assertEquals(0, new BigDecimal("120.75").compareTo(p.getItems().get(0).getPrecioCatalogoUsd()),
                "el precio cambiado deja registro del de catálogo");
        assertNull(p.getItems().get(1).getPrecioCatalogoUsd(), "sin cambio no se anota nada");

        // Búsqueda: por teléfono sin espacios, por artículo sin tildes, por origen y por fecha.
        assertTrue(pedidos.listarParaAdmin(null, "228455", null, null, null, null).stream().anyMatch(x -> x.getNumero().equals(p.getNumero())));
        assertTrue(pedidos.listarParaAdmin(null, "displayport", null, null, "EFECTIVO", "MANUAL").stream().anyMatch(x -> x.getNumero().equals(p.getNumero())));
        assertTrue(pedidos.listarParaAdmin(null, null, null, null, null, "WEB").stream().noneMatch(x -> x.getNumero().equals(p.getNumero())));
        assertTrue(pedidos.listarParaAdmin(null, null, LocalDate.now().plusDays(1), null, null, null).stream().noneMatch(x -> x.getNumero().equals(p.getNumero())));

        // El vencimiento de las 06:30 no la toca, y el cobro manual no rompe por no tener cuenta (puntos).
        pedidos.vencerPendientes();
        assertEquals("PENDIENTE", pedidos.obtenerParaAdmin(p.getNumero()).getEstado());
        var cobrado = pedidos.cambiarEstadoPagoManual(p.getId(), EstadoPago.APROBADO);
        assertEquals("CONFIRMADO", cobrado.getEstado());
    }

    @Test
    void unPedidoWebVencidoSeReactivaSinVencimientoYAdmiteCobroManual() {
        when(cotizacion.obtenerCotizacionUsdArs()).thenReturn(new BigDecimal("1500"));
        Long v = variante("Notebook " + System.nanoTime());
        var req = new CrearPedidoManualRequest();
        req.setNombre("Cliente web");
        req.setMedioPago("EFECTIVO");
        req.setItems(List.of(item(v, 1, null)));
        PedidoDTO p = pedidos.crearManual(req, "admin@example.com");

        // Se lo convierte en un pedido web de Mercado Pago que venció anoche.
        Long usuario = jdbc.queryForObject("""
                INSERT INTO usuarios (email, password, created_at, updated_at)
                VALUES (?, 'x', now(), now()) RETURNING id""", Long.class, "web" + System.nanoTime() + "@example.com");
        jdbc.update("""
                UPDATE pedidos SET origen = 'WEB', usuario_id = ?, medio_pago = 'MERCADO_PAGO', estado = 'VENCIDO',
                       vence_en = now() - interval '1 day', mercado_pago_preference_id = 'pref-vieja',
                       mercado_pago_checkout_url = 'https://mp.test/vieja'
                 WHERE id = ?""", usuario, p.getId());
        assertThrows(IllegalArgumentException.class, () -> pedidos.cambiarEstadoPagoManual(p.getId(), EstadoPago.APROBADO),
                "un pedido web de Mercado Pago vigente no se cobra a mano");

        var reactivado = pedidos.cambiarEstado(p.getId(), com.futuratecno.domain.EstadoPedido.PENDIENTE);
        assertEquals("PENDIENTE", reactivado.getEstado());
        assertNull(reactivado.getVenceEn(), "reactivado no vuelve a vencer a las 06:30");
        assertNull(jdbc.queryForObject("SELECT mercado_pago_checkout_url FROM pedidos WHERE id = ?", String.class, p.getId()),
                "el link de pago viejo caducó: se genera uno nuevo");

        pedidos.vencerPendientes();
        assertEquals("PENDIENTE", pedidos.obtenerParaAdmin(p.getNumero()).getEstado());
        assertEquals("CONFIRMADO", pedidos.cambiarEstadoPagoManual(p.getId(), EstadoPago.APROBADO).getEstado(),
                "el admin registra el cobro aunque haya sido de Mercado Pago");
    }

    @Test
    void validaNombrePrecioYMedioDePagoYLaBaseExigeUsuarioEnLasOrdenesWeb() {
        when(cotizacion.obtenerCotizacionUsdArs()).thenReturn(new BigDecimal("1500"));
        Long v = variante("Mouse " + System.nanoTime());
        var sinNombre = new CrearPedidoManualRequest();
        sinNombre.setItems(List.of(item(v, 1, null)));
        assertThrows(IllegalArgumentException.class, () -> pedidos.crearManual(sinNombre, "admin"));

        var precioCero = new CrearPedidoManualRequest();
        precioCero.setNombre("X");
        precioCero.setItems(List.of(item(v, 1, "0")));
        assertThrows(IllegalArgumentException.class, () -> pedidos.crearManual(precioCero, "admin"));

        var medioRaro = new CrearPedidoManualRequest();
        medioRaro.setNombre("X");
        medioRaro.setMedioPago("CRIPTO");
        medioRaro.setItems(List.of(item(v, 1, null)));
        assertThrows(IllegalArgumentException.class, () -> pedidos.crearManual(medioRaro, "admin"));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO pedidos (numero, estado, total_usd, total_ars, cotizacion_usada, medio_pago, estado_pago, origen, created_at, updated_at)
                VALUES ('FT-TEST-WEB', 'PENDIENTE', 1, 1, 1, 'TRANSFERENCIA', 'SIN_INICIAR', 'WEB', now(), now())"""));
    }
}
