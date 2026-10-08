import { useState, useEffect, useCallback, Fragment } from 'react'
import axios from 'axios'
import { Link } from 'react-router-dom'
import { textoDemora } from '../../utils/demora'
import { ESTADOS, ESTADO_LABEL, EstadoChip } from '../../components/EstadoPedido'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatFechaHora = (iso) =>
  iso ? new Date(iso).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—'

function PedidosPage() {
  const [pedidos, setPedidos] = useState([])
  const [filtro, setFiltro] = useState('PENDIENTE')   // arranca en la bandeja que hay que atender
  const [cargando, setCargando] = useState(true)
  const [expandido, setExpandido] = useState(null)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [q, setQ] = useState('')   // la búsqueda aplicada (espera a que se deje de tipear)
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [medio, setMedio] = useState('')
  const [origen, setOrigen] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setQ(busqueda.trim()), 350)
    return () => clearTimeout(t)
  }, [busqueda])

  const cargar = useCallback(() => {
    setCargando(true)
    const params = { estado: filtro || undefined, q: q || undefined, desde: desde || undefined, hasta: hasta || undefined, medio: medio || undefined, origen: origen || undefined }
    axios.get('/api/admin/pedidos', { params })
      .then(res => { setPedidos(res.data); setError('') })
      .catch(() => setError('No se pudieron cargar los pedidos.'))
      .finally(() => setCargando(false))
  }, [filtro, q, desde, hasta, medio, origen])

  const hayFiltrosExtra = busqueda || desde || hasta || medio || origen
  const limpiarFiltros = () => { setBusqueda(''); setQ(''); setDesde(''); setHasta(''); setMedio(''); setOrigen('') }
  const campo = { padding: '8px 10px', fontSize: '13px', borderRadius: '2px', border: '1px solid var(--color-border)', background: 'var(--color-surface-2)', color: 'var(--color-text)' }

  useEffect(() => { cargar() }, [cargar])

  const cambiarEstado = async (id, estado) => {
    try {
      await axios.put(`/api/admin/pedidos/${id}/estado`, { estado })
      cargar()
    } catch (err) {
      alert(err.response?.data?.error || 'No se pudo cambiar el estado.')
    }
  }

  // Un pedido vencido se retoma con los precios que aceptó el cliente y queda sin vencimiento,
  // pendiente hasta que se entregue y se cobre (el backend le saca el vencimiento).
  const reactivar = async (p) => {
    if (!window.confirm(`¿Reactivar el pedido ${p.numero}?\n\nQueda PENDIENTE con los precios que aceptó el cliente ese día y ya no vence solo. Después lo marcás como cobrado y entregado.`)) return
    await cambiarEstado(p.id, 'PENDIENTE')
  }

  const marcarCobrado = async (id) => {
    try {
      await axios.put(`/api/admin/pedidos/${id}/pago`, { estado: 'APROBADO' })
      cargar()
    } catch (err) {
      alert(err.response?.data?.error || 'No se pudo registrar el cobro.')
    }
  }

  const botonFiltro = (valor, texto) => (
    <button
      key={valor || 'todos'}
      type="button"
      onClick={() => setFiltro(valor)}
      style={{
        padding: '7px 14px', borderRadius: '2px', cursor: 'pointer', fontSize: '13px', fontWeight: 600,
        border: '1px solid var(--color-border)',
        background: filtro === valor ? 'var(--color-accion)' : 'transparent',
        color: filtro === valor ? 'var(--color-sobre-accion)' : 'var(--color-text)'
      }}
    >
      {texto}
    </button>
  )

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
        <h1 style={{ margin: 0 }}>Pedidos</h1>
        <Link to="/admin/pedidos/nuevo" className="btn btn-primary" style={{ textDecoration: 'none' }}>+ Nueva orden manual</Link>
      </div>

      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', margin: '16px 0 12px', alignItems: 'center' }}>
        <input type="search" placeholder="Buscar por número, cliente, teléfono, email o artículo" value={busqueda}
          onChange={e => setBusqueda(e.target.value)} style={{ ...campo, flex: '1 1 280px', minWidth: '220px' }} />
        <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Desde <input type="date" value={desde} onChange={e => setDesde(e.target.value)} style={campo} /></label>
        <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Hasta <input type="date" value={hasta} onChange={e => setHasta(e.target.value)} style={campo} /></label>
        <select value={medio} onChange={e => setMedio(e.target.value)} style={campo} aria-label="Medio de pago">
          <option value="">Todos los pagos</option>
          <option value="MERCADO_PAGO">Mercado Pago</option>
          <option value="TRANSFERENCIA">Transferencia</option>
          <option value="EFECTIVO">Efectivo</option>
        </select>
        <select value={origen} onChange={e => setOrigen(e.target.value)} style={campo} aria-label="Origen">
          <option value="">Web y manuales</option>
          <option value="WEB">Solo web</option>
          <option value="MANUAL">Solo manuales</option>
        </select>
        {hayFiltrosExtra && <button type="button" onClick={limpiarFiltros} style={{ ...campo, cursor: 'pointer' }}>Limpiar</button>}
      </div>

      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '18px' }}>
        {botonFiltro('', 'Todos')}
        {ESTADOS.map(e => botonFiltro(e, ESTADO_LABEL[e]))}
      </div>

      {error && <div className="card" style={{ color: 'var(--color-danger, #c0392b)' }}>{error}</div>}

      {cargando ? (
        <div className="card"><p>Cargando...</p></div>
      ) : pedidos.length === 0 ? (
        <div className="card"><p style={{ color: 'var(--color-text-muted)' }}>No hay pedidos {filtro ? `en estado ${ESTADO_LABEL[filtro].toLowerCase()}` : ''}.</p></div>
      ) : (
        <div className="card">
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '760px' }}>
              <thead>
                <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--color-border)' }}>
                  <th style={{ padding: '10px 8px', fontSize: '13px' }}>Pedido</th>
                  <th style={{ padding: '10px 8px', fontSize: '13px' }}>Cliente</th>
                  <th style={{ padding: '10px 8px', fontSize: '13px' }}>Fecha</th>
                  <th style={{ padding: '10px 8px', fontSize: '13px' }}>Estado</th>
                  <th style={{ padding: '10px 8px', fontSize: '13px' }}>Pago</th>
                  <th style={{ padding: '10px 8px', fontSize: '13px', textAlign: 'right' }}>Total</th>
                  <th style={{ padding: '10px 8px', fontSize: '13px' }}>Cambiar a</th>
                </tr>
              </thead>
              <tbody>
                {pedidos.map(p => (
                  <Fragment key={p.numero}>
                    <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                      <td style={{ padding: '12px 8px' }}>
                        <button
                          type="button"
                          onClick={() => setExpandido(expandido === p.numero ? null : p.numero)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, fontWeight: 600, color: 'var(--color-accent)' }}
                        >
                          {p.numero} {expandido === p.numero ? '▾' : '▸'}
                        </button>
                        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                          {p.items.length} art.
                          {p.origen === 'MANUAL' && <span style={{ marginLeft: '6px', padding: '1px 6px', borderRadius: '2px', background: 'var(--color-accent-light)', color: 'var(--color-accent)', fontWeight: 700 }}>Manual</span>}
                        </div>
                        <Link to={`/admin/pedidos/${p.numero}/comprobante`} style={{ fontSize: '12px' }}>Comprobante</Link>
                      </td>
                      <td style={{ padding: '12px 8px', fontSize: '14px' }}>
                        <div>{p.nombreContacto || '—'}</div>
                        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{p.telefonoContacto || p.usuarioEmail || p.emailContacto}</div>
                      </td>
                      <td style={{ padding: '12px 8px', fontSize: '13px' }}>{formatFechaHora(p.createdAt)}</td>
                      <td style={{ padding: '12px 8px' }}>
                        <EstadoChip estado={p.estado} />
                        {p.estado === 'VENCIDO' && (
                          <button type="button" onClick={() => reactivar(p)} style={{ display: 'block', marginTop: '6px', padding: '5px 8px', border: 'none', borderRadius: '2px', background: 'var(--color-accion)', color: 'var(--color-sobre-accion)', cursor: 'pointer', fontWeight: 700, fontSize: '12px' }}>Reactivar</button>
                        )}
                      </td>
                      <td style={{ padding: '12px 8px', fontSize: '13px', fontWeight: 700, color: p.estadoPago === 'APROBADO' ? 'var(--color-accent)' : 'var(--color-text-muted)' }}>
                        <div>{p.estadoPago === 'APROBADO' ? 'Aprobado' : p.estadoPago === 'EN_PROCESO' ? 'En revisión' : p.estadoPago === 'RECHAZADO' ? 'Rechazado' : p.estadoPago === 'SIN_INICIAR' ? 'Sin iniciar' : 'Pendiente'}</div>
                        {p.estadoPago !== 'APROBADO' && p.estado !== 'CANCELADO' && p.estado !== 'VENCIDO' && (p.medioPago !== 'MERCADO_PAGO' || p.origen === 'MANUAL' || !p.venceEn) && (
                          <button type="button" onClick={() => marcarCobrado(p.id)} style={{ marginTop: '6px', padding: '5px 8px', border: '1px solid var(--color-accion)', borderRadius: '2px', background: 'transparent', color: 'var(--color-accion)', cursor: 'pointer', fontWeight: 700, fontSize: '12px' }}>Marcar cobrado</button>
                        )}
                      </td>
                      <td style={{ padding: '12px 8px', textAlign: 'right' }}>
                        <div style={{ fontWeight: 600 }}>US$ {formatNumber(p.totalUsd)}</div>
                        <div style={{ fontSize: '12px', color: 'var(--color-price)' }}>$ {formatNumber(p.totalArs)}</div>
                      </td>
                      <td style={{ padding: '12px 8px' }}>
                        <select
                          value=""
                          onChange={e => e.target.value && cambiarEstado(p.id, e.target.value)}
                          style={{
                            padding: '6px 8px', fontSize: '13px', borderRadius: '2px',
                            border: '1px solid var(--color-border)',
                            background: 'var(--color-surface-2)', color: 'var(--color-text)'
                          }}
                        >
                          <option value="">—</option>
                          {ESTADOS.filter(e => e !== p.estado).map(e => (
                            <option key={e} value={e}>{ESTADO_LABEL[e]}</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                    {expandido === p.numero && (
                      <tr>
                        <td colSpan={7} style={{ padding: '0 8px 14px', background: 'var(--color-accent-light)' }}>
                          {p.items.map(i => (
                            <div key={i.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: '14px' }}>
                              <span>
                                {i.cantidad}× {i.productoNombre}
                                {i.especificaciones && <span style={{ color: 'var(--color-text-muted)' }}> · {i.especificaciones}</span>}
                                {textoDemora(i) && <strong> · Entrega en {textoDemora(i)}</strong>}
                                {i.sku && <span style={{ color: 'var(--color-text-muted)' }}> · {i.sku}</span>}
                              </span>
                              <span style={{ fontWeight: 600, textAlign: 'right' }}>
                                US$ {formatNumber(i.subtotalUsd)}
                                {i.precioCatalogoUsd != null && (
                                  <div style={{ fontSize: '11px', fontWeight: 500, color: 'var(--color-text-muted)' }}>
                                    precio acordado · catálogo US$ {formatNumber(i.precioCatalogoUsd)} c/u
                                  </div>
                                )}
                              </span>
                            </div>
                          ))}
                          {p.notas && (
                            <p style={{ margin: '8px 0 0', fontSize: '13px', fontStyle: 'italic', color: 'var(--color-text-muted)' }}>
                              Notas: {p.notas}
                            </p>
                          )}
                          <p style={{ margin: '8px 0 0', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                            Email: {p.usuarioEmail || p.emailContacto || '—'}{p.origen === 'MANUAL' && !p.usuarioEmail && ' (sin cuenta)'} · {p.medioPago === 'EFECTIVO' ? 'Efectivo' : p.medioPago === 'TRANSFERENCIA' ? 'Transferencia' : 'Mercado Pago'} · Dólar usado: ${formatNumber(p.cotizacionUsada)}
                            {p.mercadoPagoPaymentId && <> · ID de pago: {p.mercadoPagoPaymentId}</>}
                            {p.puntosCanjeados > 0 && <> · Canjeó {p.puntosCanjeados} punto(s)</>}
                          </p>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

export default PedidosPage
