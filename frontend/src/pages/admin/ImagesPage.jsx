import { useState, useEffect } from 'react'
import axios from 'axios'
import { IconTrash } from '../../components/icons'

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function ImagesPage() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [buscando, setBuscando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [edits, setEdits] = useState({}) // { [productoId]: urlEnEdicion }

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
    setMensaje('')
    try {
      const res = await axios.post('/api/admin/productos/buscar-imagenes')
      setMensaje(res.data.mensaje)
      await cargar()
    } catch (e) {
      console.error(e)
      setMensaje('Error al buscar imágenes en Icecat.')
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

  const conImagen = productos.filter(p => p.imagenUrl).length
  const sinImagen = productos.length - conImagen

  return (
    <div>
      <h1>Gestión de Imágenes</h1>

      <div className="card">
        <p style={{ marginBottom: '12px' }}>
          <strong>{conImagen}</strong> con imagen · <strong>{sinImagen}</strong> sin imagen · {productos.length} en total
        </p>
        <button onClick={buscarImagenes} className="btn btn-primary" disabled={buscando}>
          {buscando ? 'Buscando imágenes...' : 'Buscar imágenes faltantes (automático)'}
        </button>
        {mensaje && <p style={{ marginTop: '12px', color: '#333' }}>{mensaje}</p>}
      </div>

      <div className="card">
        <h2>Productos</h2>
        {cargando ? (
          <p>Cargando...</p>
        ) : productos.length === 0 ? (
          <p>No hay productos. Importá una lista primero en "Parsing IA".</p>
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
              {productos.map(p => (
                <tr key={p.id}>
                  <td>
                    {p.imagenUrl ? (
                      <img src={p.imagenUrl} alt="" style={{ width: '50px', height: '50px', objectFit: 'contain' }}
                           onError={(e) => { e.target.style.opacity = '0.2' }} />
                    ) : (
                      <span style={{ color: '#bbb', fontSize: '12px' }}>—</span>
                    )}
                  </td>
                  <td>
                    {[p.categoria, p.marca, p.modelo].filter(Boolean).join(' ')}
                    <div style={{ fontSize: '11px', color: '#999', marginTop: '2px' }}>
                      Última actualización: {formatFecha(p.ultimaActualizacion)}
                    </div>
                  </td>
                  <td>
                    <input
                      style={{ width: '100%', padding: '4px 6px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '13px', color: '#1a1a1a' }}
                      value={edits[p.id] ?? p.imagenUrl ?? ''}
                      placeholder="https://..."
                      onChange={(e) => setEdits(prev => ({ ...prev, [p.id]: e.target.value }))}
                    />
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <a
                      href={`https://www.google.com/search?tbm=isch&q=${encodeURIComponent([p.categoria, p.marca, p.modelo].filter(Boolean).join(' '))}`}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-secondary"
                      style={{ padding: '4px 10px', fontSize: '12px', marginRight: '6px', textDecoration: 'none', display: 'inline-block' }}
                      title="Abrir Google Imágenes para este producto"
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
