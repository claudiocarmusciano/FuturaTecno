import { useState, useEffect } from 'react'
import axios from 'axios'
import { IconEdit } from '../../components/icons'
import { indexarArbol } from '../../utils/categorias'

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
  width: '100%', padding: '6px 8px', border: '1px solid var(--color-border)',
  borderRadius: '4px', fontSize: '13px', color: 'var(--color-text)'
}

function ProductosPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [editData, setEditData] = useState(null)   // ProductoEditDTO en edición
  const [guardando, setGuardando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [arbol, setArbol] = useState([])
  // topId = categoría de primer nivel elegida; subId = su subcategoría (si tiene). Algunas
  // categorías no tienen subcategorías (ej. "Tablets") y son hoja en sí mismas: ahí topId ES
  // el categoriaId a guardar, sin necesidad de elegir subId.
  const [catPath, setCatPath] = useState({ topId: '', subId: '' })
  const [clasificando, setClasificando] = useState(false)

  // Filtro "solo sin categoría" + selección múltiple + cascada para asignación masiva.
  const [soloSinCategoria, setSoloSinCategoria] = useState(false)
  const [seleccionados, setSeleccionados] = useState(new Set())
  const [catMasiva, setCatMasiva] = useState({ topId: '', subId: '' })
  const [asignando, setAsignando] = useState(false)

  const { padreDe, nodoDe } = indexarArbol(arbol)

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

  useEffect(() => {
    cargar()
    axios.get('/api/categorias').then(res => setArbol(res.data)).catch(e => console.error('Categorías:', e))
  }, [])

  const abrirEdicion = async (id) => {
    setMensaje('')
    try {
      const res = await axios.get(`/api/admin/productos/${id}/editar`)
      setEditData(res.data)
      const hojaId = res.data.categoriaId ?? ''
      const padreId = hojaId !== '' ? (padreDe[hojaId] ?? '') : ''
      // Si tiene padre, la hoja es la subcategoría; si no, la hoja es la categoría top-level misma.
      setCatPath(padreId !== '' ? { topId: padreId, subId: hojaId } : { topId: hojaId, subId: '' })
    } catch (e) {
      console.error(e)
      setMensaje('Error al abrir el producto.')
    }
  }

  const elegirCategoria = (id) => {
    const tieneHijos = (nodoDe[id]?.hijos || []).length > 0
    setCatPath({ topId: id, subId: '' })
    // Si no tiene subcategorías, la categoría elegida ya es el categoriaId final.
    setCampo('categoriaId', (id && !tieneHijos) ? Number(id) : null)
  }
  const elegirSubcategoria = (id) => {
    setCatPath(prev => ({ ...prev, subId: id }))
    setCampo('categoriaId', id ? Number(id) : null)
  }

  const clasificarFaltantes = async () => {
    setClasificando(true)
    setMensaje('')
    try {
      const res = await axios.post('/api/admin/productos/clasificar-categorias')
      setMensaje(res.data.mensaje)
      await cargar()
    } catch (e) {
      console.error(e)
      setMensaje('Error al clasificar categorías.')
    } finally {
      setClasificando(false)
    }
  }

  const setCampo = (campo, valor) => setEditData(prev => ({ ...prev, [campo]: valor }))

  const setVariante = (idx, campo, valor) => {
    setEditData(prev => ({
      ...prev,
      variantes: prev.variantes.map((v, i) => i === idx ? { ...v, [campo]: valor } : v)
    }))
  }

  // categoriaId final según un selector en cascada {topId, subId}, sin depender del seteo incremental.
  const categoriaIdDe = (path) => {
    const top = path.topId
    if (!top) return { ok: true, id: null }                       // sin categoría
    const tieneHijos = (nodoDe[top]?.hijos || []).length > 0
    if (!tieneHijos) return { ok: true, id: Number(top) }          // categoría de primer nivel (hoja)
    if (path.subId) return { ok: true, id: Number(path.subId) }    // subcategoría elegida
    return { ok: false }                                           // eligió la categoría pero falta la subcategoría
  }

  // --- Selección múltiple + asignación masiva ---
  const productosVisibles = soloSinCategoria
    ? productos.filter(p => p.categoriaId == null)
    : productos

  const toggleSeleccion = (id) => setSeleccionados(prev => {
    const n = new Set(prev)
    if (n.has(id)) n.delete(id); else n.add(id)
    return n
  })
  const todosVisiblesSeleccionados = productosVisibles.length > 0
    && productosVisibles.every(p => seleccionados.has(p.id))
  const toggleTodos = () => {
    setSeleccionados(todosVisiblesSeleccionados ? new Set() : new Set(productosVisibles.map(p => p.id)))
  }

  const asignarMasiva = async () => {
    const cat = categoriaIdDe(catMasiva)
    if (!cat.ok) { setMensaje('Elegí la subcategoría (esa categoría tiene subcategorías).'); return }
    if (!cat.id) { setMensaje('Elegí una categoría para asignar.'); return }
    if (seleccionados.size === 0) return
    setAsignando(true)
    setMensaje('')
    try {
      const res = await axios.post('/api/admin/productos/asignar-categoria', {
        ids: [...seleccionados], categoriaId: cat.id
      })
      setSeleccionados(new Set())
      setCatMasiva({ topId: '', subId: '' })
      await cargar()
      setMensaje(res.data?.mensaje || 'Categoría asignada ✓')
    } catch (e) {
      console.error(e)
      setMensaje('Error al asignar categoría: ' + (e.response?.data?.message || e.message))
    } finally {
      setAsignando(false)
    }
  }

  const guardar = async () => {
    const cat = categoriaIdDe(catPath)
    if (!cat.ok) {
      setMensaje('Elegí la subcategoría (esa categoría tiene subcategorías).')
      return
    }
    setGuardando(true)
    setMensaje('')
    try {
      // Se manda el categoriaId computado del selector (evita guardar null por desincronización).
      await axios.put(`/api/admin/productos/${editData.id}`, { ...editData, categoriaId: cat.id })
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

  return (
    <div>
      <h1>Productos</h1>
      {mensaje && <div className="card" style={{ borderLeft: '4px solid var(--color-lime)', color: 'var(--color-lime)' }}>{mensaje}</div>}

      <div className="card">
        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '10px' }}>
          Clasifica automáticamente (mapeo manual + IA) los productos que todavía no tienen categoría asignada.
        </p>
        <button onClick={clasificarFaltantes} className="btn btn-secondary" disabled={clasificando}>
          {clasificando ? 'Clasificando...' : 'Clasificar categorías faltantes'}
        </button>
      </div>

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
          style={{ borderLeft: '4px solid var(--color-lime)', maxWidth: '820px', width: '100%', marginTop: '30px', maxHeight: '88vh', overflowY: 'auto' }}
        >
          <h2 style={{ marginBottom: '4px' }}>Editar producto</h2>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '16px' }}>
            Proveedor: {editData.proveedor || '—'} (no editable)
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '12px', marginBottom: '10px' }}>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Categoría</label>
              <select style={inputStyle} value={catPath.topId} onChange={e => elegirCategoria(e.target.value)}>
                <option value="">—</option>
                {arbol.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            </div>
            {(nodoDe[catPath.topId]?.hijos?.length > 0) && (
              <div>
                <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Subcategoría</label>
                <select style={inputStyle} value={catPath.subId} onChange={e => elegirSubcategoria(e.target.value)}>
                  <option value="">—</option>
                  {nodoDe[catPath.topId].hijos.map(sc => <option key={sc.id} value={sc.id}>{sc.nombre}</option>)}
                </select>
              </div>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '12px', marginBottom: '18px' }}>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Marca</label>
              <input style={inputStyle} value={editData.marca || ''} onChange={e => setCampo('marca', e.target.value)} />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Modelo</label>
              <input style={inputStyle} value={editData.modelo || ''} onChange={e => setCampo('modelo', e.target.value)} />
            </div>
          </div>

          <h3 style={{ fontSize: '15px', marginBottom: '4px' }}>Peso y dimensiones (para cotizar envío)</h3>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
            Opcional: si se deja vacío, se usa el default de la categoría. Cargalo solo si este producto puntual pesa
            o mide distinto al resto de su categoría.
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '12px', marginBottom: '18px' }}>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Peso (g)</label>
              <input
                style={inputStyle} type="number" min="1" placeholder="default categoría"
                value={editData.pesoGramos ?? ''}
                onChange={e => setCampo('pesoGramos', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Alto (cm)</label>
              <input
                style={inputStyle} type="number" min="1" placeholder="default categoría"
                value={editData.altoCm ?? ''}
                onChange={e => setCampo('altoCm', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Ancho (cm)</label>
              <input
                style={inputStyle} type="number" min="1" placeholder="default categoría"
                value={editData.anchoCm ?? ''}
                onChange={e => setCampo('anchoCm', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Largo (cm)</label>
              <input
                style={inputStyle} type="number" min="1" placeholder="default categoría"
                value={editData.largoCm ?? ''}
                onChange={e => setCampo('largoCm', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
          </div>

          <h3 style={{ fontSize: '15px', marginBottom: '4px' }}>Variantes / Precios</h3>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
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
                    <td style={{ whiteSpace: 'nowrap', color: 'var(--color-text)' }}>{venta.usd != null ? `US$ ${formatNumber(venta.usd)}` : '-'}</td>
                    <td style={{ whiteSpace: 'nowrap', color: 'var(--color-lime)' }}>{venta.ars != null ? `$ ${formatNumber(venta.ars)}` : '-'}</td>
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
        {/* Filtro + contador */}
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap', marginBottom: '12px' }}>
          <label style={{ display: 'flex', gap: '6px', alignItems: 'center', fontSize: '14px', cursor: 'pointer' }}>
            <input type="checkbox" checked={soloSinCategoria}
                   onChange={e => { setSoloSinCategoria(e.target.checked); setSeleccionados(new Set()) }} />
            Solo sin categoría
          </label>
          <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>{productosVisibles.length} producto(s)</span>
        </div>

        {/* Barra de asignación masiva de categoría */}
        {seleccionados.size > 0 && (
          <div style={{ background: 'var(--color-accent-light)', border: '1px solid var(--color-lime-dark)', borderRadius: '10px', padding: '12px 14px', marginBottom: '14px', display: 'flex', gap: '10px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <strong style={{ fontSize: '14px', alignSelf: 'center' }}>{seleccionados.size} seleccionado(s)</strong>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)', display: 'block' }}>Categoría</label>
              <select style={inputStyle} value={catMasiva.topId} onChange={e => setCatMasiva({ topId: e.target.value, subId: '' })}>
                <option value="">—</option>
                {arbol.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            </div>
            {(nodoDe[catMasiva.topId]?.hijos?.length > 0) && (
              <div>
                <label style={{ fontSize: '12px', color: 'var(--color-text-muted)', display: 'block' }}>Subcategoría</label>
                <select style={inputStyle} value={catMasiva.subId} onChange={e => setCatMasiva(prev => ({ ...prev, subId: e.target.value }))}>
                  <option value="">—</option>
                  {nodoDe[catMasiva.topId].hijos.map(sc => <option key={sc.id} value={sc.id}>{sc.nombre}</option>)}
                </select>
              </div>
            )}
            <button onClick={asignarMasiva} className="btn btn-primary" disabled={asignando}>
              {asignando ? 'Asignando...' : `Asignar categoría a ${seleccionados.size}`}
            </button>
            <button onClick={() => setSeleccionados(new Set())} className="btn btn-secondary">Limpiar selección</button>
          </div>
        )}

        {cargando ? (
          <p>Cargando...</p>
        ) : productosVisibles.length === 0 ? (
          <p>{soloSinCategoria ? 'No hay productos sin categoría 🎉' : 'No hay productos. Importá una lista en "Cargar por JSON".'}</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: '34px' }}>
                  <input type="checkbox" checked={todosVisiblesSeleccionados} onChange={toggleTodos} title="Seleccionar todos" />
                </th>
                <th style={{ width: '50px' }}></th>
                <th>Producto</th>
                <th>Categoría</th>
                <th>Proveedor</th>
                <th>SKU</th>
                <th>Últ. actualización</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {productosVisibles.map(p => (
                <tr key={p.id} style={seleccionados.has(p.id) ? { background: 'var(--color-accent-light)' } : {}}>
                  <td>
                    <input type="checkbox" checked={seleccionados.has(p.id)} onChange={() => toggleSeleccion(p.id)} />
                  </td>
                  <td>
                    {p.imagenUrl
                      ? <img src={p.imagenUrl} alt="" style={{ width: '40px', height: '40px', objectFit: 'contain' }} onError={e => { e.target.style.opacity = '0.2' }} />
                      : <span style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>—</span>}
                  </td>
                  <td>{[p.marca, p.modelo].filter(Boolean).join(' ')}</td>
                  <td style={{ fontSize: '13px' }}>
                    {p.categoria || <span style={{ color: '#dc3545' }}>(sin categoría)</span>}
                  </td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}>{p.proveedor}</td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}>{p.sku}</td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>{formatFecha(p.ultimaActualizacion)}</td>
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
