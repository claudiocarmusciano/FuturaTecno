import { useState, useEffect } from 'react'
import axios from 'axios'
import { IconEdit } from '../../components/icons'

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// Calcula el precio de venta (USD y ARS) a partir del costo, su moneda, y los datos del proveedor.
const calcularVenta = (precio, moneda, ed) => {
  const p = Number(precio)
  const cot = Number(ed?.cotizacion)
  if (!p || !cot) return { usd: null, ars: null }
  const costoUsd = moneda === 'USD' ? p : p / cot
  const factorFlete = 1 + (Number(ed?.fletePorcentaje) || 0) / 100
  const factorMargen = 1 + (Number(ed?.margenPorcentaje) || 0) / 100
  const ventaUsd = costoUsd * factorFlete * factorMargen
  return { usd: ventaUsd, ars: ventaUsd * cot }
}

const inputStyle = {
  width: '100%', padding: '6px 8px', border: '1px solid #ccc',
  borderRadius: '4px', fontSize: '13px', color: '#1a1a1a'
}

function ProductosPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [editData, setEditData] = useState(null)   // ProductoEditDTO en edición
  const [guardando, setGuardando] = useState(false)
  const [mensaje, setMensaje] = useState('')

  const cargar = async () => {
    setCargando(true)
    try {
      const res = await axios.get('/api/admin/productos')
      setProductos(res.data)
    } catch (e) {
      console.error(e)
      setMensaje('Error al cargar productos.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => { cargar() }, [])

  const abrirEdicion = async (id) => {
    setMensaje('')
    try {
      const res = await axios.get(`/api/admin/productos/${id}/editar`)
      setEditData(res.data)
    } catch (e) {
      console.error(e)
      setMensaje('Error al abrir el producto.')
    }
  }

  const setCampo = (campo, valor) => setEditData(prev => ({ ...prev, [campo]: valor }))

  const setVariante = (idx, campo, valor) => {
    setEditData(prev => ({
      ...prev,
      variantes: prev.variantes.map((v, i) => i === idx ? { ...v, [campo]: valor } : v)
    }))
  }

  const guardar = async () => {
    setGuardando(true)
    setMensaje('')
    try {
      await axios.put(`/api/admin/productos/${editData.id}`, editData)
      setEditData(null)
      await cargar()
      setMensaje('Producto actualizado ✓')
    } catch (e) {
      console.error(e)
      setMensaje('Error al guardar: ' + (e.response?.data?.message || e.message))
    } finally {
      setGuardando(false)
    }
  }

  const nombre = (p) => [p.categoria, p.marca, p.modelo].filter(Boolean).join(' ')

  return (
    <div>
      <h1>Productos</h1>
      {mensaje && <div className="card" style={{ borderLeft: '4px solid #28a745', color: '#155724' }}>{mensaje}</div>}

      {/* Panel de edición (modal) */}
      {editData && (
        <div
          onClick={() => setEditData(null)}
          style={{
            position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 1000,
            display: 'flex', alignItems: 'flex-start', justifyContent: 'center', padding: '20px', overflowY: 'auto'
          }}
        >
        <div
          className="card"
          onClick={(e) => e.stopPropagation()}
          style={{ borderLeft: '4px solid #007bff', maxWidth: '820px', width: '100%', marginTop: '30px', maxHeight: '88vh', overflowY: 'auto' }}
        >
          <h2 style={{ marginBottom: '4px' }}>Editar producto</h2>
          <p style={{ fontSize: '12px', color: '#888', marginBottom: '16px' }}>
            Proveedor: {editData.proveedor || '—'} (no editable)
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', marginBottom: '18px' }}>
            <div>
              <label style={{ fontSize: '12px', color: '#555' }}>Categoría</label>
              <input style={inputStyle} value={editData.categoria || ''} onChange={e => setCampo('categoria', e.target.value)} />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: '#555' }}>Marca</label>
              <input style={inputStyle} value={editData.marca || ''} onChange={e => setCampo('marca', e.target.value)} />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: '#555' }}>Modelo</label>
              <input style={inputStyle} value={editData.modelo || ''} onChange={e => setCampo('modelo', e.target.value)} />
            </div>
          </div>

          <h3 style={{ fontSize: '15px', marginBottom: '4px' }}>Variantes / Precios</h3>
          <p style={{ fontSize: '12px', color: '#888', marginBottom: '8px' }}>
            El precio que cargás es el <strong>costo</strong>. La venta se calcula con flete {Number(editData.fletePorcentaje) || 0}% + margen {Number(editData.margenPorcentaje) || 0}% · dólar ${formatNumber(editData.cotizacion)}.
          </p>
          <table className="table">
            <thead>
              <tr>
                <th>Especificaciones</th>
                <th style={{ width: '80px' }}>Moneda</th>
                <th style={{ width: '120px' }}>Costo</th>
                <th style={{ width: '80px' }}>Stock</th>
                <th style={{ whiteSpace: 'nowrap' }}>= Venta USD</th>
                <th style={{ whiteSpace: 'nowrap' }}>= Venta ARS</th>
              </tr>
            </thead>
            <tbody>
              {editData.variantes.map((v, idx) => {
                const venta = calcularVenta(v.precio, v.moneda, editData)
                return (
                  <tr key={v.id}>
                    <td><input style={inputStyle} value={v.especificaciones || ''} onChange={e => setVariante(idx, 'especificaciones', e.target.value)} /></td>
                    <td>
                      <select style={inputStyle} value={v.moneda} onChange={e => setVariante(idx, 'moneda', e.target.value)}>
                        <option value="USD">USD</option>
                        <option value="ARS">ARS</option>
                      </select>
                    </td>
                    <td><input style={inputStyle} type="number" step="0.01" value={v.precio ?? ''} onChange={e => setVariante(idx, 'precio', e.target.value)} /></td>
                    <td><input style={inputStyle} type="number" value={v.stock ?? 0} onChange={e => setVariante(idx, 'stock', e.target.value)} /></td>
                    <td style={{ whiteSpace: 'nowrap', color: '#1a1a1a' }}>{venta.usd != null ? `US$ ${formatNumber(venta.usd)}` : '-'}</td>
                    <td style={{ whiteSpace: 'nowrap', color: '#28a745' }}>{venta.ars != null ? `$ ${formatNumber(venta.ars)}` : '-'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>

          <div style={{ marginTop: '14px' }}>
            <button onClick={guardar} className="btn btn-primary" disabled={guardando} style={{ marginRight: '10px' }}>
              {guardando ? 'Guardando...' : 'Guardar cambios'}
            </button>
            <button onClick={() => setEditData(null)} className="btn btn-secondary">Cancelar</button>
          </div>
        </div>
        </div>
      )}

      {/* Lista de productos */}
      <div className="card">
        {cargando ? (
          <p>Cargando...</p>
        ) : productos.length === 0 ? (
          <p>No hay productos. Importá una lista en "Parsing IA".</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: '50px' }}></th>
                <th>Producto</th>
                <th>Proveedor</th>
                <th>Últ. actualización</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {productos.map(p => (
                <tr key={p.id}>
                  <td>
                    {p.imagenUrl
                      ? <img src={p.imagenUrl} alt="" style={{ width: '40px', height: '40px', objectFit: 'contain' }} onError={e => { e.target.style.opacity = '0.2' }} />
                      : <span style={{ color: '#bbb', fontSize: '12px' }}>—</span>}
                  </td>
                  <td>{nombre(p)}</td>
                  <td style={{ color: '#888', fontSize: '13px' }}>{p.proveedor}</td>
                  <td style={{ color: '#999', fontSize: '12px' }}>{formatFecha(p.ultimaActualizacion)}</td>
                  <td>
                    <button onClick={() => abrirEdicion(p.id)} className="btn-accion"><IconEdit /> Editar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default ProductosPage
