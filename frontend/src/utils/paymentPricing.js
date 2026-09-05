// Checkout online con acreditación inmediata: 6,29% + IVA. Estos valores deben mantenerse
// alineados con application.yml. Los factores de cuotas son orientativos: MP define el valor
// definitivo según tarjeta, emisor y promociones al entrar al checkout.
export const MP_EFFECTIVE_FEE = 0.0629 * 1.21

export const mpImmediatePrice = (transferPrice) =>
  Number(transferPrice || 0) / (1 - MP_EFFECTIVE_FEE)

export const CASH_DISCOUNT_PERCENTAGE = 7

/** El descuento por contado aplica al valor de los productos, no al flete. */
export const cashPrice = (transferPrice) =>
  Number(transferPrice || 0) * (1 - CASH_DISCOUNT_PERCENTAGE / 100)
