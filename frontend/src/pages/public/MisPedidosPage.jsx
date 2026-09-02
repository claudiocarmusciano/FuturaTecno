import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { ESTADO_LABEL, EstadoChip } from '../../components/EstadoPedido'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function MisPedidosPage() {
  const [pedidos, setPedidos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get('/api/pedidos/mis')
      .then(res => setPedidos(res.data))
      .catch(() => setError('No se pudieron cargar tus pedidos.'))
      .finally(() => setCargando(false))
  }, [])

  if (cargando) return (<div><h1>Mis pedidos</h1><div className="card"><p>Cargando...</p></div></div>)
  if (error) return (<div><h1>Mis pedidos</h1><div className="card" style={{ color: 'var(--color-danger, #c0392b)' }}>{error}</div></div>)

  if (pedidos.length === 0) {
    return (
      <div>
        <h1>Mis pedidos</h1>
        <div className="card" style={{ textAlign: 'center', padding: '46px 20px' }}>
          <p style={{ color: 'var(--color-text-muted)', marginBottom: '20px' }}>Todavía no hiciste ningún pedido.</p>
          <Link to="/catalogo" style={{ padding: '11px 22px', borderRadius: '8px', background: 'var(--color-lime)', color: '#16181d', fontWeight: 700, textDecoration: 'none' }}>
            Ver el catálogo
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div>
      <h1>Mis pedidos</h1>
      <div className="card">
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '520px' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--color-border)' }}>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)' }}>Pedido</th>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)' }}>Fecha</th>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)' }}>Estado</th>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)' }}>Pago</th>
                <th style={{ padding: '10px 8px', fontSize: '13px', color: 'var(--color-text-muted)', textAlign: 'right' }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {pedidos.map(p => (
                <tr key={p.numero} style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <td style={{ padding: '13px 8px' }}>
                    <Link to={`/pedido/${p.numero}`} style={{ fontWeight: 600, color: 'var(--color-accent)', textDecoration: 'none' }}>
                      {p.numero}
                    </Link>
                    <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                      {p.items.length} artículo{p.items.length !== 1 ? 's' : ''}
                    </div>
                  </td>
                  <td style={{ padding: '13px 8px', fontSize: '14px' }}>{formatFecha(p.createdAt)}</td>
                  <td style={{ padding: '13px 8px' }}><EstadoChip estado={p.estado} /></td>
                  <td style={{ padding: '13px 8px', fontSize: '13px', fontWeight: 600 }}>
                    {p.estadoPago === 'APROBADO' ? 'Aprobado' : p.estadoPago === 'EN_PROCESO' ? 'En revisión' : 'Pendiente'}
                  </td>
                  <td style={{ padding: '13px 8px', textAlign: 'right' }}>
                    <div style={{ fontWeight: 600 }}>US$ {formatNumber(p.totalUsd)}</div>
                    <div style={{ fontSize: '13px', color: 'var(--color-price)' }}>$ {formatNumber(p.totalArs)}</div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
        Los pedidos {ESTADO_LABEL.PENDIENTE.toLowerCase()}s vencen a las 6:30 de la mañana, cuando se actualizan los precios.
      </p>
    </div>
  )
}

export default MisPedidosPage
