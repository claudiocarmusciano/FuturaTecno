import { useState, useEffect, Fragment } from 'react'
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

  // Candidatas traídas de la propia base, por producto: { [id]: {cargando, items, error} }.
  // Es el paso barato antes de salir a la web: no gasta cuota de ningún proveedor y suele
  // resolverlo, porque el catálogo ya tiene otra variante del mismo equipo cargada con foto.
  const [similares, setSimilares] = useState({})
  // URLs cuya <img> no cargó. El servidor ya no valida: pedirlas desde Railway descartaba todas
  // las de gstatic, que Google no sirve a IPs de datacenter aunque anden bien en un navegador.
  // Acá la prueba es real, porque es el mismo contexto donde la foto se va a ver.
  const [rotas, setRotas] = useState(new Set())
  const marcarRota = url => setRotas(prev => prev.has(url) ? prev : new Set(prev).add(url))

  const buscarSimilares = async (id) => {
    if (similares[id]) { setSimilares(prev => { const c = { ...prev }; delete c[id]; return c }); return }
    setSimilares(prev => ({ ...prev, [id]: { cargando: true, items: [] } }))
    try {
      const res = await axios.get(`/api/admin/productos/${id}/imagenes-similares`)
      setSimilares(prev => ({ ...prev, [id]: { cargando: false, items: res.data } }))
    } catch (e) {
      console.error(e)
      setSimilares(prev => ({ ...prev, [id]: { cargando: false, items: [], error: true } }))
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
                <Fragment key={p.id}>
                <tr>
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
                      onClick={() => buscarSimilares(p.id)}
                      className="btn btn-secondary"
                      style={{ padding: '4px 10px', fontSize: '12px', marginRight: '6px' }}
                      title="Ver fotos que ya están en el catálogo para productos parecidos de esta misma marca. No consulta internet."
                    >📁 De la base</button>
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
                {similares[p.id] && (
                  <tr>
                    <td colSpan={4} style={{ background: 'var(--color-surface-2)' }}>
                      {similares[p.id].cargando ? (
                        <p style={{ margin: 0, fontSize: '13px' }}>Buscando en el catálogo...</p>
                      ) : similares[p.id].error ? (
                        <p style={{ margin: 0, fontSize: '13px', color: '#dc3545' }}>No se pudieron traer las candidatas.</p>
                      ) : similares[p.id].items.length === 0 ? (
                        <p style={{ margin: 0, fontSize: '13px', color: 'var(--color-text-muted)' }}>
                          No hay ningún {p.marca} parecido con foto en el catálogo. Buscá en la web con “🔍 Buscar”.
                        </p>
                      ) : (
                        <>
                          <p style={{ margin: '0 0 8px', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                            Fotos de otros {p.marca} del catálogo. <strong>Mirá el modelo debajo de cada una</strong>: la
                            que elijas queda recordada para esta marca+modelo y se reusa en cada carga futura.
                          </p>
                          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                            {similares[p.id].items.map(c => {
                              const rota = rotas.has(c.url)
                              return (
                              <button
                                key={c.productoId}
                                type="button"
                                disabled={rota}
                                onClick={() => setEdits(prev => ({ ...prev, [p.id]: c.url }))}
                                title={rota ? 'Esta foto ya no carga' : `${c.marca} ${c.modelo}`}
                                style={{
                                  width: '132px', padding: '6px', textAlign: 'left',
                                  cursor: rota ? 'not-allowed' : 'pointer', opacity: rota ? 0.45 : 1,
                                  background: 'var(--color-surface)', borderRadius: '6px',
                                  border: edits[p.id] === c.url ? '2px solid var(--color-lime)' : '1px solid var(--color-border)'
                                }}
                              >
                                {rota ? (
                                  <div style={{ height: '78px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '11px', color: '#dc3545', textAlign: 'center' }}>
                                    foto rota
                                  </div>
                                ) : (
                                  <img src={c.url} alt="" onError={() => marcarRota(c.url)}
                                       style={{ width: '100%', height: '78px', objectFit: 'contain' }} />
                                )}
                                <div style={{ fontSize: '11px', color: 'var(--color-text)', marginTop: '4px', lineHeight: 1.25 }}>
                                  {c.modelo}
                                </div>
                                <div style={{ fontSize: '10px', marginTop: '3px', color: 'var(--color-text-muted)' }}>
                                  {c.exacto ? '✓ mismo modelo'
                                    : c.afinidad >= 12 ? 'parecido'
                                    : 'apenas parecido — verificá'}
                                  {!c.activo && ' · dado de baja'}
                                </div>
                              </button>
                            )})}
                          </div>
                          <p style={{ margin: '8px 0 0', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                            Al elegir una queda cargada en el campo URL; revisá la vista previa y apretá “Guardar”.
                          </p>
                        </>
                      )}
                    </td>
                  </tr>
                )}
                </Fragment>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default ImagesPage
