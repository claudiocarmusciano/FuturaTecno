import { CASH_DISCOUNT_PERCENTAGE, cashPrice, withIva } from '../utils/paymentPricing'

const money = (value) => Number(value).toLocaleString('es-AR', { maximumFractionDigits: 0 })
const dolares = (value) => Number(value).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/**
 * Precios de un producto o total. `precio` es el precio SIN IVA del catálogo.
 * En grande el de contado efectivo (sin IVA, con el 5% off) y los dólares (sin IVA); abajo el de
 * transferencia con IVA incluido y el aviso de Mercado Pago. Lo bancarizado se factura (2026-11-25).
 * Los overrides los usa el checkout, que ya trae los totales con envío y puntos.
 */
export default function PaymentPrices({ precio, compact = false, cashPriceOverride, transferPriceOverride, usdPrice }) {
  const efectivo = cashPriceOverride ?? cashPrice(precio)
  const transferencia = transferPriceOverride ?? withIva(precio)

  return (
    <div className="pp" style={{ marginTop: compact ? '4px' : '8px', fontSize: compact ? '12px' : '14px' }}>
      <div className="pp-principal" style={{ fontFamily: 'var(--fuente-titulos)', fontSize: compact ? '22px' : '30px', fontWeight: 700, lineHeight: 1.2, color: 'var(--color-text)' }}>
        $ {money(efectivo)}
      </div>
      <div className="pp-sub" style={{ color: 'var(--color-text-muted)' }}>
        <span className="pp-largo">en contado </span>efectivo <span className="pp-largo">({CASH_DISCOUNT_PERCENTAGE}% OFF)</span>
        {Number(usdPrice) > 0 && <> · <strong style={{ color: 'var(--color-text)', fontWeight: 600 }}>US$ {dolares(usdPrice)}</strong></>}
      </div>
      <div className="pp-secundario" style={{ marginTop: '6px', color: 'var(--color-text)', fontWeight: 600 }}>
        $ {money(transferencia)} por transferencia <span className="pp-largo" style={{ color: 'var(--color-text-muted)', fontWeight: 400 }}>(IVA incluido)</span>
      </div>
      <p className="pp-cuotas" style={{ margin: '6px 0 0', fontSize: '11px', lineHeight: 1.35, color: 'var(--color-text-muted)' }}>
        Pagá con Mercado Pago en cuotas fijas y en pesos (IVA incluido)
      </p>
    </div>
  )
}
