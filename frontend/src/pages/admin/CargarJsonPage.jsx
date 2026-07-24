import { useState, useEffect } from 'react'
import axios from 'axios'

const inputStyle = {
  padding: '9px 12px', fontSize: '14px', border: '1px solid var(--color-border)',
  borderRadius: '8px', color: 'var(--color-text)', background: '#fff', width: '100%'
}

const EJEMPLO = `{
  "marca": "ASUS",
  "modelo_exacto": "ASUS Vivobook Go 15 E510KA-BQ1203W, Intel Celeron N4500, 4GB RAM, 128GB eMMC, 15.6\\" FHD, Windows 11 Home",
  "especificaciones": { "procesador": "Intel Celeron N4500", "ram": "4GB DDR4", "almacenamiento": "128GB eMMC", "pantalla": "15.6\\" FHD" },
  "precio_usd": 320,
  "imagenes": ["https://..."],
  "categoria": "Notebooks > Consumo"
}`

function CargarJsonPage() {
  const [proveedores, setProveedores] = useState([])
  const [proveedorId, setProveedorId] = useState('')
  const [crearNuevo, setCrearNuevo] = useState(false)
  const [nuevoProv, setNuevoProv] = useState({ nombre: '', codigo: '', margenPorcentaje: '15', fletePorcentaje: '5' })

  const [json, setJson] = useState('')
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const cargarProveedores = () => {
    axios.get('/api/admin/proveedores')
      .then(res => setProveedores(res.data))
      .catch(() => setError('No se pudieron cargar los proveedores.'))
  }
  useEffect(cargarProveedores, [])

  const crearProveedor = async () => {
    setError('')
    if (!nuevoProv.nombre.trim()) { setError('El proveedor necesita un nombre.'); return }
    try {
      const res = await axios.post('/api/admin/proveedores', {
        nombre: nuevoProv.nombre.trim(),
        codigo: nuevoProv.codigo.trim(),
        margenPorcentaje: Number(nuevoProv.margenPorcentaje) || 0,
        fletePorcentaje: Number(nuevoProv.fletePorcentaje) || 0
      })
      setProveedores(prev => [...prev.filter(p => p.id !== res.data.id), res.data])
      setProveedorId(String(res.data.id))
      setCrearNuevo(false)
      setNuevoProv({ nombre: '', codigo: '', margenPorcentaje: '15', fletePorcentaje: '5' })
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data || 'No se pudo crear el proveedor.')
    }
  }

  const cargar = async () => {
    setError(''); setResultado(null)
    if (!proveedorId) { setError('Elegí un proveedor.'); return }
    let articulos
    try {
      const parsed = JSON.parse(json)
      articulos = Array.isArray(parsed) ? parsed : [parsed]   // acepta 1 objeto o un array
    } catch {
      setError('El JSON no es válido. Revisá que esté bien formado.')
      return
    }
    if (articulos.length === 0) { setError('El JSON no tiene artículos.'); return }
    setCargando(true)
    try {
      const res = await axios.post('/api/admin/carga-json', { proveedorId: Number(proveedorId), articulos })
      setResultado(res.data)
    } catch (err) {
      setError(err.response?.data?.error || 'Error al cargar los artículos.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: '6px' }}>Cargar artículos por JSON</h1>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: '20px' }}>
        Pegá un JSON (un objeto o un array) con los productos. Se crean bajo el proveedor elegido,
        con imágenes, y se intenta asignar la categoría automáticamente (si no se puede, queda para asignar a mano).
      </p>

      {error && (
        <div style={{ background: '#fff1f0', color: '#d70015', border: '1px solid #ffd9d6', padding: '11px 14px', borderRadius: '10px', marginBottom: '16px', fontSize: '14px' }}>
          {typeof error === 'string' ? error : 'Ocurrió un error.'}
        </div>
      )}

      {/* Proveedor */}
      <div className="card" style={{ marginBottom: '18px' }}>
        <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '8px' }}>1. Proveedor</div>
        {!crearNuevo ? (
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
            <select value={proveedorId} onChange={e => setProveedorId(e.target.value)} style={{ ...inputStyle, maxWidth: '320px' }}>
              <option value="">— Elegí un proveedor —</option>
              {proveedores.map(p => <option key={p.id} value={p.id}>{p.nombre}{p.codigo ? ` (${p.codigo})` : ''}</option>)}
            </select>
            <button className="btn btn-secondary" onClick={() => setCrearNuevo(true)}>+ Nuevo proveedor</button>
          </div>
        ) : (
          <div style={{ display: 'grid', gap: '10px', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', alignItems: 'end' }}>
            <div><label style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>Nombre *</label>
              <input style={inputStyle} value={nuevoProv.nombre} onChange={e => setNuevoProv({ ...nuevoProv, nombre: e.target.value })} /></div>
            <div><label style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>Código</label>
              <input style={inputStyle} value={nuevoProv.codigo} onChange={e => setNuevoProv({ ...nuevoProv, codigo: e.target.value })} placeholder="auto" /></div>
            <div><label style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>Margen %</label>
              <input type="number" style={inputStyle} value={nuevoProv.margenPorcentaje} onChange={e => setNuevoProv({ ...nuevoProv, margenPorcentaje: e.target.value })} /></div>
            <div><label style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>Flete %</label>
              <input type="number" style={inputStyle} value={nuevoProv.fletePorcentaje} onChange={e => setNuevoProv({ ...nuevoProv, fletePorcentaje: e.target.value })} /></div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button className="btn btn-primary" onClick={crearProveedor}>Crear</button>
              <button className="btn btn-secondary" onClick={() => setCrearNuevo(false)}>Cancelar</button>
            </div>
          </div>
        )}
      </div>

      {/* JSON */}
      <div className="card" style={{ marginBottom: '18px' }}>
        <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '8px' }}>2. JSON de artículos</div>
        <textarea
          value={json}
          onChange={e => setJson(e.target.value)}
          placeholder={EJEMPLO}
          spellCheck={false}
          style={{ ...inputStyle, minHeight: '260px', fontFamily: 'monospace', fontSize: '13px', whiteSpace: 'pre' }}
        />
        <div style={{ marginTop: '12px', display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button className="btn btn-primary" onClick={cargar} disabled={cargando}>
            {cargando ? 'Cargando...' : 'Cargar artículos'}
          </button>
          <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
            Tip: agregá <code>"categoria": "Notebooks &gt; Consumo"</code> para que se clasifique solo.
          </span>
        </div>
      </div>

      {/* Resultado */}
      {resultado && (
        <div className="card">
          <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '10px' }}>{resultado.mensaje}</div>
          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '14px', fontSize: '13px' }}>
            <span>✅ Creados: <strong>{resultado.creados}</strong></span>
            <span>♻️ Actualizados: <strong>{resultado.actualizados}</strong></span>
            <span>⏭️ Omitidos: <strong>{resultado.omitidos}</strong></span>
            <span>🏷️ Sin categoría: <strong>{resultado.sinCategoria}</strong></span>
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table className="table" style={{ fontSize: '13px' }}>
              <thead><tr><th>Producto</th><th>Estado</th><th>Categoría</th></tr></thead>
              <tbody>
                {resultado.items.map((it, i) => (
                  <tr key={i}>
                    <td>{it.producto}</td>
                    <td>{it.estado === 'omitido' ? `omitido — ${it.motivo}` : it.estado}</td>
                    <td>{it.categoria || (it.estado === 'omitido' ? '—' : <span style={{ color: '#b45309' }}>sin categoría (asignar a mano)</span>)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

export default CargarJsonPage
