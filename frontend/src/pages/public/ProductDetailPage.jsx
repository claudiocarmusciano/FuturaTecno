import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import axios from 'axios'
import { WHATSAPP_NUMBER, NOMBRE_NEGOCIO } from '../../config'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const nombreProducto = (p) =>
  [p.categoria, p.marca, p.modelo].filter(Boolean).join(' ')

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : null

// Fecha larga con día de la semana, ej: "miércoles 25 de junio". Se interpreta como fecha local.
const formatFechaLarga = (isoDate) => {
  if (!isoDate) return null
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
}

function ProductDetailPage() {
  const { id } = useParams()
  const [producto, setProducto] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [eta, setEta] = useState(null)

  useEffect(() => {
    setCargando(true)
    axios.get(`/api/productos/${id}`)
      .then(res => setProducto(res.data))
      .catch(err => {
        console.error(err)
        setError('No se encontró el producto.')
      })
      .finally(() => setCargando(false))
  }, [id])

  useEffect(() => {
    axios.get('/api/eta').then(res => setEta(res.data)).catch(err => console.error('ETA:', err))
  }, [])

  const volver = (
    <Link to="/" style={{ color: '#007bff', textDecoration: 'none', fontSize: '14px' }}>← Volver al catálogo</Link>
  )

  if (cargando) return (<div>{volver}<div className="card" style={{ marginTop: '16px' }}><p>Cargando...</p></div></div>)
  if (error || !producto) return (<div>{volver}<div className="card" style={{ marginTop: '16px', color: '#721c24' }}>{error || 'Producto no disponible.'}</div></div>)

  const nombre = nombreProducto(producto)
  const mensaje = `Hola ${NOMBRE_NEGOCIO}, me interesa el ${nombre} que vi en el catálogo. ¿Está disponible?`
  const waLink = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(mensaje)}`

  return (
    <div>
      {volver}

      <div className="card detalle-grid" style={{ marginTop: '16px' }}>
        {/* Imagen */}
        <div>
          {producto.imagenUrl ? (
            <img
              src={producto.imagenUrl}
              alt={nombre}
              style={{ width: '100%', height: '320px', objectFit: 'contain', background: '#fff', borderRadius: '6px' }}
              onError={(e) => { e.target.style.display = 'none' }}
            />
          ) : (
            <div style={{
              width: '100%', height: '320px', background: '#f0f0f0', borderRadius: '6px',
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#bbb'
            }}>Sin imagen</div>
          )}
        </div>

        {/* Info */}
        <div>
          {producto.categoria && (
            <span className="chip-categoria" style={{ marginBottom: '12px' }}>{producto.categoria}</span>
          )}
          <h1 style={{ margin: '8px 0 20px', fontSize: '30px' }}>{producto.marca} {producto.modelo}</h1>

          {producto.variantes.map(v => (
            <div key={v.id} style={{ borderTop: '1px solid var(--color-border)', padding: '16px 0' }}>
              {v.especificaciones && (
                <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>{v.especificaciones}</p>
              )}
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '14px' }}>
                <strong style={{ fontSize: '28px', color: 'var(--color-text)', letterSpacing: '-0.02em' }}>US$ {formatNumber(v.precioUsd)}</strong>
                <span style={{ color: 'var(--color-price)', fontSize: '17px' }}>$ {formatNumber(v.precioArs)}</span>
              </div>
            </div>
          ))}

          <p style={{ fontSize: '13px', color: 'var(--color-price)', margin: '12px 0 2px' }}>Stock sujeto a disponibilidad</p>
          {producto.ultimaActualizacion && (
            <p style={{ fontSize: '12px', color: '#888', margin: '0 0 2px' }}>
              Última actualización: {formatFecha(producto.ultimaActualizacion)}
            </p>
          )}
          <p style={{ fontSize: '12px', color: '#999', margin: '0 0 16px' }}>Las imágenes son meramente ilustrativas.</p>

          {/* Estimación de entrega (ETA) */}
          {eta?.fechaEntrega && (
            <div style={{
              background: 'var(--color-accent-light)', border: '1px solid var(--color-border)', borderRadius: '12px',
              padding: '14px 16px', marginBottom: '20px', fontSize: '14px', color: '#424245'
            }}>
              🚚 <strong>Entrega estimada:</strong> {formatFechaLarga(eta.fechaEntrega)}
              <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                Comprando hoy{eta.antesDeCorte ? '' : ` (después de las ${eta.horaCorte}:00 hs)`} · {eta.diasHabiles} días hábiles · no incluye fines de semana ni feriados.
              </div>
            </div>
          )}

          {/* Botón WhatsApp */}
          <a
            href={waLink}
            target="_blank"
            rel="noreferrer"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '10px',
              background: '#25D366', color: 'white', textDecoration: 'none',
              padding: '12px 24px', borderRadius: '8px', fontSize: '16px', fontWeight: 600,
              boxShadow: '0 2px 6px rgba(37,211,102,0.4)'
            }}
          >
            <span style={{ fontSize: '20px' }}>💬</span> Consultar por WhatsApp
          </a>
          <p style={{ fontSize: '12px', color: '#999', marginTop: '10px' }}>
            Te abrimos un chat con el producto ya identificado.
          </p>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
