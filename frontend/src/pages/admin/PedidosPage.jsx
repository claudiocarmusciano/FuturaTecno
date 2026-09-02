import { useState, useEffect, useCallback, Fragment } from 'react'
import axios from 'axios'
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

  const cargar = useCallback(() => {
    setCargando(true)
    const url = filtro ? `/api/admin/pedidos?estado=${filtro}` : '/api/admin/pedidos'
    axios.get(url)
      .then(res => setPedidos(res.data))
      .catch(() => setError('No se pudieron cargar los pedidos.'))
      .finally(() => setCargando(false))
  }, [filtro])

  useEffect(() => { cargar() }, [cargar])

  const cambiarEstado = async (id, estado) => {
    try {
      await axios.put(`/api/admin/pedidos/${id}/estado`, { estado })
      cargar()
    } catch (err) {
      alert(err.response?.data?.error || 'No se pudo cambiar el estado.')
    }
  }

  const botonFiltro = (valor, texto) => (
    <button
      key={valor || 'todos'}
      type="button"
      onClick={() => setFiltro(valor)}
      style={{
        padding: '7px 14px', borderRadius: '999px', cursor: 'pointer', fontSize: '13px', fontWeight: 600,
        border: '1px solid var(--color-border)',
        background: filtro === valor ? 'var(--color-lime)' : 'transparent',
        color: filtro === valor ? '#16181d' : 'var(--color-text)'
      }}
    >
      {texto}
    </button>
  )

  return (
    <div>
      <h1>Pedidos</h1>

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
                        </div>
                      </td>
                      <td style={{ padding: '12px 8px', fontSize: '14px' }}>
                        <div>{p.nombreContacto || '—'}</div>
                        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{p.telefonoContacto || p.usuarioEmail}</div>
                      </td>
                      <td style={{ padding: '12px 8px', fontSize: '13px' }}>{formatFechaHora(p.createdAt)}</td>
                      <td style={{ padding: '12px 8px' }}><EstadoChip estado={p.estado} /></td>
                      <td style={{ padding: '12px 8px', fontSize: '13px', fontWeight: 700, color: p.estadoPago === 'APROBADO' ? 'var(--color-accent)' : 'var(--color-text-muted)' }}>
                        {p.estadoPago === 'APROBADO' ? 'Aprobado' : p.estadoPago === 'EN_PROCESO' ? 'En revisión' : p.estadoPago === 'RECHAZADO' ? 'Rechazado' : 'Pendiente'}
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
                            padding: '6px 8px', fontSize: '13px', borderRadius: '6px',
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
                                {i.sku && <span style={{ color: 'var(--color-text-muted)' }}> · {i.sku}</span>}
                              </span>
                              <span style={{ fontWeight: 600 }}>US$ {formatNumber(i.subtotalUsd)}</span>
                            </div>
                          ))}
                          {p.notas && (
                            <p style={{ margin: '8px 0 0', fontSize: '13px', fontStyle: 'italic', color: 'var(--color-text-muted)' }}>
                              Notas: {p.notas}
                            </p>
                          )}
                          <p style={{ margin: '8px 0 0', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                            Email: {p.usuarioEmail} · Dólar usado: ${formatNumber(p.cotizacionUsada)}
                            {p.mercadoPagoPaymentId && <> · ID de pago: {p.mercadoPagoPaymentId}</>}
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
