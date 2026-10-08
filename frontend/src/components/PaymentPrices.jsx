import { CASH_DISCOUNT_PERCENTAGE, cashPrice } from '../utils/paymentPricing'

const money = (value) => Number(value).toLocaleString('es-AR', { maximumFractionDigits: 0 })

/**
 * Precio en pesos: el de transferencia en grande, el de contado efectivo y el aviso de cuotas.
 * Los precios en dólares no se muestran al cliente (pedido del usuario, 2026-10-07).
 */
export default function PaymentPrices({ transferPrice, compact = false, cashPriceOverride }) {
  const efectivo = cashPriceOverride ?? cashPrice(transferPrice)

  return (
    <div style={{ marginTop: compact ? '4px' : '8px', fontSize: compact ? '12px' : '14px' }}>
      <div style={{ fontFamily: 'var(--fuente-titulos)', fontSize: compact ? '22px' : '30px', fontWeight: 700, lineHeight: 1.2, color: 'var(--color-text)' }}>
        $ {money(transferPrice)}
      </div>
      <div style={{ color: 'var(--color-text-muted)' }}>por transferencia</div>
      <div style={{ marginTop: '6px', color: 'var(--color-accion)', fontWeight: 600 }}>
        $ {money(efectivo)} en contado efectivo <span style={{ color: 'var(--color-text-muted)', fontWeight: 400 }}>({CASH_DISCOUNT_PERCENTAGE}% OFF)</span>
      </div>
      <p style={{ margin: '6px 0 0', fontSize: '11px', lineHeight: 1.35, color: 'var(--color-text-muted)' }}>
        Pagá con Mercado Pago en cuotas fijas y en pesos
      </p>
    </div>
  )
}
