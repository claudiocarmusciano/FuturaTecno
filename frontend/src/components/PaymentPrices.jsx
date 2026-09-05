import { CASH_DISCOUNT_PERCENTAGE, cashPrice } from '../utils/paymentPricing'

const money = (value) => Number(value).toLocaleString('es-AR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

/** Resumen comercial simple: transferencia, contado y alternativas de cuotas. */
export default function PaymentPrices({ transferPrice, compact = false, cashPriceOverride }) {
  const efectivo = cashPriceOverride ?? cashPrice(transferPrice)

  return (
    <div style={{ marginTop: compact ? '7px' : '12px', fontSize: compact ? '12px' : '14px' }}>
      <div style={{ color: 'var(--color-price)', fontWeight: 700 }}>
        $ {money(transferPrice)} por transferencia
      </div>
      <div style={{ marginTop: '4px', color: 'var(--color-lime)', fontWeight: 700 }}>
        $ {money(efectivo)} en contado efectivo <span style={{ color: 'var(--color-text-muted)', fontWeight: 400 }}>({CASH_DISCOUNT_PERCENTAGE}% OFF)</span>
      </div>
      <p style={{ margin: '7px 0 0', fontSize: '11px', lineHeight: 1.35, color: 'var(--color-text-muted)' }}>
        También podés pagar con Mercado Pago en 1, 3, 6 o 12 cuotas fijas.
      </p>
    </div>
  )
}
