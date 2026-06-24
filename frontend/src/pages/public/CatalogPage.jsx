import { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : null

const formatFechaLarga = (isoDate) => {
  if (!isoDate) return null
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
}

// Precio "desde" del producto: el menor precio USD entre sus variantes (para ordenar/filtrar).
const precioDesde = (p) => {
  const precios = (p.variantes || []).map(v => Number(v.precioUsd)).filter(n => n > 0)
  return precios.length ? Math.min(...precios) : Infinity
}

function CatalogPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [busqueda, setBusqueda] = useState('')
  const [categoria, setCategoria] = useState('')
  const [marca, setMarca] = useState('')
  const [orden, setOrden] = useState('')          // '', 'precio-asc', 'precio-desc'
  const [precioMin, setPrecioMin] = useState('')
  const [precioMax, setPrecioMax] = useState('')
  const [eta, setEta] = useState(null)
  const [cotizacion, setCotizacion] = useState(null)

  useEffect(() => {
    axios.get('/api/productos')
      .then(res => setProductos(res.data))
      .catch(err => {
        console.error('Error al cargar catálogo:', err)
        setError('No se pudo cargar el catálogo. ¿Está corriendo el backend?')
      })
      .finally(() => setCargando(false))
    axios.get('/api/eta').then(res => setEta(res.data)).catch(err => console.error('ETA:', err))
    axios.get('/api/cotizacion').then(res => setCotizacion(res.data)).catch(err => console.error('Cotización:', err))
  }, [])

  const categorias = useMemo(
    () => [...new Set(productos.map(p => p.categoria).filter(Boolean))].sort(),
    [productos]
  )
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
      if (categoria && p.categoria !== categoria) return false
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
  }, [productos, busqueda, categoria, marca, orden, precioMin, precioMax])

  const limpiarTodo = () => {
    setCategoria(''); setMarca(''); setBusqueda(''); setOrden(''); setPrecioMin(''); setPrecioMax('')
  }
  const hayFiltros = categoria || marca || busqueda || orden || precioMin || precioMax

  if (cargando) return (<div><h1>Catálogo</h1><div className="card"><p>Cargando productos...</p></div></div>)
  if (error) return (<div><h1>Catálogo</h1><div className="card" style={{ color: 'var(--color-danger)' }}>{error}</div></div>)
  if (productos.length === 0) return (<div><h1>Catálogo</h1><div className="card"><p>Todavía no hay productos cargados.</p></div></div>)

  const chip = (activo) => ({
    padding: '6px 14px',
    borderRadius: '999px',
    border: '1px solid ' + (activo ? 'var(--color-lime-dark)' : 'var(--color-border)'),
    background: activo ? 'var(--color-lime)' : '#fff',
    color: activo ? '#16181d' : '#475569',
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: activo ? 600 : 500,
    whiteSpace: 'nowrap',
    transition: 'all 0.12s'
  })
  const inputFiltro = {
    padding: '9px 12px', fontSize: '14px', border: '1px solid var(--color-border)',
    borderRadius: '8px', color: 'var(--color-text)', background: '#fff'
  }

  return (
    <div>
      <h1 style={{ marginBottom: '6px' }}>Catálogo</h1>

      {eta?.fechaEntrega && (
        <div style={{
          background: 'var(--color-accent-light)', border: '1px solid var(--color-border)', borderRadius: '12px',
          padding: '12px 16px', margin: '18px 0 20px', fontSize: '14px', color: '#424245'
        }}>
          🚚 Comprando hoy, tu pedido llega aprox. el <strong style={{ color: '#1d1d1f' }}>{formatFechaLarga(eta.fechaEntrega)}</strong> ({eta.diasHabiles} días hábiles).
        </div>
      )}

      {/* Barra de filtros */}
      <div className="card" style={{ marginBottom: '18px' }}>
        <input
          type="text"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          placeholder="🔍 Buscar por marca, modelo o características..."
          style={{ ...inputFiltro, width: '100%', marginBottom: '16px' }}
        />

        {categorias.length > 0 && (
          <div style={{ marginBottom: '12px' }}>
            <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>Categoría</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
              <button style={chip(categoria === '')} onClick={() => setCategoria('')}>Todas</button>
              {categorias.map(c => <button key={c} style={chip(categoria === c)} onClick={() => setCategoria(c)}>{c}</button>)}
            </div>
          </div>
        )}

        {marcas.length > 1 && (
          <div style={{ marginBottom: '14px' }}>
            <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>Marca</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
              <button style={chip(marca === '')} onClick={() => setMarca('')}>Todas</button>
              {marcas.map(m => <button key={m} style={chip(marca === m)} onClick={() => setMarca(m)}>{m}</button>)}
            </div>
          </div>
        )}

        {/* Orden + rango de precio */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'flex-end', borderTop: '1px solid var(--color-border)', paddingTop: '14px' }}>
          <div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>Ordenar por</div>
            <select value={orden} onChange={e => setOrden(e.target.value)} style={inputFiltro}>
              <option value="">Relevancia</option>
              <option value="precio-asc">Precio: menor a mayor</option>
              <option value="precio-desc">Precio: mayor a menor</option>
            </select>
          </div>
          <div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '6px', fontWeight: 600 }}>
              Precio en US$
              {rangoPrecios && (
                <span style={{ fontWeight: 400, color: '#9ca3af' }}> (entre {rangoPrecios.min} y {rangoPrecios.max})</span>
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
        Mostrando <strong>{filtrados.length}</strong> de {productos.length} producto(s)
      </p>
      <p style={{ color: '#9ca3af', fontSize: '12px', marginBottom: '22px' }}>
        Las imágenes son meramente ilustrativas · Stock sujeto a disponibilidad
        {cotizacion?.valor && <> · 💵 Precios calculados al {cotizacion.fuente} ${formatNumber(cotizacion.valor)}</>}
      </p>

      {filtrados.length === 0 ? (
        <div className="card"><p>No hay productos que coincidan con los filtros.</p></div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(260px, 100%), 1fr))', gap: '20px' }}>
          {filtrados.map(p => (
            <Link key={p.id} to={`/producto/${p.id}`} className="producto-card">
              {p.imagenUrl ? (
                <img
                  src={p.imagenUrl}
                  alt={`${p.marca} ${p.modelo}`}
                  style={{ width: '100%', height: '180px', objectFit: 'contain', marginBottom: '14px', background: '#fff' }}
                  onError={(e) => { e.target.style.display = 'none' }}
                />
              ) : (
                <div style={{
                  width: '100%', height: '180px', marginBottom: '14px', background: '#f1f5f9',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#cbd5e1', fontSize: '13px', borderRadius: '8px'
                }}>Sin imagen</div>
              )}
              {p.categoria && <span className="chip-categoria" style={{ marginBottom: '8px' }}>{p.categoria}</span>}
              <h3 style={{ margin: '8px 0 12px', fontSize: '16px' }}>{p.marca} {p.modelo}</h3>

              {p.variantes.map(v => (
                <div key={v.id} style={{ borderTop: '1px solid #f1f5f9', paddingTop: '10px', marginTop: '10px' }}>
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
                  <p style={{ fontSize: '11px', color: '#aaa', marginTop: '2px' }}>
                    Actualizado: {formatFecha(p.ultimaActualizacion)}
                  </p>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

export default CatalogPage
