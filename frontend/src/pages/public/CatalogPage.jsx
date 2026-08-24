import { useState, useEffect, useMemo, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import { indexarArbol, idsHojaDe } from '../../utils/categorias'
import { useCart } from '../../cart/CartContext'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : null

const formatFechaLarga = (isoDate) => {
  if (!isoDate) return null
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
}

// Orden por defecto del catálogo: del más barato al más caro. "relevancia" (el orden en que
// los devuelve la API) queda como opción, pero ya no es lo primero que ve el visitante.
const ORDEN_POR_DEFECTO = 'precio-asc'

// Cuántas tarjetas se pintan por página. 24 es divisible por 2, 3 y 4, así que la última fila
// queda completa en cualquiera de los anchos de la grilla (auto-fill de 260px).
// Ojo: la paginación es del lado del cliente — la API sigue devolviendo el catálogo entero.
const POR_PAGINA = 24

const botonPagina = (activo, deshabilitado) => ({
  padding: '8px 14px', borderRadius: '8px', fontSize: '14px', fontWeight: 600,
  border: `1px solid ${activo ? 'var(--color-lime)' : 'var(--color-border)'}`,
  background: activo ? 'var(--color-lime)' : 'transparent',
  color: activo ? '#16181d' : 'var(--color-text)',
  cursor: deshabilitado ? 'default' : 'pointer',
  opacity: deshabilitado ? 0.35 : 1
})

/**
 * Números a mostrar: siempre la primera y la última, más una ventana alrededor de la actual.
 * Con 603 productos son 26 páginas — listarlas todas sería una tira inusable en mobile.
 */
const paginasVisibles = (actual, total) => {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const nums = new Set([1, total, actual, actual - 1, actual + 1])
  const ordenadas = [...nums].filter(n => n >= 1 && n <= total).sort((a, b) => a - b)
  const out = []
  ordenadas.forEach((n, i) => {
    if (i > 0 && n - ordenadas[i - 1] > 1) out.push('…')
    out.push(n)
  })
  return out
}

// Precio "desde" del producto: el menor precio USD entre sus variantes (para ordenar/filtrar).
const precioDesde = (p) => {
  const precios = (p.variantes || []).map(v => Number(v.precioUsd)).filter(n => n > 0)
  return precios.length ? Math.min(...precios) : Infinity
}

// Nodo del árbol de categorías, tipo acordeón: si tiene subcategorías, tocar la fila entera
// pliega/despliega (no filtra); si es una hoja (subcategoría, o categoría sin hijos), tocarla filtra.
function NodoCategoria({ nodo, nivel, seleccionado, onSeleccionar, expandidos, toggleExpandir }) {
  const tieneHijos = nodo.hijos && nodo.hijos.length > 0
  const expandido = expandidos.has(nodo.id)
  const activo = seleccionado === nodo.id

  return (
    <div style={nivel === 0 ? { borderBottom: '1px solid var(--color-border)' } : {}}>
      <button
        onClick={() => tieneHijos ? toggleExpandir(nodo.id) : onSeleccionar(nodo.id)}
        style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%',
          background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left',
          padding: nivel === 0 ? '11px 4px' : '9px 4px 9px', paddingLeft: `${4 + nivel * 16}px`,
          fontSize: nivel === 0 ? '14px' : '13px', fontWeight: activo ? 700 : (nivel === 0 ? 600 : 500),
          color: activo ? 'var(--color-accent)' : 'var(--color-text)'
        }}
      >
        <span>{nodo.nombre}</span>
        {tieneHijos && (
          <span style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>{expandido ? '▾' : '▸'}</span>
        )}
      </button>
      {tieneHijos && expandido && (
        <div style={{ paddingBottom: '4px' }}>
          {nodo.hijos.map(h => (
            <NodoCategoria
              key={h.id} nodo={h} nivel={nivel + 1}
              seleccionado={seleccionado} onSeleccionar={onSeleccionar}
              expandidos={expandidos} toggleExpandir={toggleExpandir}
            />
          ))}
        </div>
      )}
    </div>
  )
}

// Estilo de cada opción dentro del panel desplegable de marcas.
const opcionMarca = (activo) => ({
  display: 'block', width: '100%', textAlign: 'left', padding: '8px 10px',
  background: activo ? 'var(--color-lime)' : 'none', border: 'none', borderRadius: '6px',
  cursor: 'pointer', fontSize: '14px', color: activo ? '#16181d' : 'var(--color-text)',
  fontWeight: activo ? 600 : 500
})

// Filtro de marca como menú desplegable: se abre al tocar el botón, muestra la marca elegida,
// y al seleccionar una opción (o clickear afuera) se vuelve a plegar. Mismo comportamiento en desktop y mobile.
function MarcaDropdown({ marca, marcas, onChange }) {
  const [abierto, setAbierto] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    if (!abierto) return
    const onDoc = (e) => { if (ref.current && !ref.current.contains(e.target)) setAbierto(false) }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [abierto])

  const elegir = (m) => { onChange(m); setAbierto(false) }

  return (
    <div ref={ref} style={{ position: 'relative', maxWidth: '260px' }}>
      <button
        type="button"
        onClick={() => setAbierto(a => !a)}
        style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px', width: '100%',
          padding: '9px 12px', fontSize: '14px', border: '1px solid var(--color-border)', borderRadius: '8px',
          background: 'var(--color-surface-2)', color: 'var(--color-text)', cursor: 'pointer'
        }}
      >
        <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{marca || 'Todas las marcas'}</span>
        <span style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>{abierto ? '▾' : '▸'}</span>
      </button>
      {abierto && (
        <div style={{
          position: 'absolute', top: 'calc(100% + 4px)', left: 0, right: 0, zIndex: 20,
          background: 'var(--color-surface-2)', border: '1px solid var(--color-border)', borderRadius: '8px',
          boxShadow: 'var(--shadow)', maxHeight: '280px', overflowY: 'auto', padding: '4px'
        }}>
          <button type="button" onClick={() => elegir('')} style={opcionMarca(marca === '')}>Todas</button>
          {marcas.map(m => (
            <button key={m} type="button" onClick={() => elegir(m)} style={opcionMarca(marca === m)}>{m}</button>
          ))}
        </div>
      )}
    </div>
  )
}

function CatalogPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  // Los filtros viven en la URL (?cat=&marca=&q=&orden=&min=&max=) para que se conserven
  // al volver desde el detalle de un producto (o con el botón "atrás" del navegador).
  const [searchParams, setSearchParams] = useSearchParams()

  const [arbol, setArbol] = useState([])
  const [categoriaId, setCategoriaId] = useState(() => {
    const c = searchParams.get('cat')
    return c ? Number(c) : ''
  })
  const [expandidos, setExpandidos] = useState(new Set())
  const [menuAbierto, setMenuAbierto] = useState(false)   // drawer de categorías en mobile

  const [busqueda, setBusqueda] = useState(() => searchParams.get('q') || '')
  const [marca, setMarca] = useState(() => searchParams.get('marca') || '')
  const [orden, setOrden] = useState(() => searchParams.get('orden') || ORDEN_POR_DEFECTO)   // 'relevancia', 'precio-asc', 'precio-desc'
  const [precioMin, setPrecioMin] = useState(() => searchParams.get('min') || '')
  const [precioMax, setPrecioMax] = useState(() => searchParams.get('max') || '')
  const [eta, setEta] = useState(null)
  const [cotizacion, setCotizacion] = useState(null)
  const { agregar } = useCart()
  const [agregado, setAgregado] = useState(null)   // id del producto recién agregado (feedback)
  const [pagina, setPagina] = useState(1)

  // Refleja los filtros actuales en la URL (replace: no ensucia el historial en cada tecla).
  useEffect(() => {
    const params = {}
    if (categoriaId) params.cat = String(categoriaId)
    if (marca) params.marca = marca
    if (busqueda) params.q = busqueda
    if (orden && orden !== ORDEN_POR_DEFECTO) params.orden = orden
    if (precioMin) params.min = precioMin
    if (precioMax) params.max = precioMax
    setSearchParams(params, { replace: true })
  }, [categoriaId, marca, busqueda, orden, precioMin, precioMax, setSearchParams])

  useEffect(() => {
    axios.get('/api/productos')
      .then(res => setProductos(res.data))
      .catch(err => {
        console.error('Error al cargar catálogo:', err)
        setError('No se pudo cargar el catálogo. ¿Está corriendo el backend?')
      })
      .finally(() => setCargando(false))
    axios.get('/api/categorias').then(res => setArbol(res.data)).catch(err => console.error('Categorías:', err))
    axios.get('/api/eta').then(res => setEta(res.data)).catch(err => console.error('ETA:', err))
    axios.get('/api/cotizacion').then(res => setCotizacion(res.data)).catch(err => console.error('Cotización:', err))
  }, [])

  // Solo se muestran en el menú las categorías (y subcategorías) que tienen al menos
  // un producto cargado, en cualquier nivel de profundidad.
  const arbolConProductos = useMemo(() => {
    const idsConProductos = new Set(productos.map(p => p.categoriaId).filter(Boolean))
    const podar = (nodos) => nodos
      .map(n => {
        const hijos = n.hijos?.length ? podar(n.hijos) : []
        const esHojaConProductos = !n.hijos?.length && idsConProductos.has(n.id)
        return (hijos.length > 0 || esHojaConProductos) ? { ...n, hijos } : null
      })
      .filter(Boolean)
    return podar(arbol)
  }, [arbol, productos])

  const { nodoDe, padreDe } = useMemo(() => indexarArbol(arbolConProductos), [arbolConProductos])

  // Al restaurar una categoría desde la URL (o al seleccionarla), expande su rama para que
  // la selección quede visible en el árbol lateral. Solo agrega; nunca colapsa lo que abrió el usuario.
  useEffect(() => {
    if (!categoriaId) return
    const ancestros = []
    let cur = padreDe[categoriaId]
    while (cur) { ancestros.push(cur); cur = padreDe[cur] }
    if (ancestros.length) setExpandidos(prev => new Set([...prev, ...ancestros]))
  }, [categoriaId, padreDe])

  const toggleExpandir = (id) => setExpandidos(prev => {
    const next = new Set(prev)
    if (next.has(id)) next.delete(id); else next.add(id)
    return next
  })
  const seleccionarCategoria = (id) => {
    setCategoriaId(prev => prev === id ? '' : id)
    setMenuAbierto(false)   // en mobile, elegir una subcategoría cierra el drawer solo
  }

  // Ids de subcategoría (hoja) que caen bajo el nodo elegido, sea sección, categoría u hoja.
  // Se filtra por id, nunca por nombre: nombres como "Imagen" se repiten en ramas distintas.
  const idsFiltro = useMemo(() => {
    if (!categoriaId || !nodoDe[categoriaId]) return null
    return new Set(idsHojaDe(nodoDe[categoriaId]))
  }, [categoriaId, nodoDe])

  const marcas = useMemo(
    () => [...new Set(productos.map(p => p.marca).filter(Boolean))].sort(),
    [productos]
  )

  // Rango real de precios (en US$) de todo el catálogo, para guiar al usuario.
  const rangoPrecios = useMemo(() => {
    const ps = productos.map(precioDesde).filter(n => Number.isFinite(n))
    return ps.length ? { min: Math.floor(Math.min(...ps)), max: Math.ceil(Math.max(...ps)) } : null
  }, [productos])

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    const min = precioMin !== '' ? Number(precioMin) : null
    const max = precioMax !== '' ? Number(precioMax) : null

    let lista = productos.filter(p => {
      if (idsFiltro && !idsFiltro.has(p.categoriaId)) return false
      if (marca && p.marca !== marca) return false
      if (q) {
        const texto = [p.categoria, p.marca, p.modelo,
          ...(p.variantes || []).map(v => v.especificaciones)].filter(Boolean).join(' ').toLowerCase()
        if (!texto.includes(q)) return false
      }
      const precio = precioDesde(p)
      if (min != null && precio < min) return false
      if (max != null && precio > max) return false
      return true
    })

    if (orden === 'precio-asc') lista = [...lista].sort((a, b) => precioDesde(a) - precioDesde(b))
    if (orden === 'precio-desc') lista = [...lista].sort((a, b) => precioDesde(b) - precioDesde(a))
    return lista
  }, [productos, busqueda, idsFiltro, marca, orden, precioMin, precioMax])

  // Cualquier cambio de filtro devuelve a la página 1: si estabas en la 12 y filtrás algo que
  // deja 3 resultados, quedarías mirando una página vacía.
  useEffect(() => { setPagina(1) }, [categoriaId, marca, busqueda, orden, precioMin, precioMax])

  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA))
  const paginaActual = Math.min(pagina, totalPaginas)
  const desde = (paginaActual - 1) * POR_PAGINA
  const visibles = filtrados.slice(desde, desde + POR_PAGINA)

  const irAPagina = (n) => {
    setPagina(Math.min(Math.max(1, n), totalPaginas))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const limpiarTodo = () => {
    setCategoriaId(''); setMarca(''); setBusqueda(''); setOrden(ORDEN_POR_DEFECTO); setPrecioMin(''); setPrecioMax('')
  }
  const hayFiltros = categoriaId || marca || busqueda || orden !== ORDEN_POR_DEFECTO || precioMin || precioMax

  if (cargando) return (<div><h1>Catálogo</h1><div className="card"><p>Cargando productos...</p></div></div>)
  if (error) return (<div><h1>Catálogo</h1><div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div></div>)
  if (productos.length === 0) return (<div><h1>Catálogo</h1><div className="card"><p>Todavía no hay productos cargados.</p></div></div>)

  const inputFiltro = {
    padding: '9px 12px', fontSize: '14px', border: '1px solid var(--color-border)',
    borderRadius: '8px', color: 'var(--color-text)', background: 'var(--color-surface-2)'
  }

  return (
    <div>
      <h1 style={{ marginBottom: '6px' }}>Catálogo</h1>

      {eta?.fechaEntrega && (
        <div style={{
          background: 'var(--color-accent-light)', border: '1px solid var(--color-border)', borderRadius: '12px',
          padding: '12px 16px', margin: '18px 0 20px', fontSize: '14px', color: 'var(--color-text-muted)'
        }}>
          🚚 Comprando hoy, tu pedido llega aprox. el <strong style={{ color: 'var(--color-text)' }}>{formatFechaLarga(eta.fechaEntrega)}</strong> ({eta.diasHabiles} días hábiles).
        </div>
      )}

      {/* Botón hamburguesa: solo visible en mobile (ver CSS) */}
      <button className="catalogo-menu-toggle" onClick={() => setMenuAbierto(true)} aria-label="Abrir categorías">
        ☰ Categorías
      </button>

      {menuAbierto && <div className="catalogo-sidebar-backdrop" onClick={() => setMenuAbierto(false)} />}

      <div className="catalogo-layout">
        {/* Sidebar de categorías (drawer deslizable en mobile, panel fijo en desktop) */}
        <aside className={`catalogo-sidebar${menuAbierto ? ' abierto' : ''}`}>
          <div className="catalogo-sidebar-header">
            <span>📂 Categorías</span>
            <button className="catalogo-sidebar-cerrar" onClick={() => setMenuAbierto(false)} aria-label="Cerrar">✕</button>
          </div>
          <div style={{ marginTop: '10px' }}>
            <button
              onClick={() => { setCategoriaId(''); setMenuAbierto(false) }}
              style={{
                background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left', padding: '5px 0',
                fontSize: '14px', fontWeight: categoriaId === '' ? 700 : 600,
                color: categoriaId === '' ? 'var(--color-accent)' : 'var(--color-text)'
              }}
            >
              Todas
            </button>
            {arbolConProductos.map(seccion => (
              <NodoCategoria
                key={seccion.id} nodo={seccion} nivel={0}
                seleccionado={categoriaId} onSeleccionar={seleccionarCategoria}
                expandidos={expandidos} toggleExpandir={toggleExpandir}
              />
            ))}
          </div>
        </aside>

        <div>
          {/* Barra de filtros */}
          <div className="card" style={{ marginBottom: '18px' }}>
            <input
              type="text"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="🔍 Buscar por marca, modelo o características..."
              style={{ ...inputFiltro, width: '100%', marginBottom: '16px' }}
            />

            {marcas.length > 1 && (
              <div style={{ marginBottom: '14px' }}>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>Marca</div>
                <MarcaDropdown marca={marca} marcas={marcas} onChange={setMarca} />
              </div>
            )}

            {/* Orden + rango de precio */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'flex-end', borderTop: '1px solid var(--color-border)', paddingTop: '14px' }}>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>Ordenar por</div>
                <select value={orden} onChange={e => setOrden(e.target.value)} style={inputFiltro}>
                  <option value="relevancia">Relevancia</option>
                  <option value="precio-asc">Precio: menor a mayor</option>
                  <option value="precio-desc">Precio: mayor a menor</option>
                </select>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>
                  Precio en US$
                  {rangoPrecios && (
                    <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}> (entre {rangoPrecios.min} y {rangoPrecios.max})</span>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <input type="number" value={precioMin} onChange={e => setPrecioMin(e.target.value)} placeholder={rangoPrecios ? `${rangoPrecios.min}` : 'Mín'} style={{ ...inputFiltro, width: '100px' }} />
                  <span style={{ color: 'var(--color-text-muted)' }}>—</span>
                  <input type="number" value={precioMax} onChange={e => setPrecioMax(e.target.value)} placeholder={rangoPrecios ? `${rangoPrecios.max}` : 'Máx'} style={{ ...inputFiltro, width: '100px' }} />
                </div>
              </div>
              {hayFiltros && (
                <button onClick={limpiarTodo} className="btn btn-secondary" style={{ padding: '9px 14px', fontSize: '13px' }}>
                  Limpiar filtros
                </button>
              )}
            </div>
          </div>

          <p style={{ color: 'var(--color-text-muted)', marginBottom: '4px', fontSize: '14px' }}>
            {filtrados.length === 0
              ? <>Sin resultados de {productos.length} producto(s)</>
              : <>Mostrando <strong>{desde + 1}–{desde + visibles.length}</strong> de {filtrados.length}
                  {filtrados.length !== productos.length && <> (filtrados de {productos.length})</>}</>}
          </p>
          <p style={{ color: 'var(--color-text-muted)', fontSize: '12px', marginBottom: '22px' }}>
            <strong>Las imágenes son meramente ilustrativas:</strong> confirmá características, color y disponibilidad antes de comprar · Por la alta rotación de stock, la disponibilidad se confirma al procesar el pedido
            {cotizacion?.valor && <> · 💵 Precios calculados al {cotizacion.fuente} ${formatNumber(cotizacion.valor)}</>}
          </p>

          {filtrados.length === 0 ? (
            <div className="card"><p>No hay productos que coincidan con los filtros.</p></div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(260px, 100%), 1fr))', gap: '20px' }}>
              {visibles.map(p => (
                <Link key={p.id} to={`/producto/${p.id}`} className="producto-card">
                  {/* Tile blanco a propósito: las fotos de los mayoristas vienen recortadas sobre
                      blanco o en PNG transparente, y sobre el fondo oscuro un producto negro
                      desaparecería. */}
                  {p.imagenUrl ? (
                    <img
                      src={p.imagenUrl}
                      alt={`${p.marca} ${p.modelo}`}
                      style={{ width: '100%', height: '180px', objectFit: 'contain', marginBottom: '14px', background: '#fff', borderRadius: '8px' }}
                      onError={(e) => { e.target.style.display = 'none' }}
                    />
                  ) : (
                    <div style={{
                      width: '100%', height: '180px', marginBottom: '14px', background: 'var(--color-surface-2)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-text-muted)', fontSize: '13px', borderRadius: '8px'
                    }}>Sin imagen</div>
                  )}
                  {p.categoria && <span className="chip-categoria" style={{ marginBottom: '8px' }}>{p.categoria}</span>}
                  <h3 style={{ margin: '8px 0 4px', fontSize: '16px' }}>{p.marca} {p.modelo}</h3>
                  {p.sku && <p style={{ margin: '0 0 12px', fontSize: '11px', color: 'var(--color-text-muted)' }}>Cód. {p.sku}</p>}

                  {p.variantes.map(v => (
                    <div key={v.id} style={{ borderTop: '1px solid var(--color-border)', paddingTop: '10px', marginTop: '10px' }}>
                      {v.especificaciones && (
                        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '6px' }}>{v.especificaciones}</p>
                      )}
                      <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', flexWrap: 'wrap' }}>
                        <strong style={{ fontSize: '20px', color: 'var(--color-text)' }}>US$ {formatNumber(v.precioUsd)}</strong>
                        <span style={{ color: 'var(--color-price)', fontSize: '14px', fontWeight: 600 }}>$ {formatNumber(v.precioArs)}</span>
                      </div>
                    </div>
                  ))}

                  <div style={{ marginTop: 'auto', paddingTop: '12px' }}>
                    <p style={{ fontSize: '11px', color: 'var(--color-price)' }}>● Stock sujeto a disponibilidad</p>
                    {p.ultimaActualizacion && (
                      <p style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                        Actualizado: {formatFecha(p.ultimaActualizacion)}
                      </p>
                    )}

                    {/* Con una sola variante se agrega desde acá; con varias hay que elegir cuál,
                        así que la card lleva al detalle en vez de meter una al azar.
                        preventDefault: el botón vive dentro del <Link> de la card. */}
                    {p.variantes.length === 1 ? (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.preventDefault()
                          e.stopPropagation()
                          agregar(p, p.variantes[0])
                          setAgregado(p.id)
                        }}
                        style={{
                          width: '100%', marginTop: '12px', padding: '10px', borderRadius: '8px',
                          border: agregado === p.id ? '1px solid var(--color-lime)' : 'none',
                          background: agregado === p.id ? 'var(--color-lime-tint)' : 'var(--color-lime)',
                          color: agregado === p.id ? 'var(--color-lime)' : '#16181d',
                          fontSize: '14px', fontWeight: 600, cursor: 'pointer'
                        }}
                      >
                        {agregado === p.id ? '✓ En el carrito' : '🛒 Agregar al carrito'}
                      </button>
                    ) : (
                      <div
                        style={{
                          width: '100%', marginTop: '12px', padding: '10px', borderRadius: '8px',
                          border: '1px solid var(--color-border)', background: 'transparent',
                          color: 'var(--color-accent)', fontSize: '14px', fontWeight: 600, textAlign: 'center'
                        }}
                      >
                        Ver {p.variantes.length} opciones →
                      </div>
                    )}
                  </div>
                </Link>
              ))}
            </div>
          )}

          {totalPaginas > 1 && (
            <nav
              aria-label="Paginación del catálogo"
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', flexWrap: 'wrap', marginTop: '28px' }}
            >
              <button
                type="button"
                onClick={() => irAPagina(paginaActual - 1)}
                disabled={paginaActual === 1}
                style={botonPagina(false, paginaActual === 1)}
              >
                ← Anterior
              </button>

              {paginasVisibles(paginaActual, totalPaginas).map((n, i) => (
                n === '…' ? (
                  <span key={`sep-${i}`} style={{ color: 'var(--color-text-muted)', padding: '0 4px' }}>…</span>
                ) : (
                  <button
                    key={n}
                    type="button"
                    onClick={() => irAPagina(n)}
                    aria-current={n === paginaActual ? 'page' : undefined}
                    style={botonPagina(n === paginaActual, false)}
                  >
                    {n}
                  </button>
                )
              ))}

              <button
                type="button"
                onClick={() => irAPagina(paginaActual + 1)}
                disabled={paginaActual === totalPaginas}
                style={botonPagina(false, paginaActual === totalPaginas)}
              >
                Siguiente →
              </button>
            </nav>
          )}
        </div>
      </div>
    </div>
  )
}

export default CatalogPage
