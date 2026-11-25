// Los precios del catálogo son SIN IVA. Lo bancarizado (transferencia y Mercado Pago) se factura
// y suma el IVA a los productos (no al envío); el efectivo y los dólares van sin IVA.
// Tiene que coincidir con iva.venta-porcentaje del backend (application.yml).
export const IVA_PERCENTAGE = 21

/** Precio con IVA, el que se paga por transferencia. */
export const withIva = (precioSinIva) =>
  Number(precioSinIva || 0) * (1 + IVA_PERCENTAGE / 100)

// Checkout online con acreditación inmediata: 6,29% + IVA. Estos valores deben mantenerse
// alineados con application.yml. Los factores de cuotas son orientativos: MP define el valor
// definitivo según tarjeta, emisor y promociones al entrar al checkout.
export const MP_EFFECTIVE_FEE = 0.0629 * 1.21

/** Bruto a cobrar por Mercado Pago a partir del importe YA con IVA (y envío). */
export const mpImmediatePrice = (importeConIva) =>
  Number(importeConIva || 0) / (1 - MP_EFFECTIVE_FEE)

// Tiene que coincidir con contado-efectivo.descuento-porcentaje del backend (application.yml).
export const CASH_DISCOUNT_PERCENTAGE = 5

/** El descuento por contado aplica al valor de los productos (sin IVA), no al flete. */
export const cashPrice = (precioSinIva) =>
  Number(precioSinIva || 0) * (1 - CASH_DISCOUNT_PERCENTAGE / 100)
