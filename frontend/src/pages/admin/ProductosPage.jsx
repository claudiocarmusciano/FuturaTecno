import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { IconEdit, IconSearch, IconTrash } from '../../components/icons'
import { indexarArbol } from '../../utils/categorias'
import { ordenarPor } from '../../utils/orden'

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// El margen y el flete que se aplican de verdad: el del producto si tiene override, si no el del
// proveedor. null/'' significa "sin override", no 0%.
const margenEfectivo = (ed) => ed?.margenPorcentaje ?? ed?.margenPorcentajeProveedor
const fleteEfectivo = (ed) => ed?.fletePorcentaje ?? ed?.fletePorcentajeProveedor

// Calcula el precio de venta (USD y ARS) a partir del costo, su moneda, y los datos del proveedor.
const calcularVenta = (precio, moneda, ed) => {
  const p = Number(precio)
  const cot = Number(ed?.cotizacion)
  if (!p || !cot) return { usd: null, ars: null }
  const costoUsd = moneda === 'USD' ? p : p / cot
  // Override del producto si lo tiene, si no el del proveedor. Vacío no es 0%: es "usá el del
  // proveedor". Tiene que dar lo mismo que PrecioService, o el preview mentiría sobre el precio.
  const factorFlete = 1 + (Number(fleteEfectivo(ed)) || 0) / 100
  const factorMargen = 1 + (Number(margenEfectivo(ed)) || 0) / 100
  const ventaUsd = costoUsd * factorFlete * factorMargen
  return { usd: ventaUsd, ars: ventaUsd * cot }
}

const inputStyle = {
  width: '100%', padding: '6px 8px', border: '1px solid var(--color-border)',
  borderRadius: '4px', fontSize: '13px', color: 'var(--color-text)'
}

function ProductosPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [editData, setEditData] = useState(null)   // ProductoEditDTO en edición
  const [guardando, setGuardando] = useState(false)
  const [eliminando, setEliminando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [arbol, setArbol] = useState([])
  // topId = categoría de primer nivel elegida; subId = su subcategoría (si tiene). Algunas
  // categorías no tienen subcategorías (ej. "Tablets") y son hoja en sí mismas: ahí topId ES
  // el categoriaId a guardar, sin necesidad de elegir subId.
  const [catPath, setCatPath] = useState({ topId: '', subId: '' })
  const [clasificando, setClasificando] = useState(false)

  // Filtro "solo sin categoría" + búsqueda + selección múltiple + cascada para asignación masiva.
  const [soloSinCategoria, setSoloSinCategoria] = useState(false)
  const [pagina, setPagina] = useState(1)
  // Cambiar un filtro deja el listado más corto: seguir en la página 7 mostraría una tabla vacía.
  const [proveedorFiltro, setProveedorFiltro] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [seleccionados, setSeleccionados] = useState(new Set())
  const [catMasiva, setCatMasiva] = useState({ topId: '', subId: '' })
  const [asignando, setAsignando] = useState(false)
  const [eliminandoMasiva, setEliminandoMasiva] = useState(false)
  const [creandoCat, setCreandoCat] = useState(false)
  const [nombreCatNueva, setNombreCatNueva] = useState('')
  const [guardandoCat, setGuardandoCat] = useState(false)
  const origen = searchParams.get('origen')
  const destinoOrigen = origen === 'categorias' ? '/admin/categorias'
    : origen === 'imagenes' ? `/admin/imagenes?${new URLSearchParams({
      busqueda: searchParams.get('busqueda') || '',
      filtro: searchParams.get('filtro') || 'sin-imagen'
    })}` : null

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
      // El path de categoría lo arma el efecto de abajo, que espera a que esté el árbol.
      pathDerivadoPara.current = null
      setCatPath({ topId: '', subId: '' })
      setEditData(res.data)
    } catch (e) {
      console.error(e)
      setMensaje('Error al abrir el producto.')
    }
  }

  // El árbol de categorías (GET /api/categorias) llega por su cuenta, y la edición puede abrirse
  // antes: entrando por ?editar= desde Imágenes o desde un reporte, abrirEdicion corría con
  // padreDe todavía vacío, así que la hoja (ej. "iPhone") terminaba en el select de primer nivel,
  // donde no figura como opción — y la categoría se veía vacía aunque el producto la tuviera.
  // Por eso el path se deriva acá, recién cuando están el producto Y el árbol.
  const pathDerivadoPara = useRef(null)
  useEffect(() => {
    if (!editData || arbol.length === 0) return
    if (pathDerivadoPara.current === editData.id) return   // no pisar lo que el usuario ya eligió
    pathDerivadoPara.current = editData.id
    const hojaId = editData.categoriaId ?? ''
    const padreId = hojaId !== '' ? (padreDe[hojaId] ?? '') : ''
    // Si tiene padre, la hoja es la subcategoría; si no, la hoja es la categoría top-level misma.
    setCatPath(padreId !== '' ? { topId: padreId, subId: hojaId } : { topId: hojaId, subId: '' })
  }, [editData, arbol])

  // Permite llegar desde los reportes administrativos a la edición de un producto puntual.
  // La URL se conserva para que el enlace se pueda abrir también en otra pestaña.
  useEffect(() => {
    const productoId = Number(searchParams.get('editar'))
    if (Number.isInteger(productoId) && productoId > 0) abrirEdicion(productoId)
  }, [searchParams])

  useEffect(() => { setPagina(1) }, [busqueda, soloSinCategoria, proveedorFiltro])

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

  // --- Filtros + selección múltiple + asignación masiva ---
  // La búsqueda es en el navegador a propósito: el listado del admin ya viene entero (son ~1.500
  // productos), así que filtrar acá es instantáneo y no agrega un viaje al backend por tecla.
  // Si el catálogo creciera un orden de magnitud, esto hay que pasarlo a server-side.
  const termino = busqueda.trim().toLowerCase()
  const coincide = (p) => {
    if (!termino) return true
    // Se busca por palabras sueltas y en cualquier orden: "lenovo 15" encuentra
    // "Lenovo IdeaPad Slim 3 15.6".
    const texto = [p.marca, p.modelo, p.sku, p.categoria, p.especificaciones]
      .filter(Boolean).join(' ').toLowerCase()
    return termino.split(/\s+/).every(palabra => texto.includes(palabra))
  }

  const productosVisibles = productos
    .filter(p => !soloSinCategoria || p.categoriaId == null)
    .filter(p => !proveedorFiltro || p.proveedor === proveedorFiltro)
    .filter(coincide)
  const proveedores = ordenarPor([...new Set(productos.map(p => p.proveedor).filter(Boolean))])

  // Se pinta de a una página. Antes se dibujaban las ~4.000 filas de una: 75.000 nodos de DOM y
  // una petición de imagen por fila, con el hilo principal bloqueado varios segundos y las fotos
  // cayendo de a goteo (medida una en 133 s). La API nunca fue el problema: responde en ~500 ms.
  // `productosVisibles` sigue siendo el filtrado COMPLETO a propósito: es lo que usan "seleccionar
  // todos" y las acciones masivas, y paginarlo cambiaría en silencio qué abarca una asignación.
  const POR_PAGINA = 50
  const totalPaginas = Math.max(1, Math.ceil(productosVisibles.length / POR_PAGINA))
  const paginaActual = Math.min(pagina, totalPaginas)
  const productosPagina = productosVisibles.slice((paginaActual - 1) * POR_PAGINA, paginaActual * POR_PAGINA)

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

  // Alta de categoría sin salir de Productos: el caso real es descubrir que la categoría no
  // existe justo cuando se va a asignar. Cuelga de la categoría elegida arriba (queda como
  // subcategoría) o nace de primer nivel si no hay ninguna elegida, y se autoselecciona.
  const crearCategoria = async () => {
    const nombre = nombreCatNueva.trim()
    if (!nombre) return
    const padreId = catMasiva.topId ? Number(catMasiva.topId) : null
    setGuardandoCat(true)
    setMensaje('')
    try {
      const { data } = await axios.post('/api/admin/categorias', { nombre, padreId })
      const res = await axios.get('/api/categorias')
      setArbol(res.data)
      setCatMasiva(padreId ? { topId: String(padreId), subId: String(data.id) } : { topId: String(data.id), subId: '' })
      setNombreCatNueva('')
      setCreandoCat(false)
      setMensaje(`Categoría "${nombre}" creada ✓`)
    } catch (e) {
      console.error(e)
      setMensaje('No se pudo crear la categoría: ' + (e.response?.data?.error || e.message))
    } finally {
      setGuardandoCat(false)
    }
  }

  const cancelarAltaCategoria = () => { setCreandoCat(false); setNombreCatNueva('') }

  const eliminarMasivamente = async () => {
    const cantidad = seleccionados.size
    if (cantidad === 0) return
    if (!window.confirm(`¿Dar de baja ${cantidad} producto(s)? Dejarán de verse en el catálogo y en este listado.`)) return
    setEliminandoMasiva(true)
    setMensaje('')
    try {
      const res = await axios.post('/api/admin/productos/dar-de-baja', { ids: [...seleccionados] })
      setSeleccionados(new Set())
      await cargar()
      setMensaje(res.data?.mensaje || `${cantidad} producto(s) dados de baja ✓`)
    } catch (e) {
      console.error(e)
      setMensaje('Error al dar de baja los productos: ' + (e.response?.data?.message || e.message))
    } finally {
      setEliminandoMasiva(false)
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
      if (destinoOrigen) {
        navigate(destinoOrigen)
        return
      }
      setEditData(null)
      await cargar()
      setMensaje('Producto actualizado ✓')
    } catch (e) {
      console.error(e)
      setMensaje('Error al guardar: ' + (e.response?.data?.error || e.response?.data?.message || e.message))
    } finally {
      setGuardando(false)
    }
  }

  // Baja lógica (activo=false): el producto deja de verse en catálogo y admin, pero un pedido
  // que ya lo referencie conserva su trazabilidad (guarda su propio snapshot, no depende del
  // producto vivo). No hay "deshacer" en el panel, por eso se confirma con el nombre a la vista.
  const eliminarProducto = async () => {
    const nombre = [editData.marca, editData.modelo].filter(Boolean).join(' ') || 'este producto'
    if (!window.confirm(`¿Eliminar "${nombre}"? Deja de verse en el catálogo y en este listado.`)) return
    setEliminando(true)
    setMensaje('')
    try {
      await axios.delete(`/api/admin/productos/${editData.id}`)
      if (destinoOrigen) {
        navigate(destinoOrigen)
        return
      }
      setEditData(null)
      await cargar()
      setMensaje('Producto eliminado ✓')
    } catch (e) {
      console.error(e)
      setMensaje('Error al eliminar: ' + (e.response?.data?.message || e.message))
    } finally {
      setEliminando(false)
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

          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 110px', gap: '12px', alignItems: 'end', marginBottom: '18px' }}>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>URL de imagen principal</label>
              <input
                style={inputStyle}
                type="url"
                placeholder="https://ejemplo.com/imagen.jpg"
                value={editData.imagenUrl || ''}
                onChange={e => setCampo('imagenUrl', e.target.value)}
              />
              <small style={{ color: 'var(--color-text-muted)', display: 'block', marginTop: '4px' }}>Vacío = producto sin imagen.</small>
            </div>
            <div style={{ height: '88px', border: '1px solid var(--color-border)', borderRadius: '6px', background: 'var(--color-surface-2)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
              {editData.imagenUrl
                ? <img src={editData.imagenUrl} alt="Vista previa de la imagen principal" style={{ width: '100%', height: '100%', objectFit: 'contain' }} onError={e => { e.currentTarget.style.display = 'none' }} />
                : <span style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>Sin imagen</span>}
            </div>
          </div>

          <h3 style={{ fontSize: '15px', marginBottom: '4px' }}>Peso y dimensiones (para cotizar envío)</h3>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
            Si se deja vacío, se usa el valor por defecto de la categoría. Cargalo solo si este producto puntual pesa
            o mide distinto al resto de su categoría; al guardar queda como un ajuste propio del producto.
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '12px', marginBottom: '18px' }}>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Margen % · proveedor: {editData.margenPorcentajeProveedor ?? '—'}</label>
              <input
                style={inputStyle} type="number" min="0" step="0.01"
                placeholder={editData.margenPorcentajeProveedor != null ? `${editData.margenPorcentajeProveedor}%` : 'sin definir'}
                value={editData.margenPorcentaje ?? ''}
                onChange={e => setCampo('margenPorcentaje', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Flete % · proveedor: {editData.fletePorcentajeProveedor ?? '—'}</label>
              <input
                style={inputStyle} type="number" min="0" step="0.01"
                placeholder={editData.fletePorcentajeProveedor != null ? `${editData.fletePorcentajeProveedor}%` : 'sin definir'}
                value={editData.fletePorcentaje ?? ''}
                onChange={e => setCampo('fletePorcentaje', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Peso (g) · default: {editData.pesoGramosDefault ?? '—'}</label>
              <input
                style={inputStyle} type="number" min="1" placeholder={editData.pesoGramosDefault != null ? `${editData.pesoGramosDefault} g` : 'sin default'}
                value={editData.pesoGramos ?? ''}
                onChange={e => setCampo('pesoGramos', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Alto (cm) · default: {editData.altoCmDefault ?? '—'}</label>
              <input
                style={inputStyle} type="number" min="1" placeholder={editData.altoCmDefault != null ? `${editData.altoCmDefault} cm` : 'sin default'}
                value={editData.altoCm ?? ''}
                onChange={e => setCampo('altoCm', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Ancho (cm) · default: {editData.anchoCmDefault ?? '—'}</label>
              <input
                style={inputStyle} type="number" min="1" placeholder={editData.anchoCmDefault != null ? `${editData.anchoCmDefault} cm` : 'sin default'}
                value={editData.anchoCm ?? ''}
                onChange={e => setCampo('anchoCm', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
            <div>
              <label style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>Largo (cm) · default: {editData.largoCmDefault ?? '—'}</label>
              <input
                style={inputStyle} type="number" min="1" placeholder={editData.largoCmDefault != null ? `${editData.largoCmDefault} cm` : 'sin default'}
                value={editData.largoCm ?? ''}
                onChange={e => setCampo('largoCm', e.target.value === '' ? null : Number(e.target.value))}
              />
            </div>
          </div>

          <h3 style={{ fontSize: '15px', marginBottom: '4px' }}>Variantes / Precios</h3>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
            El precio que cargás es el <strong>costo</strong>. La venta se calcula con flete {Number(fleteEfectivo(editData)) || 0}% + margen {Number(margenEfectivo(editData)) || 0}%
            {(editData.fletePorcentaje != null || editData.margenPorcentaje != null) && <strong> (ajustado para este producto)</strong>} · dólar ${formatNumber(editData.cotizacion)}.
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

          <div style={{ marginTop: '14px', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
            <div>
              <button onClick={guardar} className="btn btn-primary" disabled={guardando || eliminando} style={{ marginRight: '10px' }}>
                {guardando ? 'Guardando...' : 'Guardar cambios'}
              </button>
              <button onClick={() => destinoOrigen ? navigate(destinoOrigen) : setEditData(null)} className="btn btn-secondary" disabled={guardando || eliminando}>Cancelar</button>
            </div>
            <button
              onClick={eliminarProducto}
              className="btn-accion danger"
              disabled={guardando || eliminando}
              title="Eliminar este producto"
            >
              <IconTrash /> {eliminando ? 'Eliminando...' : 'Eliminar producto'}
            </button>
          </div>
        </div>
        </div>
      )}

      {/* Lista de productos */}
      <div className="card">
        {/* Buscador + filtro + contador */}
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap', marginBottom: '12px' }}>
          <div style={{ position: 'relative', flex: '1 1 280px', maxWidth: '420px' }}>
            <span style={{
              position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)',
              width: '16px', height: '16px', color: 'var(--color-text-muted)', pointerEvents: 'none'
            }}>
              <IconSearch />
            </span>
            <input
              value={busqueda}
              onChange={e => setBusqueda(e.target.value)}
              placeholder="Buscar por marca, modelo, SKU o categoría"
              aria-label="Buscar productos"
              style={{
                width: '100%', padding: '8px 32px 8px 34px', fontSize: '14px',
                border: '1px solid var(--color-border)', borderRadius: '8px',
                background: 'transparent', color: 'var(--color-text)'
              }}
            />
            {busqueda && (
              <button
                type="button"
                onClick={() => setBusqueda('')}
                aria-label="Limpiar búsqueda"
                style={{
                  position: 'absolute', right: '6px', top: '50%', transform: 'translateY(-50%)',
                  background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px',
                  lineHeight: 1, color: 'var(--color-text-muted)', padding: '2px 6px'
                }}
              >
                ×
              </button>
            )}
          </div>
          <label style={{ display: 'flex', gap: '6px', alignItems: 'center', fontSize: '14px', cursor: 'pointer' }}>
            <input type="checkbox" className="check-seleccion" checked={soloSinCategoria}
                   onChange={e => { setSoloSinCategoria(e.target.checked); setSeleccionados(new Set()) }} />
            Solo sin categoría
          </label>
          <select
            value={proveedorFiltro}
            onChange={e => { setProveedorFiltro(e.target.value); setSeleccionados(new Set()) }}
            aria-label="Filtrar por proveedor"
            style={{ padding: '7px 9px', border: '1px solid var(--color-border)', borderRadius: '8px', background: 'transparent', color: 'var(--color-text)', fontSize: '14px' }}
          >
            <option value="">Todos los proveedores</option>
            {proveedores.map(proveedor => <option key={proveedor} value={proveedor}>{proveedor}</option>)}
          </select>
          <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
            {productosVisibles.length} producto(s)
            {termino && productos.length !== productosVisibles.length && ` de ${productos.length}`}
          </span>
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
            {creandoCat ? (
              <div>
                <label style={{ fontSize: '12px', color: 'var(--color-text-muted)', display: 'block' }}>
                  {catMasiva.topId ? `Nueva subcategoría de ${nodoDe[catMasiva.topId]?.nombre}` : 'Nueva categoría'}
                </label>
                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                  <input
                    autoFocus
                    style={inputStyle}
                    value={nombreCatNueva}
                    placeholder="Ej. eReaders"
                    onChange={e => setNombreCatNueva(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter') crearCategoria()
                      if (e.key === 'Escape') cancelarAltaCategoria()
                    }}
                  />
                  <button onClick={crearCategoria} className="btn btn-primary" disabled={guardandoCat || !nombreCatNueva.trim()}>
                    {guardandoCat ? 'Creando...' : 'Crear'}
                  </button>
                  <button onClick={cancelarAltaCategoria} className="btn btn-secondary" disabled={guardandoCat}>Cancelar</button>
                </div>
              </div>
            ) : (
              <button onClick={() => setCreandoCat(true)} className="btn btn-secondary" disabled={asignando || eliminandoMasiva}>
                + Nueva categoría
              </button>
            )}
            <button onClick={asignarMasiva} className="btn btn-primary" disabled={asignando || eliminandoMasiva || creandoCat}>
              {asignando ? 'Asignando...' : `Asignar categoría a ${seleccionados.size}`}
            </button>
            <button onClick={eliminarMasivamente} className="btn-accion danger" disabled={asignando || eliminandoMasiva}>
              <IconTrash /> {eliminandoMasiva ? 'Dando de baja...' : `Dar de baja ${seleccionados.size}`}
            </button>
            <button onClick={() => setSeleccionados(new Set())} className="btn btn-secondary" disabled={asignando || eliminandoMasiva}>Limpiar selección</button>
          </div>
        )}

        {cargando ? (
          <p>Cargando...</p>
        ) : productosVisibles.length === 0 ? (
          <p>{termino
            ? `No hay resultados para "${busqueda.trim()}".`
            : soloSinCategoria
              ? 'No hay productos sin categoría 🎉'
              : 'No hay productos. Importá una lista en "Cargar por JSON".'}</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: '44px' }}>
                  <input type="checkbox" className="check-seleccion" checked={todosVisiblesSeleccionados} onChange={toggleTodos} title="Seleccionar todos" />
                </th>
                <th style={{ width: '50px' }}></th>
                <th>Producto</th>
                <th>Categoría</th>
                <th>Proveedor</th>
                <th>SKU</th>
                <th style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>Costo USD</th>
                <th style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>Venta</th>
                <th>Últ. actualización</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {productosPagina.map(p => (
                <tr key={p.id} style={seleccionados.has(p.id) ? { background: 'var(--color-accent-light)' } : {}}>
                  <td>
                    <input type="checkbox" className="check-seleccion" checked={seleccionados.has(p.id)} onChange={() => toggleSeleccion(p.id)} />
                  </td>
                  <td>
                    {p.imagenUrl
                      ? <img src={p.imagenUrl} alt="" loading="lazy" width="40" height="40" style={{ width: '40px', height: '40px', objectFit: 'contain' }} onError={e => { e.target.style.opacity = '0.2' }} />
                      : <span style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>—</span>}
                  </td>
                  <td>{[p.marca, p.modelo].filter(Boolean).join(' ')}</td>
                  <td style={{ fontSize: '13px' }}>
                    {p.categoria || <span style={{ color: '#dc3545' }}>(sin categoría)</span>}
                  </td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}>{p.proveedor}</td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}>{p.sku}</td>
                  <td style={{ fontSize: '13px', textAlign: 'right', whiteSpace: 'nowrap', color: 'var(--color-text-muted)' }}>
                    {p.costoUsd != null ? `US$ ${formatNumber(p.costoUsd)}` : '—'}
                  </td>
                  <td style={{ fontSize: '13px', textAlign: 'right', whiteSpace: 'nowrap' }}>
                    {p.ventaUsd != null ? <>
                      <strong>US$ {formatNumber(p.ventaUsd)}</strong>
                      {p.ventaArs != null && (
                        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                          ${formatNumber(p.ventaArs)}
                        </div>
                      )}
                    </> : '—'}
                  </td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>{formatFecha(p.ultimaActualizacion)}</td>
                  <td>
                    <button onClick={() => abrirEdicion(p.id)} className="btn-accion"><IconEdit /> Editar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {totalPaginas > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', marginTop: '16px', flexWrap: 'wrap' }}>
            <button type="button" className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '13px' }}
                    disabled={paginaActual === 1} onClick={() => setPagina(paginaActual - 1)}>← Anterior</button>
            <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
              Página {paginaActual} de {totalPaginas} · mostrando {productosPagina.length} de {productosVisibles.length}
            </span>
            <button type="button" className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '13px' }}
                    disabled={paginaActual === totalPaginas} onClick={() => setPagina(paginaActual + 1)}>Siguiente →</button>
          </div>
        )}
      </div>
    </div>
  )
}

export default ProductosPage
