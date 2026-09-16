package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.domain.Variante;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Única fuente de verdad de la fórmula de precios:
 * {@code precio de venta USD = costo USD × (1 + flete%) × (1 + margen%)}.
 *
 * <p>El flete es un porcentaje por proveedor, no un monto fijo. Vive acá y no duplicado en cada
 * servicio porque lo usan tanto el catálogo (precio que ve el cliente) como los pedidos (precio que
 * se congela al confirmar): si las dos cuentas se separaran, un cliente podría ver un precio y que
 * se le registre otro.
 */
@Service
public class PrecioService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private final BigDecimal comisionMercadoPagoPct;
    private final BigDecimal ivaComisionPct;
    private final BigDecimal descuentoContadoEfectivoPct;

    public PrecioService(
            @Value("${mercadopago.comision-inmediata-porcentaje:6.29}") BigDecimal comisionMercadoPagoPct,
            @Value("${mercadopago.iva-comision-porcentaje:21}") BigDecimal ivaComisionPct,
            @Value("${contado-efectivo.descuento-porcentaje:7}") BigDecimal descuentoContadoEfectivoPct) {
        this.comisionMercadoPagoPct = comisionMercadoPagoPct;
        this.ivaComisionPct = ivaComisionPct;
        this.descuentoContadoEfectivoPct = descuentoContadoEfectivoPct;
    }

    /**
     * Precio de venta en USD de una variante, redondeado a 2 decimales.
     *
     * <p>El margen y el flete salen del PRODUCTO si los tiene cargados (V41) y del proveedor si no.
     * Null en el producto significa "usá el del proveedor", nunca 0%: 0 sería vender al costo.
     * Por eso se compara contra null y no se usa un coalesce a cero.
     */
    public BigDecimal precioVentaUsd(Variante variante, Producto producto, Proveedor proveedor) {
        BigDecimal fletePct = primero(
                producto != null ? producto.getFletePorcentaje() : null,
                proveedor != null ? proveedor.getFletePorcentaje() : null);
        BigDecimal margenPct = primero(
                producto != null ? producto.getMargenPorcentaje() : null,
                proveedor != null ? proveedor.getMargenPorcentaje() : null);

        BigDecimal factorFlete = BigDecimal.ONE.add(fletePct.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal factorMargen = BigDecimal.ONE.add(margenPct.divide(CIEN, 6, RoundingMode.HALF_UP));

        return variante.getCostoUsd()
                .multiply(factorFlete)
                .multiply(factorMargen)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Primer valor no nulo, o cero si no hay ninguno (proveedor sin configurar). */
    private static BigDecimal primero(BigDecimal... valores) {
        for (BigDecimal v : valores) if (v != null) return v;
        return BigDecimal.ZERO;
    }

    /** Convierte un importe en USD a ARS con la cotización dada, redondeado a 2 decimales. */
    public BigDecimal aArs(BigDecimal usd, BigDecimal cotizacion) {
        return usd.multiply(cotizacion).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Bruto a cobrar para que, descontada la comisión inmediata y su IVA, quede el importe base.
     * Se divide por (1 - tasa efectiva); sumar 7,6109% no alcanzaría a recuperar el mismo neto.
     */
    public BigDecimal precioMercadoPagoInmediato(BigDecimal importeBase) {
        if (importeBase == null) return BigDecimal.ZERO.setScale(2);
        BigDecimal comision = comisionMercadoPagoPct.divide(CIEN, 8, RoundingMode.HALF_UP);
        BigDecimal iva = BigDecimal.ONE.add(ivaComisionPct.divide(CIEN, 8, RoundingMode.HALF_UP));
        BigDecimal divisor = BigDecimal.ONE.subtract(comision.multiply(iva));
        if (divisor.signum() <= 0) throw new IllegalStateException("La comisión de Mercado Pago configurada no es válida.");
        return importeBase.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /** Descuento comercial aplicable solamente a los productos abonados al contado. */
    public BigDecimal precioContadoEfectivo(BigDecimal importeProductos) {
        if (importeProductos == null) return BigDecimal.ZERO.setScale(2);
        BigDecimal descuento = descuentoContadoEfectivoPct.divide(CIEN, 8, RoundingMode.HALF_UP);
        return importeProductos.multiply(BigDecimal.ONE.subtract(descuento)).setScale(2, RoundingMode.HALF_UP);
    }
}
