package com.futuratecno.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrecioServiceTest {

    @Test
    void recuperaElPrecioBaseLuegoDeComisionInmediataEIva() {
        PrecioService service = new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"), new BigDecimal("7"), new BigDecimal("21"));

        assertEquals(new BigDecimal("899382.18"),
                service.precioMercadoPagoInmediato(new BigDecimal("830931.10")));
    }

    private static PrecioService servicio() {
        return new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"), new BigDecimal("7"), new BigDecimal("21"));
    }

    private static com.futuratecno.domain.Variante variante(String costoUsd) {
        var v = new com.futuratecno.domain.Variante();
        v.setCostoUsd(new BigDecimal(costoUsd));
        return v;
    }

    private static com.futuratecno.domain.Proveedor proveedor(String flete, String margen) {
        var p = new com.futuratecno.domain.Proveedor();
        p.setFletePorcentaje(new BigDecimal(flete));
        p.setMargenPorcentaje(new BigDecimal(margen));
        return p;
    }

    private static com.futuratecno.domain.Producto producto(String flete, String margen) {
        var p = new com.futuratecno.domain.Producto();
        if (flete != null) p.setFletePorcentaje(new BigDecimal(flete));
        if (margen != null) p.setMargenPorcentaje(new BigDecimal(margen));
        return p;
    }

    /** Sin override sigue mandando el proveedor: 100 × 1,05 × 1,15. */
    @Test
    void sinOverrideUsaLosPorcentajesDelProveedor() {
        assertEquals(new BigDecimal("120.75"),
                servicio().precioVentaUsd(variante("100"), producto(null, null), proveedor("5", "15")));
    }

    /** Con override manda el producto: 100 × 1,05 × 1,40. */
    @Test
    void elOverrideDelProductoGanaSobreElDelProveedor() {
        assertEquals(new BigDecimal("147.00"),
                servicio().precioVentaUsd(variante("100"), producto(null, "40"), proveedor("5", "15")));
    }

    /**
     * 0% es un override válido y distinto de "sin override": vender al costo. Si se hubiera usado
     * un coalesce a cero en vez de comparar contra null, este caso sería indistinguible del vacío.
     */
    @Test
    void ceroPorCientoEsUnOverrideRealNoUnCampoVacio() {
        assertEquals(new BigDecimal("100.00"),
                servicio().precioVentaUsd(variante("100"), producto("0", "0"), proveedor("5", "15")));
    }

    /** Un producto sin proveedor cargado no puede reventar el catálogo. */
    @Test
    void sinProveedorNiOverrideDevuelveElCosto() {
        assertEquals(new BigDecimal("100.00"),
                servicio().precioVentaUsd(variante("100"), producto(null, null), null));
    }

    @Test
    void aplicaSietePorCientoDeDescuentoAlContado() {
        PrecioService service = new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"), new BigDecimal("7"), new BigDecimal("21"));

        assertEquals(new BigDecimal("930.00"), service.precioContadoEfectivo(new BigDecimal("1000.00")));
    }

    @Test
    void totalACobrarSumaIvaSoloALoBancarizadoYNoAlEnvio() {
        PrecioService service = new PrecioService(new BigDecimal("6.29"), new BigDecimal("21"), new BigDecimal("5"), new BigDecimal("21"));
        BigDecimal productos = new BigDecimal("1000.00");
        BigDecimal envio = new BigDecimal("100.00");

        // Transferencia: (1000 − 0) × 1,21 + 100
        assertEquals(new BigDecimal("1310.00"), service.totalACobrar("TRANSFERENCIA", productos, null, envio, new BigDecimal("21")));
        // Efectivo: 5% off y sin IVA
        assertEquals(new BigDecimal("1050.00"), service.totalACobrar("EFECTIVO", productos, null, envio, BigDecimal.ZERO));
        // Mercado Pago: base con IVA y además la comisión
        assertEquals(service.precioMercadoPagoInmediato(new BigDecimal("1310.00")),
                service.totalACobrar("MERCADO_PAGO", productos, null, envio, new BigDecimal("21")));
        // Pedido viejo (sin IVA guardado): igual que antes
        assertEquals(new BigDecimal("1100.00"), service.totalACobrar("TRANSFERENCIA", productos, null, envio, BigDecimal.ZERO));
        // Los puntos se descuentan antes del IVA
        assertEquals(new BigDecimal("1068.00"), service.totalACobrar("TRANSFERENCIA", productos, new BigDecimal("200"), envio, new BigDecimal("21")));
        assertEquals(new BigDecimal("21"), service.ivaParaMedioPago("MERCADO_PAGO"));
        assertEquals(BigDecimal.ZERO, service.ivaParaMedioPago("EFECTIVO"));
    }
}
