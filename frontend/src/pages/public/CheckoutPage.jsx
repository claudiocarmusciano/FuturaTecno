import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'
import { useCart } from '../../cart/CartContext'
import { WHATSAPP_NUMBER, NOMBRE_NEGOCIO } from '../../config'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** Arma el mensaje de WhatsApp con el pedido completo (antes era de a un producto por vez). */
const mensajeWhatsapp = (pedido) => {
  const lineas = pedido.items.map(i => `• ${i.cantidad}× ${i.productoNombre} — US$ ${formatNumber(i.subtotalUsd)}`)
  return `Hola ${NOMBRE_NEGOCIO}, hice el pedido ${pedido.numero}:\n\n${lineas.join('\n')}\n\nTotal: US$ ${formatNumber(pedido.totalUsd)}`
}

function CheckoutPage() {
  const { user, isAuth, listo } = useAuth()
  const { items, totalUsd, totalArs, vacio, vaciar } = useCart()
  const navigate = useNavigate()

  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [notas, setNotas] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (user?.nombre) setNombre(user.nombre)
  }, [user])

  if (!listo) return null

  if (vacio) {
    return (
      <div>
        <h1>Confirmar pedido</h1>
        <div className="card">
          <p style={{ color: 'var(--color-text-muted)' }}>El carrito está vacío.</p>
          <Link to="/catalogo">← Volver al catálogo</Link>
        </div>
      </div>
    )
  }

  // El carrito se arma sin cuenta; la sesión se pide recién acá. Al volver del login
  // se vuelve a esta misma pantalla con el carrito intacto (vive en localStorage).
  if (!isAuth) {
    return (
      <div>
        <h1>Confirmar pedido</h1>
        <div className="card" style={{ textAlign: 'center', padding: '40px 20px' }}>
          <p style={{ fontSize: '17px', marginBottom: '6px' }}>Necesitás una cuenta para confirmar</p>
          <p style={{ color: 'var(--color-text-muted)', fontSize: '14px', marginBottom: '22px' }}>
            Es para que puedas seguir tu pedido y tener el historial. Tu carrito se mantiene.
          </p>
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link
              to="/login"
              state={{ from: '/checkout' }}
              style={{ padding: '12px 26px', borderRadius: '8px', background: 'var(--color-lime)', color: '#16181d', fontWeight: 700, textDecoration: 'none' }}
            >
              Ingresar
            </Link>
            <Link
              to="/registro"
              state={{ from: '/checkout' }}
              style={{ padding: '12px 26px', borderRadius: '8px', border: '1px solid var(--color-border)', color: 'var(--color-text)', fontWeight: 600, textDecoration: 'none' }}
            >
              Crear cuenta
            </Link>
          </div>
        </div>
      </div>
    )
  }

  const confirmar = async (e) => {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const { data: pedido } = await axios.post('/api/pedidos', {
        items: items.map(i => ({ varianteId: i.varianteId, cantidad: i.cantidad })),
        nombreContacto: nombre,
        telefonoContacto: telefono,
        notas
      })
      vaciar()
      // Abre WhatsApp con el pedido completo y deja al usuario en el detalle.
      window.open(`https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(mensajeWhatsapp(pedido))}`, '_blank')
      navigate(`/pedido/${pedido.numero}`, { replace: true })
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo confirmar el pedido. Probá de nuevo.')
      setEnviando(false)
    }
  }

  return (
    <div>
      <h1>Confirmar pedido</h1>

      <div className="card">
        <h2 style={{ fontSize: '17px', marginTop: 0 }}>Resumen</h2>
        {items.map(i => (
          <div key={i.varianteId} style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', padding: '9px 0', borderBottom: '1px solid var(--color-border)' }}>
            <span>
              {i.cantidad}× {i.nombre}
              {i.especificaciones && <span style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}> · {i.especificaciones}</span>}
            </span>
            <span style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>US$ {formatNumber(i.precioUsd * i.cantidad)}</span>
          </div>
        ))}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '14px', fontSize: '19px', fontWeight: 700 }}>
          <span>Total</span>
          <span>US$ {formatNumber(totalUsd)}</span>
        </div>
        <div style={{ textAlign: 'right', color: 'var(--color-price)' }}>$ {formatNumber(totalArs)}</div>
      </div>

      {/* Advertencia de vigencia: se acordó avisarlo al confirmar (y va también en el email). */}
      <div className="card" style={{ borderLeft: '4px solid var(--color-lime)' }}>
        <strong>Tu pedido vale hasta mañana a las 6:30 de la mañana.</strong>
        <p style={{ margin: '6px 0 0', fontSize: '14px', color: 'var(--color-text-muted)' }}>
          Los precios se actualizan cada mañana junto con el stock de nuestros proveedores.
          Si no llegamos a cerrarlo antes de ese horario, el pedido vence y hay que rehacerlo
          con los precios del día.
        </p>
      </div>

      <form onSubmit={confirmar} className="card">
        <h2 style={{ fontSize: '17px', marginTop: 0 }}>Tus datos</h2>

        <label style={{ display: 'block', fontSize: '14px', marginBottom: '4px' }}>Nombre</label>
        <input
          value={nombre}
          onChange={e => setNombre(e.target.value)}
          required
          style={{ width: '100%', padding: '10px', marginBottom: '14px', border: '1px solid var(--color-border)', borderRadius: '8px', fontSize: '15px' }}
        />

        <label style={{ display: 'block', fontSize: '14px', marginBottom: '4px' }}>Teléfono</label>
        <input
          value={telefono}
          onChange={e => setTelefono(e.target.value)}
          required
          placeholder="2284 12-3456"
          style={{ width: '100%', padding: '10px', marginBottom: '14px', border: '1px solid var(--color-border)', borderRadius: '8px', fontSize: '15px' }}
        />

        <label style={{ display: 'block', fontSize: '14px', marginBottom: '4px' }}>Notas (opcional)</label>
        <textarea
          value={notas}
          onChange={e => setNotas(e.target.value)}
          rows={3}
          placeholder="Algo que quieras aclarar sobre el pedido"
          style={{ width: '100%', padding: '10px', marginBottom: '16px', border: '1px solid var(--color-border)', borderRadius: '8px', fontSize: '15px', fontFamily: 'inherit' }}
        />

        {error && <p style={{ color: 'var(--color-danger, #c0392b)', fontSize: '14px' }}>{error}</p>}

        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <Link to="/carrito" style={{ padding: '12px 22px', borderRadius: '8px', border: '1px solid var(--color-border)', color: 'var(--color-text)', textDecoration: 'none', fontWeight: 600 }}>
            ← Volver al carrito
          </Link>
          <button
            type="submit"
            disabled={enviando}
            style={{
              padding: '12px 26px', borderRadius: '8px', border: 'none',
              cursor: enviando ? 'default' : 'pointer', opacity: enviando ? 0.6 : 1,
              background: 'var(--color-lime)', color: '#16181d', fontWeight: 700, fontSize: '16px'
            }}
          >
            {enviando ? 'Confirmando...' : 'Confirmar pedido'}
          </button>
        </div>
        <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '10px', marginBottom: 0 }}>
          Al confirmar te abrimos WhatsApp con el pedido cargado y te mandamos un email con el detalle.
        </p>
      </form>
    </div>
  )
}

export default CheckoutPage
