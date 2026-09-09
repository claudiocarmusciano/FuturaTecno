import { useState, useEffect } from 'react'
import axios from 'axios'
import { Link, useSearchParams } from 'react-router-dom'
import { IconTrash } from '../../components/icons'

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function ImagesPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [buscando, setBuscando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [edits, setEdits] = useState({}) // { [productoId]: urlEnEdicion }
  const [searchParams, setSearchParams] = useSearchParams()
  const busqueda = searchParams.get('busqueda') || ''
  const soloSinImagen = searchParams.get('filtro') !== 'todos'
  const actualizarFiltro = (clave, valor) => {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev)
      if (valor) next.set(clave, valor)
      else next.delete(clave)
      return next
    }, { replace: true })
  }
  const setBusqueda = valor => actualizarFiltro('busqueda', valor)
  const setSoloSinImagen = valor => actualizarFiltro('filtro', valor ? '' : 'todos')

  const cargar = async () => {
    setCargando(true)
    try {
      const res = await axios.get('/api/admin/productos')
      setProductos(res.data)
    } catch (e) {
      console.error(e)
      setMensaje('Error al cargar productos. ¿Backend corriendo?')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => { cargar() }, [])

  const buscarImagenes = async () => {
    setBuscando(true)
    setMensaje('Procesando hasta 5 artículos. Puede demorar unos segundos; el resto quedará para el próximo lote.')
    try {
      const res = await axios.post('/api/admin/productos/buscar-imagenes')
      setMensaje(res.data.mensaje)
      await cargar()
    } catch (e) {
      console.error(e)
      setMensaje('Error al buscar imágenes automáticamente.')
    } finally {
      setBuscando(false)
    }
  }

  const guardarUrl = async (id) => {
    const url = edits[id] ?? ''
    try {
      await axios.put(`/api/admin/productos/${id}/imagen`, { url })
      setEdits(prev => { const c = { ...prev }; delete c[id]; return c })
      await cargar()
    } catch (e) {
      console.error(e)
      setMensaje('Error al guardar la imagen.')
    }
  }

  const eliminarProducto = async (id, nombre) => {
    if (!window.confirm(`¿Eliminar "${nombre}" del catálogo? Esta acción lo quita de la lista y del catálogo público.`)) return
    try {
      await axios.delete(`/api/admin/productos/${id}`)
      await cargar()
    } catch (e) {
      console.error(e)
      setMensaje('Error al eliminar el producto.')
    }
  }

  const tieneImagen = (p) => Boolean(p.imagenUrl && p.imagenUrl.trim())
  const conImagen = productos.filter(tieneImagen).length
  const sinImagen = productos.length - conImagen
  // La prioridad operativa es resolver los faltantes; al no haber ninguno se muestra el listado
  // completo para que la pantalla no quede vacía.
  const mostrarSoloSinImagen = soloSinImagen && sinImagen > 0
  const productosBase = mostrarSoloSinImagen ? productos.filter(p => !tieneImagen(p)) : productos
  const terminoBusqueda = busqueda.trim().toLowerCase()
  const productosFiltrados = terminoBusqueda
    ? productosBase.filter(p => [p.categoria, p.marca, p.modelo, p.proveedor, p.sku, p.especificaciones]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(terminoBusqueda))
    : productosBase

  return (
    <div>
      <h1>Gestión de Imágenes</h1>

      <div className="card">
        <p style={{ marginBottom: '12px' }}>
          <strong>{conImagen}</strong> con imagen · <strong>{sinImagen}</strong> sin imagen · {productos.length} en total
        </p>
        <button onClick={buscarImagenes} className="btn btn-primary" disabled={buscando}>
          {buscando ? 'Buscando hasta 5 imágenes...' : 'Buscar imágenes faltantes (automático)'}
        </button>
        {mensaje && <p style={{ marginTop: '12px', color: 'var(--color-text)' }}>{mensaje}</p>}
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
          <div>
            <h2 style={{ marginBottom: '4px' }}>Productos</h2>
            {mostrarSoloSinImagen && <p style={{ margin: 0, color: 'var(--color-text-muted)', fontSize: '13px' }}>Mostrando primero los {sinImagen} artículo(s) sin imagen.</p>}
          </div>
          <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
            <button type="button" onClick={() => setSoloSinImagen(true)} className="btn" style={{ padding: '7px 10px', fontSize: '12px', border: soloSinImagen ? '1px solid var(--color-lime)' : '1px solid var(--color-border)', background: soloSinImagen ? 'var(--color-lime)' : 'transparent', color: soloSinImagen ? '#16181d' : 'var(--color-text)' }}>
              Sin imagen ({sinImagen})
            </button>
            <button type="button" onClick={() => setSoloSinImagen(false)} className="btn" style={{ padding: '7px 10px', fontSize: '12px', border: !soloSinImagen ? '1px solid var(--color-lime)' : '1px solid var(--color-border)', background: !soloSinImagen ? 'var(--color-lime)' : 'transparent', color: !soloSinImagen ? '#16181d' : 'var(--color-text)' }}>
              Todos ({productos.length})
            </button>
          </div>
        </div>
        <input
          type="search"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          placeholder="Buscar por marca, modelo, categoría, proveedor o SKU..."
          aria-label="Buscar productos"
          style={{ width: '100%', maxWidth: '560px', marginBottom: '16px', padding: '9px 12px', border: '1px solid var(--color-border)', borderRadius: '8px', fontSize: '14px', color: 'var(--color-text)', background: 'var(--color-surface-2)' }}
        />
        {cargando ? (
          <p>Cargando...</p>
        ) : productos.length === 0 ? (
          <p>No hay productos. Importá una lista primero en "Cargar por JSON".</p>
        ) : productosFiltrados.length === 0 ? (
          <p>{mostrarSoloSinImagen ? 'No quedan artículos sin imagen para mostrar.' : `No hay productos que coincidan con “${busqueda}”.`}</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Imagen</th>
                <th>Producto</th>
                <th>URL (cargar/editar manualmente)</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {productosFiltrados.map(p => (
                <tr key={p.id}>
                  <td>
                    {(() => {
                      const preview = edits[p.id] !== undefined ? edits[p.id] : p.imagenUrl
                      return preview ? (
                        <img src={preview} alt="" style={{ width: '50px', height: '50px', objectFit: 'contain' }}
                             onError={(e) => { e.target.style.opacity = '0.2' }}
                             onLoad={(e) => { e.target.style.opacity = '1' }} />
                      ) : (
                        <span style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>—</span>
                      )
                    })()}
                  </td>
                  <td>
                    <Link
                      to={`/admin/productos?${new URLSearchParams({ editar: String(p.id), origen: 'imagenes', busqueda, filtro: soloSinImagen ? 'sin-imagen' : 'todos' })}`}
                      title="Editar producto completo"
                      style={{ color: 'inherit', textDecoration: 'underline', textDecorationColor: 'var(--color-lime)', textUnderlineOffset: '4px' }}
                    >
                      {[p.categoria, p.marca, p.modelo].filter(Boolean).join(' ')}
                    </Link>
                    <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                      Última actualización: {formatFecha(p.ultimaActualizacion)}
                    </div>
                  </td>
                  <td>
                    <input
                      style={{ width: '100%', padding: '4px 6px', border: '1px solid var(--color-border)', borderRadius: '3px', fontSize: '13px', color: 'var(--color-text)' }}
                      value={edits[p.id] ?? p.imagenUrl ?? ''}
                      placeholder="https://..."
                      onChange={(e) => setEdits(prev => ({ ...prev, [p.id]: e.target.value }))}
                    />
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <a
                      href={`https://www.google.com/search?tbm=isch&q=${encodeURIComponent([p.categoria, p.marca, p.modelo, (p.especificaciones || '').replace(/\//g, ' ')].filter(Boolean).join(' ').replace(/\s+/g, ' ').trim())}`}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-secondary"
                      style={{ padding: '4px 10px', fontSize: '12px', marginRight: '6px', textDecoration: 'none', display: 'inline-block' }}
                      title="Abrir Google Imágenes (incluye las especificaciones) — click derecho en la foto → Copiar dirección de imagen → pegar acá"
                    >🔍 Buscar</a>
                    <button
                      onClick={() => guardarUrl(p.id)}
                      className="btn btn-primary"
                      style={{ padding: '4px 10px', fontSize: '12px', marginRight: '6px' }}
                      disabled={edits[p.id] === undefined}
                    >Guardar</button>
                    <button
                      onClick={() => eliminarProducto(p.id, [p.categoria, p.marca, p.modelo].filter(Boolean).join(' '))}
                      className="btn-accion danger"
                      title="Eliminar producto del catálogo"
                    ><IconTrash /> Eliminar</button>
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

export default ImagesPage
