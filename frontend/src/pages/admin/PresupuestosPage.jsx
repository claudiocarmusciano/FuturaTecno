import { Fragment, useEffect, useState } from 'react'
import axios from 'axios'
import { formatPesos } from '../../utils/minimoCompra'
import { imprimirPresupuesto } from '../../utils/imprimirPresupuesto'

const fecha = (value) => value ? new Date(value).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'

/** Presupuestos de "Armá tu PC" que guardaron los clientes registrados, para hacerles seguimiento. */
export default function PresupuestosPage() {
  const [lista, setLista] = useState(null)
  const [error, setError] = useState('')
  const [abierto, setAbierto] = useState(null)

  useEffect(() => {
    axios.get('/api/admin/presupuestos').then(r => setLista(r.data))
      .catch(() => setError('No se pudieron cargar los presupuestos.'))
  }, [])

  return <div>
    <h1>Presupuestos de clientes</h1>
    <p style={{ color: 'var(--color-text-muted)' }}>Lo que guardaron los clientes registrados en "Armá tu PC", con los precios del día en que lo guardaron.</p>
    {error && <div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div>}
    {!lista && !error && <div className="card">Cargando…</div>}
    {lista?.length === 0 && <div className="card">Todavía ningún cliente guardó un presupuesto.</div>}
    {lista?.length > 0 && (
      <div className="card" style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
          <thead><tr style={{ textAlign: 'left' }}><th>Fecha</th><th>Cliente</th><th>Contacto</th><th>Presupuesto</th><th style={{ textAlign: 'right' }}>Total</th><th></th></tr></thead>
          <tbody>
            {lista.map(p => (<Fragment key={p.id}>
              <tr style={{ borderTop: '1px solid var(--color-border)' }}>
                <td style={{ padding: '8px 6px', whiteSpace: 'nowrap' }}>{fecha(p.actualizado)}</td>
                <td style={{ padding: '8px 6px' }}>{p.cliente}</td>
                <td style={{ padding: '8px 6px', fontSize: '13px' }}>{p.email}{p.celular ? <><br />{p.celular}</> : null}</td>
                <td style={{ padding: '8px 6px' }}>
                  <button className="btn btn-secondary" style={{ padding: '4px 10px' }} onClick={() => setAbierto(abierto === p.id ? null : p.id)}>
                    {p.nombre} · {(p.resumen || []).length} comp.
                  </button>
                </td>
                <td style={{ padding: '8px 6px', textAlign: 'right', whiteSpace: 'nowrap' }}>$ {formatPesos(p.totalArs)}</td>
                <td style={{ padding: '8px 6px' }}>
                  <button className="btn btn-secondary" style={{ padding: '4px 10px' }} onClick={() => imprimirPresupuesto({ nombre: p.nombre, items: p.resumen || [], totalArs: p.totalArs, totalUsd: p.totalUsd, fecha: p.actualizado, cliente: p.cliente })}>Imprimir</button>
                </td>
              </tr>
              {abierto === p.id && (
                <tr><td colSpan={6} style={{ padding: '0 6px 10px' }}>
                  <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '13px', lineHeight: 1.5 }}>
                    {(p.resumen || []).map((i, n) => <li key={n}>{i.titulo}: {i.marca} {i.modelo}{i.cantidad > 1 ? ` (x${i.cantidad})` : ''} — $ {formatPesos(i.precioArs * i.cantidad)}</li>)}
                  </ul>
                </td></tr>
              )}
            </Fragment>))}
          </tbody>
        </table>
      </div>
    )}
  </div>
}
