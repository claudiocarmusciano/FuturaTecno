import { useState, useEffect, Fragment } from 'react'
import axios from 'axios'
import { Link, useSearchParams } from 'react-router-dom'
import { IconTrash, IconDatabase, IconSearchLine, IconCheck } from '../../components/icons'

const formatUsd = (n) => Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatArs = (n) => Number(n).toLocaleString('es-AR', { maximumFractionDigits: 0 })

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function ImagesPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [buscando, setBuscando] = useState(null)   // 'base' | 'internet' | null
  const [mensaje, setMensaje] = useState('')
  const [edits, setEdits] = useState({}) // { [productoId]: urlEnEdicion }
  const [searchParams, setSearchParams] = useSearchParams()
  const [pagina, setPagina] = useState(1)
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
  // Cambiar el filtro o la búsqueda acorta el listado: seguir en la página 7 dejaría la tabla vacía.
  useEffect(() => { setPagina(1) }, [busqueda, soloSinImagen])

  // Dos búsquedas separadas: la de la base es gratis y recorre todos los productos sin foto de
  // una vez; la de internet va de a 10 (puede gastar crédito) y cada clic sigue con los siguientes.
  const buscarImagenes = async (donde) => {
    setBuscando(donde)
    setMensaje(donde === 'base'
      ? 'Buscando en la base todos los productos sin foto…'
      : 'Buscando en internet 10 productos. Puede demorar un minuto.')
    try {
      const res = await axios.post(`/api/admin/productos/buscar-imagenes-${donde}`)
      setMensaje(res.data.mensaje)
      await cargar()
    } catch (e) {
      console.error(e)
      setMensaje(`Error al buscar imágenes en ${donde === 'base' ? 'la base' : 'internet'}.`)
    } finally {
      setBuscando(null)
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

  // Misma razón que en Productos: pintar las ~4.000 filas de una bloqueaba el hilo principal
  // varios segundos y encolaba una petición de imagen por fila. Acá además cada fila abre un
  // panel de candidatas, así que conviene todavía más tener pocas en pantalla.
  const POR_PAGINA = 50
  const totalPaginas = Math.max(1, Math.ceil(productosFiltrados.length / POR_PAGINA))
  const paginaActual = Math.min(pagina, totalPaginas)
  const productosPagina = productosFiltrados.slice((paginaActual - 1) * POR_PAGINA, paginaActual * POR_PAGINA)

  return (
    <div>
      <h1>Gestión de Imágenes</h1>

      <div className="card">
        <p style={{ marginBottom: '12px' }}>
          <strong>{conImagen}</strong> con imagen · <strong>{sinImagen}</strong> sin imagen · {productos.length} en total
        </p>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button onClick={() => buscarImagenes('base')} className="btn btn-primary" disabled={!!buscando || sinImagen === 0}
            title="Todos los productos sin foto, con fotos que ya están en la base: la memoria de imágenes y otro producto del mismo modelo. No gasta crédito.">
            <IconDatabase /> {buscando === 'base' ? 'Buscando en la base…' : 'Buscar en la base (todos)'}
          </button>
          <button onClick={() => buscarImagenes('internet')} className="btn btn-secondary" disabled={!!buscando || sinImagen === 0}
            title="10 productos por clic en DuckDuckGo y Anthropic (gasta crédito). Cada clic sigue con los 10 siguientes.">
            <IconSearchLine /> {buscando === 'internet' ? 'Buscando en internet…' : 'Buscar en internet (de a 10)'}
          </button>
        </div>
        {mensaje && <p style={{ marginTop: '12px', color: 'var(--color-text)' }}>{mensaje}</p>}
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
          <div>
            <h2 style={{ marginBottom: '4px' }}>Productos</h2>
            {mostrarSoloSinImagen && <p style={{ margin: 0, color: 'var(--color-text-muted)', fontSize: '13px' }}>Mostrando primero los {sinImagen} artículo(s) sin imagen.</p>}
          </div>
          <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
            <button type="button" onClick={() => setSoloSinImagen(true)} className="btn" style={{ padding: '7px 10px', fontSize: '12px', border: soloSinImagen ? '1px solid var(--color-accion)' : '1px solid var(--color-border)', background: soloSinImagen ? 'var(--color-accion)' : 'transparent', color: soloSinImagen ? 'var(--color-sobre-accion)' : 'var(--color-text)' }}>
              Sin imagen ({sinImagen})
            </button>
            <button type="button" onClick={() => setSoloSinImagen(false)} className="btn" style={{ padding: '7px 10px', fontSize: '12px', border: !soloSinImagen ? '1px solid var(--color-accion)' : '1px solid var(--color-border)', background: !soloSinImagen ? 'var(--color-accion)' : 'transparent', color: !soloSinImagen ? 'var(--color-sobre-accion)' : 'var(--color-text)' }}>
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
          style={{ width: '100%', maxWidth: '560px', marginBottom: '16px', padding: '9px 12px', border: '1px solid var(--color-border)', borderRadius: '2px', fontSize: '14px', color: 'var(--color-text)', background: 'var(--color-surface-2)' }}
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
              {productosPagina.map(p => (
                <Fragment key={p.id}>
                <tr>
                  <td>
                    {(() => {
                      const preview = edits[p.id] !== undefined ? edits[p.id] : p.imagenUrl
                      return preview ? (
                        <img src={preview} alt="" loading="lazy" width="50" height="50" style={{ width: '50px', height: '50px', objectFit: 'contain' }}
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
                      style={{ color: 'inherit', textDecoration: 'underline', textDecorationColor: 'var(--color-accion)', textUnderlineOffset: '4px' }}
                    >
                      {[p.categoria, p.marca, p.modelo].filter(Boolean).join(' ')}
                    </Link>
                    {/* El color primero: la foto tiene que ser de ESE color. Varios = la ficha lista
                        los disponibles y no dice cuál es este artículo. */}
                    {p.colores?.length > 0 && (
                      <div style={{ marginTop: '6px', display: 'flex', flexWrap: 'wrap', gap: '6px', alignItems: 'center' }}>
                        <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>{p.colores.length > 1 ? 'Colores:' : 'Color:'}</span>
                        {p.colores.map(c => (
                          <span key={c} style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-sobre-accion)', background: 'var(--color-accion)', borderRadius: '2px', padding: '1px 8px' }}>{c}</span>
                        ))}
                        {p.colores.length > 1 && <span style={{ fontSize: '11px', color: 'var(--color-warning, var(--color-warning))' }}>no dice cuál es este</span>}
                      </div>
                    )}
                    {p.especificaciones && (
                      <div style={{ fontSize: '12px', color: 'var(--color-text)', marginTop: '4px', maxWidth: '420px' }}>{p.especificaciones}</div>
                    )}
                    <div style={{ fontSize: '12px', marginTop: '6px', display: 'flex', flexWrap: 'wrap', gap: '4px 14px' }}>
                      <span><span style={{ color: 'var(--color-text-muted)' }}>Proveedor:</span> {p.proveedor || '—'}</span>
                      <span><span style={{ color: 'var(--color-text-muted)' }}>Costo:</span> {p.costoUsd != null ? `US$ ${formatUsd(p.costoUsd)}` : '—'}</span>
                      <span><span style={{ color: 'var(--color-text-muted)' }}>Venta:</span> {p.ventaUsd != null
                        ? <strong style={{ color: 'var(--color-accion)' }}>US$ {formatUsd(p.ventaUsd)}{p.ventaArs != null && <span style={{ fontWeight: 400, color: 'var(--color-text-muted)' }}> · $ {formatArs(p.ventaArs)}</span>}</strong>
                        : '—'}</span>
                    </div>
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
                    ><IconSearchLine /> Buscar</a>
                    <button
                      onClick={() => buscarSimilares(p.id)}
                      className="btn btn-secondary"
                      style={{ padding: '4px 10px', fontSize: '12px', marginRight: '6px' }}
                      title="Ver fotos que ya están en el catálogo para productos parecidos de esta misma marca. No consulta internet."
                    ><IconDatabase /> De la base</button>
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
                          No hay ningún {p.marca} parecido con foto en el catálogo. Buscá en la web con “Buscar”.
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
                                  background: 'var(--color-surface)', borderRadius: '2px',
                                  border: edits[p.id] === c.url ? '2px solid var(--color-accion)' : '1px solid var(--color-border)'
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
                                  {c.exacto ? <><IconCheck /> mismo modelo</>
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
        {totalPaginas > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', marginTop: '16px', flexWrap: 'wrap' }}>
            <button type="button" className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '13px' }}
                    disabled={paginaActual === 1} onClick={() => setPagina(paginaActual - 1)}>← Anterior</button>
            <span style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
              Página {paginaActual} de {totalPaginas} · mostrando {productosPagina.length} de {productosFiltrados.length}
            </span>
            <button type="button" className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '13px' }}
                    disabled={paginaActual === totalPaginas} onClick={() => setPagina(paginaActual + 1)}>Siguiente →</button>
          </div>
        )}
      </div>
    </div>
  )
}

export default ImagesPage
