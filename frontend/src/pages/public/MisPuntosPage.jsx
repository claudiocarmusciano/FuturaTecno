import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

const fecha = (value) => value ? new Date(value).toLocaleDateString('es-AR', { day: '2-digit', month: 'long', year: 'numeric' }) : '—'

export default function MisPuntosPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get('/api/puntos/mis-puntos').then(r => setData(r.data))
      .catch(e => setError(e.response?.data?.error || 'No se pudo consultar tu saldo.'))
  }, [])

  if (error) return <div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div>
  if (!data) return <div className="card">Cargando tus puntos…</div>

  return <div style={{ maxWidth: '780px', margin: '0 auto' }}>
    <h1>Mis puntos</h1>
    <div className="card" style={{ border: '1px solid var(--color-lime)', background: 'linear-gradient(135deg, rgba(200,224,72,.13), transparent)' }}>
      <div style={{ color: 'var(--color-text-muted)', fontSize: '14px' }}>Saldo disponible</div>
      <div style={{ fontSize: '44px', lineHeight: 1.1, fontWeight: 800, color: 'var(--color-lime)', marginTop: '5px' }}>{data.puntosDisponibles} puntos</div>
      <p style={{ margin: '9px 0 0', fontSize: '15px' }}>Equivalen a <strong>US$ {data.puntosDisponibles}</strong> para descontar en una próxima compra.</p>
      {data.proximoVencimiento && <p style={{ margin: '7px 0 0', color: 'var(--color-text-muted)', fontSize: '13px' }}>El próximo vencimiento es el {fecha(data.proximoVencimiento)}.</p>}
      <Link to="/catalogo" className="btn-primario" style={{ display: 'inline-block', marginTop: '16px', textDecoration: 'none' }}>Ver catálogo</Link>
    </div>
    <div className="card">
      <h2 style={{ marginTop: 0, fontSize: '18px' }}>Cómo funciona</h2>
      <p style={{ color: 'var(--color-text-muted)', lineHeight: 1.55 }}>Por cada US$ 100 en productos de una compra cobrada, sumás 1 punto. Cada punto equivale a US$ 1 de descuento, vence a los 12 meses y se usa en el checkout. La compra mínima de US$ 250 se calcula antes del descuento.</p>
    </div>
    <div className="card">
      <h2 style={{ marginTop: 0, fontSize: '18px' }}>Movimientos</h2>
      {data.movimientos.length === 0 ? <p style={{ color: 'var(--color-text-muted)' }}>Todavía no tenés movimientos.</p> : data.movimientos.map((m, index) => (
        <div key={index} style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', padding: '12px 0', borderBottom: '1px solid var(--color-border)' }}>
          <div><strong>{m.detalle}</strong><div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '3px' }}>{fecha(m.fecha)}{m.venceEn ? ` · vence ${fecha(m.venceEn)}` : ''}</div></div>
          <strong style={{ whiteSpace: 'nowrap', color: m.puntos > 0 ? 'var(--color-lime)' : 'var(--color-text)' }}>{m.puntos > 0 ? '+' : ''}{m.puntos}</strong>
        </div>
      ))}
    </div>
  </div>
}
