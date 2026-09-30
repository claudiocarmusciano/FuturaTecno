import { useState, useEffect, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { etiquetaEnvio } from '../../utils/envio'

const formatNumber = (n) =>
  Number(n || 0).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const MODOS_ENVIO = ['', 'retiro-local', 'entrega-local-olavarria', 'estándar', 'sucursal']

/**
 * Orden de venta manual: para el cliente que compra por WhatsApp o en el local y no entra a la
 * web. Cada artículo arranca con el precio del catálogo del día y se puede cambiar (queda
 * registrado). No exige compra mínima ni vence a las 06:30.
 */
export default function NuevaOrdenPage() {
  const navigate = useNavigate()
  const [cliente, setCliente] = useState({ email: '', nombre: '', telefono: '' })
  const [usuarios, setUsuarios] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [resultados, setResultados] = useState([])
  const [buscando, setBuscando] = useState(false)
  const [items, setItems] = useState([])   // {varianteId, nombre, detalle, precioCatalogoUsd, precioUsd, cantidad}
  const [medioPago, setMedioPago] = useState('EFECTIVO')
  const [modoEnvio, setModoEnvio] = useState('retiro-local')
  const [cp, setCp] = useState('')
  const [costoEnvio, setCostoEnvio] = useState('')
  const [notas, setNotas] = useState('')
  const [cotizacion, setCotizacion] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get('/api/admin/usuarios').then(r => setUsuarios(r.data || [])).catch(() => setUsuarios([]))
    axios.get('/api/cotizacion').then(r => setCotizacion(Number(r.data.valor))).catch(() => setCotizacion(null))
  }, [])

  // Cuenta existente con ese email: la orden queda en su historial y suma puntos.
  const cuenta = useMemo(() => {
    const e = cliente.email.trim().toLowerCase()
    return e ? usuarios.find(u => (u.email || '').toLowerCase() === e) : null
  }, [cliente.email, usuarios])

  useEffect(() => {
    if (cuenta && !cliente.nombre) {
      setCliente(c => ({ ...c, nombre: [cuenta.nombre, cuenta.apellido].filter(Boolean).join(' '), telefono: c.telefono || cuenta.celular || '' }))
    }
  }, [cuenta]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const q = busqueda.trim()
    if (q.length < 2) { setResultados([]); return }
    const t = setTimeout(() => {
      setBuscando(true)
      axios.get('/api/productos/buscar', { params: { q, size: 8, orden: 'relevancia' } })
        .then(r => setResultados(r.data.items || []))
        .catch(() => setResultados([]))
        .finally(() => setBuscando(false))
    }, 350)
    return () => clearTimeout(t)
  }, [busqueda])

  const agregar = (p, v) => {
    setItems(prev => {
      const i = prev.findIndex(x => x.varianteId === v.id)
      if (i >= 0) return prev.map((x, k) => k === i ? { ...x, cantidad: x.cantidad + 1 } : x)
      const precio = v.precioUsd != null ? Number(v.precioUsd) : null
      return [...prev, {
        varianteId: v.id,
        nombre: [p.marca, p.modelo].filter(Boolean).join(' '),
        detalle: v.especificaciones || '',
        precioCatalogoUsd: precio,
        precioUsd: precio != null ? String(precio) : '',
        cantidad: 1
      }]
    })
    setBusqueda('')
    setResultados([])
  }

  const cambiarItem = (id, campo, valor) => setItems(prev => prev.map(x => x.varianteId === id ? { ...x, [campo]: valor } : x))
  const quitar = (id) => setItems(prev => prev.filter(x => x.varianteId !== id))

  const totalUsd = items.reduce((s, i) => s + (Number(i.precioUsd) || 0) * (Number(i.cantidad) || 0), 0)
  const envioArs = modoEnvio === 'retiro-local' ? 0 : (costoEnvio === '' ? null : Number(costoEnvio))

  const confirmar = async (e) => {
    e.preventDefault()
    setError('')
    if (!cliente.nombre.trim()) { setError('Completá el nombre del cliente.'); return }
    if (items.length === 0) { setError('Agregá al menos un artículo.'); return }
    if (items.some(i => !(Number(i.precioUsd) > 0) || !(Number(i.cantidad) > 0))) {
      setError('Revisá precios y cantidades: tienen que ser mayores a cero.'); return
    }
    setEnviando(true)
    try {
      const { data } = await axios.post('/api/admin/pedidos', {
        email: cliente.email.trim() || null,
        nombre: cliente.nombre.trim(),
        telefono: cliente.telefono.trim() || null,
        medioPago,
        notas: notas.trim() || null,
        modoEnvio: modoEnvio || null,
        cpDestino: cp.trim() || null,
        costoEnvioArs: envioArs,
        items: items.map(i => ({
          varianteId: i.varianteId,
          cantidad: Number(i.cantidad),
          // Solo se manda si cambió: así el backend usa y congela el precio del catálogo del día.
          precioUnitarioUsd: i.precioCatalogoUsd != null && Number(i.precioUsd) === i.precioCatalogoUsd ? null : Number(i.precioUsd)
        }))
      })
      navigate(`/admin/pedidos/${data.numero}/comprobante`)
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo crear la orden.')
    } finally {
      setEnviando(false)
    }
  }

  const campo = { width: '100%', padding: '9px 11px', fontSize: '14px', borderRadius: '8px', border: '1px solid var(--color-border)', background: 'var(--color-surface-2)', color: 'var(--color-text)' }
  const etiqueta = { display: 'grid', gap: '5px', fontSize: '13px', fontWeight: 600 }

  return (
    <div>
      <Link to="/admin/pedidos" style={{ fontSize: '14px' }}>← Volver a pedidos</Link>
      <h1 style={{ marginTop: '10px' }}>Nueva orden manual</h1>
      <p style={{ color: 'var(--color-text-muted)', marginTop: '-6px' }}>
        Para el cliente que compra por WhatsApp o en el local. No exige compra mínima y no vence a las 6:30.
      </p>

      <form onSubmit={confirmar}>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Cliente</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '12px' }}>
            <label style={etiqueta}>Email <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}>(opcional)</span>
              <input type="email" value={cliente.email} onChange={e => setCliente({ ...cliente, email: e.target.value })} style={campo} />
            </label>
            <label style={etiqueta}>Nombre y apellido *
              <input value={cliente.nombre} onChange={e => setCliente({ ...cliente, nombre: e.target.value })} style={campo} required />
            </label>
            <label style={etiqueta}>Teléfono
              <input value={cliente.telefono} onChange={e => setCliente({ ...cliente, telefono: e.target.value })} style={campo} />
            </label>
          </div>
          <p style={{ fontSize: '13px', margin: '10px 0 0', color: cuenta ? 'var(--color-accent)' : 'var(--color-text-muted)' }}>
            {cuenta ? '✓ Tiene cuenta: la orden queda en su historial y suma puntos.'
              : cliente.email.trim() ? 'Sin cuenta con ese email: la orden queda con estos datos de contacto (no suma puntos).'
                : 'Sin email, la orden queda solo con nombre y teléfono.'}
          </p>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Artículos</h3>
          <div style={{ position: 'relative' }}>
            <input type="search" placeholder="Buscar en el catálogo por marca, modelo o característica" value={busqueda}
              onChange={e => setBusqueda(e.target.value)} style={campo} />
            {(resultados.length > 0 || buscando) && (
              <div style={{ position: 'absolute', zIndex: 5, left: 0, right: 0, marginTop: '4px', maxHeight: '340px', overflowY: 'auto', background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: '10px', boxShadow: '0 12px 30px rgba(0,0,0,.25)' }}>
                {buscando && <div style={{ padding: '10px 12px', fontSize: '13px', color: 'var(--color-text-muted)' }}>Buscando…</div>}
                {resultados.flatMap(p => (p.variantes || []).map(v => (
                  <button type="button" key={v.id} onClick={() => agregar(p, v)}
                    style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', width: '100%', textAlign: 'left', padding: '10px 12px', border: 0, borderBottom: '1px solid var(--color-border)', background: 'transparent', color: 'var(--color-text)', cursor: 'pointer' }}>
                    <span>
                      <span style={{ fontWeight: 600 }}>{[p.marca, p.modelo].filter(Boolean).join(' ')}</span>
                      {v.especificaciones && <span style={{ display: 'block', fontSize: '12px', color: 'var(--color-text-muted)' }}>{v.especificaciones}</span>}
                    </span>
                    <span style={{ whiteSpace: 'nowrap', fontWeight: 700 }}>{v.precioUsd != null ? `US$ ${formatNumber(v.precioUsd)}` : 'sin precio'}</span>
                  </button>
                )))}
              </div>
            )}
          </div>

          {items.length === 0 ? (
            <p style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>Todavía no agregaste artículos.</p>
          ) : (
            <div style={{ overflowX: 'auto', marginTop: '12px' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '620px' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--color-border)', fontSize: '13px' }}>
                    <th style={{ padding: '8px' }}>Artículo</th>
                    <th style={{ padding: '8px', width: '90px' }}>Cant.</th>
                    <th style={{ padding: '8px', width: '150px' }}>Precio unit. US$</th>
                    <th style={{ padding: '8px', textAlign: 'right' }}>Subtotal</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {items.map(i => {
                    const cambiado = i.precioCatalogoUsd != null && Number(i.precioUsd) !== i.precioCatalogoUsd
                    return (
                      <tr key={i.varianteId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td style={{ padding: '8px', fontSize: '14px' }}>
                          {i.nombre}
                          {i.detalle && <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{i.detalle}</div>}
                        </td>
                        <td style={{ padding: '8px' }}>
                          <input type="number" min="1" value={i.cantidad} onChange={e => cambiarItem(i.varianteId, 'cantidad', e.target.value)} style={campo} />
                        </td>
                        <td style={{ padding: '8px' }}>
                          <input type="number" min="0.01" step="0.01" value={i.precioUsd} onChange={e => cambiarItem(i.varianteId, 'precioUsd', e.target.value)} style={campo} />
                          {cambiado && <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '3px' }}>catálogo: US$ {formatNumber(i.precioCatalogoUsd)}</div>}
                        </td>
                        <td style={{ padding: '8px', textAlign: 'right', fontWeight: 600 }}>US$ {formatNumber((Number(i.precioUsd) || 0) * (Number(i.cantidad) || 0))}</td>
                        <td style={{ padding: '8px' }}>
                          <button type="button" onClick={() => quitar(i.varianteId)} aria-label="Quitar" style={{ border: 0, background: 'transparent', color: 'var(--color-text-muted)', cursor: 'pointer', fontSize: '18px' }}>×</button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Pago y entrega</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
            <label style={etiqueta}>Medio de pago
              <select value={medioPago} onChange={e => setMedioPago(e.target.value)} style={campo}>
                <option value="EFECTIVO">Efectivo</option>
                <option value="TRANSFERENCIA">Transferencia</option>
                <option value="MERCADO_PAGO">Mercado Pago (link o QR)</option>
              </select>
            </label>
            <label style={etiqueta}>Entrega
              <select value={modoEnvio} onChange={e => setModoEnvio(e.target.value)} style={campo}>
                {MODOS_ENVIO.map(m => <option key={m || 'nada'} value={m}>{m ? etiquetaEnvio(m) : 'A coordinar'}</option>)}
              </select>
            </label>
            {modoEnvio !== 'retiro-local' && modoEnvio !== '' && (
              <>
                <label style={etiqueta}>Código postal
                  <input value={cp} onChange={e => setCp(e.target.value)} style={campo} />
                </label>
                <label style={etiqueta}>Costo de envío $ <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}>(vacío = cotizar con Andreani)</span>
                  <input type="number" min="0" step="0.01" value={costoEnvio} onChange={e => setCostoEnvio(e.target.value)} style={campo} />
                </label>
              </>
            )}
          </div>
          <label style={{ ...etiqueta, marginTop: '12px' }}>Notas <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}>(aparecen en el comprobante)</span>
            <textarea rows={2} value={notas} onChange={e => setNotas(e.target.value)} style={campo} maxLength={1000} />
          </label>
        </div>

        <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <div>
            <div style={{ fontSize: '22px', fontWeight: 800 }}>Total US$ {formatNumber(totalUsd)}</div>
            {cotizacion && <div style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>≈ $ {formatNumber(totalUsd * cotizacion)} al dólar de hoy{envioArs ? ` + envío $ ${formatNumber(envioArs)}` : ''}</div>}
            {medioPago === 'EFECTIVO' && <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>En efectivo se descuenta el 7% sobre los productos al cobrar (lo muestra el comprobante).</div>}
            {medioPago === 'MERCADO_PAGO' && <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Con Mercado Pago se suma el costo de acreditación inmediata, igual que en la web.</div>}
          </div>
          <div style={{ display: 'grid', gap: '6px', justifyItems: 'end' }}>
            {error && <div style={{ color: 'var(--color-danger, #c0392b)', fontWeight: 600 }}>{error}</div>}
            <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Creando…' : 'Crear orden'}</button>
          </div>
        </div>
      </form>
    </div>
  )
}
