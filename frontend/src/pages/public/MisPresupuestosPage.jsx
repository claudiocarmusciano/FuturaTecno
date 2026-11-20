import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { formatPesos } from '../../utils/minimoCompra'
import { imprimirPresupuesto } from '../../utils/imprimirPresupuesto'

const fecha = (value) => value ? new Date(value).toLocaleDateString('es-AR', { day: '2-digit', month: 'long', year: 'numeric' }) : '—'

/** Presupuestos de "Armá tu PC" que guardó el cliente. Los precios son los del día en que se guardó. */
export default function MisPresupuestosPage() {
  const [lista, setLista] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get('/api/presupuestos').then(r => setLista(r.data))
      .catch(e => setError(e.response?.data?.error || 'No se pudieron cargar tus presupuestos.'))
  }, [])

  const borrar = async (p) => {
    if (!window.confirm(`¿Borrar el presupuesto "${p.nombre}"?`)) return
    try {
      await axios.delete(`/api/presupuestos/${p.id}`)
      setLista(prev => prev.filter(x => x.id !== p.id))
    } catch (e) {
      setError(e.response?.data?.error || 'No se pudo borrar el presupuesto.')
    }
  }

  const abrir = p => `/arma-tu-pc?armado=${encodeURIComponent(p.seleccion?.armado || '')}&presupuesto=${p.id}&nombre=${encodeURIComponent(p.nombre)}`

  if (!lista && !error) return <div className="card">Cargando tus presupuestos…</div>

  return <div style={{ maxWidth: '780px', margin: '0 auto' }}>
    <h1>Mis presupuestos</h1>
    {error && <div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div>}
    {lista && lista.length === 0 && (
      <div className="card">
        <p style={{ marginTop: 0 }}>Todavía no guardaste presupuestos.</p>
        <Link to="/arma-tu-pc" className="btn btn-primary">Armá tu PC</Link>
      </div>
    )}
    {lista?.map(p => (
      <div key={p.id} className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: '18px' }}>{p.nombre}</h2>
            <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '3px' }}>Guardado el {fecha(p.actualizado)} · precios de ese día</div>
          </div>
          <strong style={{ whiteSpace: 'nowrap' }}>$ {formatPesos(p.totalArs)}</strong>
        </div>
        <ul style={{ margin: '12px 0', paddingLeft: '18px', fontSize: '14px', lineHeight: 1.5 }}>
          {(p.resumen || []).map((i, n) => <li key={n}>{i.titulo}: {i.modelo}{i.cantidad > 1 ? ` (x${i.cantidad})` : ''}</li>)}
        </ul>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <Link to={abrir(p)} className="btn btn-primary">Abrir y editar</Link>
          <button className="btn btn-secondary" onClick={() => imprimirPresupuesto({ nombre: p.nombre, items: p.resumen || [], totalArs: p.totalArs, totalUsd: p.totalUsd, fecha: p.actualizado })}>Imprimir</button>
          <button className="btn btn-secondary" onClick={() => borrar(p)}>Borrar</button>
        </div>
      </div>
    ))}
  </div>
}
