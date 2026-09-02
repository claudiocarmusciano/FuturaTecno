import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../../cart/CartContext'
import PaymentPrices from '../../components/PaymentPrices'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const MONTO_MINIMO_PEDIDO_USD = 250

function CartPage() {
  const { items, quitar, cambiarCantidad, vaciar, revalidar, totalUsd, totalArs, vacio } = useCart()
  const navigate = useNavigate()
  const [avisos, setAvisos] = useState({ cambios: [], removidos: [] })
  const [revisando, setRevisando] = useState(true)
  const faltaParaMinimo = Math.max(0, MONTO_MINIMO_PEDIDO_USD - Number(totalUsd || 0))
  const alcanzaMinimo = faltaParaMinimo === 0

  // Al abrir el carrito se re-piden los precios: el carrito puede tener días y el precio de venta
  // se recalcula con la cotización del día. Mejor que se entere acá y no al confirmar.
  useEffect(() => {
    let vivo = true
    revalidar()
      .then(res => { if (vivo) setAvisos(res) })
      .catch(() => { /* si falla, se sigue con los precios guardados */ })
      .finally(() => { if (vivo) setRevisando(false) })
    return () => { vivo = false }
    // Solo al montar: revalidar cambia de identidad con cada edición del carrito.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (vacio) {
    return (
      <div>
        <h1>Tu carrito</h1>
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <div style={{ fontSize: '44px', marginBottom: '12px' }}>🛒</div>
          <p style={{ color: 'var(--color-text-muted)', marginBottom: '20px' }}>Todavía no agregaste nada.</p>
          <Link to="/catalogo" className="btn-primario" style={{ textDecoration: 'none' }}>Ver el catálogo</Link>
        </div>
      </div>
    )
  }

  return (
    <div>
      <h1>Tu carrito</h1>

      {revisando && (
        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>Actualizando precios...</p>
      )}

      {avisos.cambios.length > 0 && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-lime)', marginBottom: '16px' }}>
          <strong>Se actualizaron algunos precios</strong>
          <ul style={{ margin: '8px 0 0', paddingLeft: '18px', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            {avisos.cambios.map(c => (
              <li key={c.nombre}>
                {c.nombre}: US$ {formatNumber(c.anterior)} → <strong>US$ {formatNumber(c.nuevo)}</strong>
              </li>
            ))}
          </ul>
        </div>
      )}

      {avisos.removidos.length > 0 && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-danger, #c0392b)', marginBottom: '16px' }}>
          <strong>Sacamos artículos que ya no están disponibles</strong>
          <ul style={{ margin: '8px 0 0', paddingLeft: '18px', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            {avisos.removidos.map(r => <li key={r.varianteId}>{r.nombre}</li>)}
          </ul>
        </div>
      )}

      <div className="card">
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '540px' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--color-border)' }}>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)' }}>Artículo</th>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)', textAlign: 'center' }}>Cantidad</th>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)', textAlign: 'right' }}>Subtotal</th>
                <th style={{ width: '40px' }}></th>
              </tr>
            </thead>
            <tbody>
              {items.map(i => (
                <tr key={i.varianteId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <td style={{ padding: '14px 8px' }}>
                    <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                      {i.imagenUrl
                        ? <img src={i.imagenUrl} alt="" style={{ width: '52px', height: '52px', objectFit: 'contain', borderRadius: '6px' }} />
                        : <div style={{ width: '52px', height: '52px', borderRadius: '6px', background: 'var(--color-accent-light)' }} />}
                      <div>
                        <Link to={`/producto/${i.productoId}`} style={{ fontWeight: 600, color: 'var(--color-text)', textDecoration: 'none' }}>
                          {i.nombre}
                        </Link>
                        {i.especificaciones && (
                          <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{i.especificaciones}</div>
                        )}
                        <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                          US$ {formatNumber(i.precioUsd)} c/u
                        </div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '14px 8px', textAlign: 'center' }}>
                    <input
                      type="number"
                      min="1"
                      value={i.cantidad}
                      onChange={e => cambiarCantidad(i.varianteId, e.target.value)}
                      style={{
                        width: '68px', padding: '7px', textAlign: 'center', fontSize: '14px',
                        border: '1px solid var(--color-border)', borderRadius: '6px'
                      }}
                    />
                  </td>
                  <td style={{ padding: '14px 8px', textAlign: 'right' }}>
                    <div style={{ fontWeight: 600 }}>US$ {formatNumber(i.precioUsd * i.cantidad)}</div>
                    <div style={{ fontSize: '13px', color: 'var(--color-price)' }}>$ {formatNumber(i.precioArs * i.cantidad)}</div>
                  </td>
                  <td style={{ padding: '14px 8px', textAlign: 'right' }}>
                    <button
                      type="button"
                      onClick={() => quitar(i.varianteId)}
                      aria-label={`Quitar ${i.nombre}`}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '17px', color: 'var(--color-text-muted)' }}
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '16px', marginTop: '20px' }}>
          <button
            type="button"
            onClick={vaciar}
            style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '14px', color: 'var(--color-text-muted)', textDecoration: 'underline' }}
          >
            Vaciar carrito
          </button>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '14px', color: 'var(--color-text-muted)' }}>Total de productos</div>
            <div style={{ fontSize: '30px', fontWeight: 700, letterSpacing: '-0.02em' }}>US$ {formatNumber(totalUsd)}</div>
            <PaymentPrices transferPrice={totalArs} />
          </div>
        </div>
      </div>

      {!alcanzaMinimo && (
        <div className="card" style={{ marginTop: '18px', borderLeft: '4px solid var(--color-lime)' }}>
          <strong>Compra mínima: US$ {formatNumber(MONTO_MINIMO_PEDIDO_USD)}</strong>
          <p style={{ margin: '6px 0 0', color: 'var(--color-text-muted)', fontSize: '14px' }}>
            Te faltan US$ {formatNumber(faltaParaMinimo)} en productos para poder confirmar el pedido.
          </p>
        </div>
      )}

      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginTop: '18px' }}>
        <Link to="/catalogo" style={{
          padding: '12px 22px', borderRadius: '8px', textDecoration: 'none', fontWeight: 600,
          border: '1px solid var(--color-border)', color: 'var(--color-text)'
        }}>
          ← Seguir comprando
        </Link>
        <button
          type="button"
          onClick={() => navigate('/checkout')}
          disabled={!alcanzaMinimo}
          style={{
            padding: '12px 26px', borderRadius: '8px', border: 'none',
            cursor: alcanzaMinimo ? 'pointer' : 'not-allowed', opacity: alcanzaMinimo ? 1 : 0.55,
            background: 'var(--color-lime)', color: '#16181d', fontWeight: 700, fontSize: '16px'
          }}
        >
          Confirmar pedido →
        </button>
      </div>
    </div>
  )
}

export default CartPage
