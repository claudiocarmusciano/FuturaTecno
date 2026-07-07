import { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
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
  const navigate = useNavigate()
  const location = useLocation()
  const [producto, setProducto] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [eta, setEta] = useState(null)
  const [imagenActiva, setImagenActiva] = useState(0)

  useEffect(() => {
    setCargando(true)
    setImagenActiva(0)
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

  // Vuelve al catálogo conservando los filtros: si venimos de él, hace "atrás" en el historial
  // (restaura la URL con ?cat=&marca=... y la posición de scroll). Si se entró directo al
  // detalle (link compartido), va al catálogo normal.
  const volverAlCatalogo = (e) => {
    e.preventDefault()
    if (location.key !== 'default') navigate(-1)
    else navigate('/')
  }
  const volver = (
    <a href="/" onClick={volverAlCatalogo} style={{ color: 'var(--color-accent)', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>← Volver al catálogo</a>
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
          {(() => {
            const imagenes = producto.imagenes?.length ? producto.imagenes : (producto.imagenUrl ? [producto.imagenUrl] : [])
            const actual = imagenes[imagenActiva] ?? imagenes[0]
            return actual ? (
              <>
                <img
                  src={actual}
                  alt={nombre}
                  style={{ width: '100%', height: '320px', objectFit: 'contain', background: '#fff', borderRadius: '6px' }}
                  onError={(e) => { e.target.style.display = 'none' }}
                />
                {imagenes.length > 1 && (
                  <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                    {imagenes.map((url, i) => (
                      <button
                        key={url + i}
                        type="button"
                        onClick={() => setImagenActiva(i)}
                        style={{
                          width: '56px', height: '56px', padding: 0, background: '#fff', cursor: 'pointer',
                          border: `2px solid ${i === imagenActiva ? 'var(--color-accent)' : 'var(--color-border)'}`,
                          borderRadius: '6px', overflow: 'hidden'
                        }}
                      >
                        <img src={url} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                      </button>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <div style={{
                width: '100%', height: '320px', background: '#f0f0f0', borderRadius: '6px',
                display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#bbb'
              }}>Sin imagen</div>
            )
          })()}
        </div>

        {/* Info */}
        <div>
          {(producto.seccion || producto.categoriaPadre) && (
            <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px' }}>
              {[producto.seccion, producto.categoriaPadre].filter(Boolean).join(' › ')}
            </p>
          )}
          {producto.categoria && (
            <span className="chip-categoria" style={{ marginBottom: '12px' }}>{producto.categoria}</span>
          )}
          <h1 style={{ margin: '8px 0 4px', fontSize: '30px' }}>{producto.marca} {producto.modelo}</h1>
          {producto.sku && <p style={{ margin: '0 0 20px', fontSize: '12px', color: '#9ca3af' }}>Cód. {producto.sku}</p>}

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
