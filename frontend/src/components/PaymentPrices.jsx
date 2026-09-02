import { paymentOptions } from '../utils/paymentPricing'

const money = (value) => Number(value).toLocaleString('es-AR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

/** Precios comparables. El base es transferencia; las cuotas son una estimación visible. */
export default function PaymentPrices({ transferPrice, compact = false }) {
  const options = paymentOptions(transferPrice)

  return (
    <div style={{ marginTop: compact ? '7px' : '12px', fontSize: compact ? '12px' : '14px' }}>
      <div style={{ color: 'var(--color-price)', fontWeight: 700 }}>
        $ {money(transferPrice)} por transferencia
      </div>
      <div style={{ marginTop: '4px', color: 'var(--color-text)' }}>
        Mercado Pago:
      </div>
      <div style={{ display: 'grid', gap: '2px', marginTop: '3px', color: 'var(--color-text-muted)' }}>
        {options.map(option => (
          <span key={option.installments}>
            {option.installments === 1 ? '1 pago' : `${option.installments} cuotas estimadas`} de{' '}
            <strong style={{ color: 'var(--color-text)' }}>$ {money(option.installmentAmount)}</strong>
          </span>
        ))}
      </div>
      <p style={{ margin: '7px 0 0', fontSize: '11px', lineHeight: 1.35, color: 'var(--color-text-muted)' }}>
        Cuotas estimadas: el valor depende de la tarjeta de crédito. Mercado Pago informa el importe definitivo antes de confirmar.
      </p>
    </div>
  )
}
