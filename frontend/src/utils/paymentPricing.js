// Checkout online con acreditación inmediata: 6,29% + IVA. Estos valores deben mantenerse
// alineados con application.yml. Los factores de cuotas son orientativos: MP define el valor
// definitivo según tarjeta, emisor y promociones al entrar al checkout.
export const MP_EFFECTIVE_FEE = 0.0629 * 1.21

export const mpImmediatePrice = (transferPrice) =>
  Number(transferPrice || 0) / (1 - MP_EFFECTIVE_FEE)

const INSTALLMENT_TOTAL_FACTORS = {
  3: 1.19689768,
  6: 1.32139995,
  12: 1.55389995
}

export const paymentOptions = (transferPrice) => {
  const mpPrice = mpImmediatePrice(transferPrice)
  return [
    { installments: 1, installmentAmount: mpPrice, total: mpPrice },
    ...Object.entries(INSTALLMENT_TOTAL_FACTORS).map(([installments, factor]) => ({
      installments: Number(installments),
      installmentAmount: mpPrice * factor / Number(installments),
      total: mpPrice * factor
    }))
  ]
}
