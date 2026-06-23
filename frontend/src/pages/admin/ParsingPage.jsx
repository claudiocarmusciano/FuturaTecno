import { useState, useEffect } from 'react'
import axios from 'axios'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// Calcula los precios derivados (USD y ARS) a partir del precio de origen, su moneda y la cotización.
const derivarPrecios = (precio, moneda, cotizacion) => {
  const p = Number(precio)
  if (!p || !cotizacion) return { usd: null, ars: null }
  if (moneda === 'USD') return { usd: p, ars: p * cotizacion }
  return { usd: p / cotizacion, ars: p }
}

function ParsingPage() {
  const [textoWhatsapp, setTextoWhatsapp] = useState('')
  const [proveedorId, setProveedorId] = useState('')
  const [preview, setPreview] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [cargando, setCargando] = useState(false)
  const [importando, setImportando] = useState(false)
  const [cotizacion, setCotizacion] = useState(null)
  const [mensaje, setMensaje] = useState('')
  const [resultado, setResultado] = useState('')

  useEffect(() => {
    cargarProveedores()
  }, [])

  const cargarProveedores = async () => {
    try {
      const response = await axios.get('/api/admin/proveedores')
      setProveedores(response.data)
    } catch (error) {
      console.error('Error al cargar proveedores:', error)
    }
  }

  const handlePreview = async () => {
    if (!textoWhatsapp || !proveedorId) {
      alert('Seleccioná un proveedor y pegá el texto de WhatsApp')
      return
    }

    setCargando(true)
    setMensaje('')
    setResultado('')
    try {
      const response = await axios.post('/api/admin/parsing', {
        texto: textoWhatsapp,
        proveedorId: parseInt(proveedorId)
      })
      const cot = response.data.cotizacionUsdArs || null
      // Normalizo cada producto a una fila editable: precio de origen + moneda de origen.
      const items = (response.data.productos || []).map(p => ({
        categoria: p.categoria || '',
        marca: p.marca || '',
        modelo: p.modelo || '',
        especificaciones: p.especificaciones || '',
        moneda: p.monedaOrigen === 'USD' ? 'USD' : 'ARS',
        precio: p.monedaOrigen === 'USD' ? p.precioUsd : p.precioArs
      }))
      setPreview(items)
      setCotizacion(cot)
      setMensaje(response.data.mensaje || '')
    } catch (error) {
      console.error('Error al parsear:', error)
      setPreview([])
      setMensaje('Error de conexión con el backend. ¿Está corriendo en el puerto 8080?')
    } finally {
      setCargando(false)
    }
  }

  const updateItem = (idx, campo, valor) => {
    setPreview(prev => prev.map((it, i) => (i === idx ? { ...it, [campo]: valor } : it)))
  }

  const removeItem = (idx) => {
    setPreview(prev => prev.filter((_, i) => i !== idx))
  }

  const handleConfirm = async () => {
    if (preview.length === 0) return
    setImportando(true)
    setResultado('')
    try {
      const response = await axios.post('/api/admin/parsing/confirmar', {
        proveedorId: parseInt(proveedorId),
        productos: preview.map(it => ({
          categoria: it.categoria || null,
          marca: it.marca,
          modelo: it.modelo,
          especificaciones: it.especificaciones || null,
          moneda: it.moneda,
          precio: it.precio != null && it.precio !== '' ? Number(it.precio) : null
        }))
      })
      setResultado(response.data.mensaje || 'Importación completada')
      setPreview([])
      setMensaje('')
    } catch (error) {
      console.error('Error al importar:', error)
      setResultado('Error al importar: ' + (error.response?.data?.mensaje || error.message))
    } finally {
      setImportando(false)
    }
  }

  const inputStyle = {
    width: '100%',
    padding: '4px 6px',
    border: '1px solid #ccc',
    borderRadius: '3px',
    fontSize: '13px',
    color: '#1a1a1a'
  }

  return (
    <div>
      <h1>Parsing de Listas de Precios</h1>

      <div className="card">
        <h2>Pegar lista de WhatsApp</h2>
        <div className="form-group">
          <label>Proveedor</label>
          <select
            value={proveedorId}
            onChange={(e) => setProveedorId(e.target.value)}
          >
            <option value="">Seleccionar proveedor</option>
            {proveedores.map(p => (
              <option key={p.id} value={p.id}>{p.nombre}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Texto de WhatsApp</label>
          <textarea
            value={textoWhatsapp}
            onChange={(e) => setTextoWhatsapp(e.target.value)}
            rows="10"
            placeholder="Pega aquí el texto del proveedor..."
          />
        </div>
        <button onClick={handlePreview} className="btn btn-primary" disabled={cargando}>
          {cargando ? 'Procesando...' : 'Previsualizar'}
        </button>
      </div>

      {resultado && (
        <div className="card" style={{ borderLeft: '4px solid #28a745', color: '#155724' }}>
          {resultado}
        </div>
      )}

      {mensaje && (
        <div
          className="card"
          style={{
            borderLeft: `4px solid ${preview.length > 0 ? '#28a745' : '#dc3545'}`,
            color: preview.length > 0 ? '#155724' : '#721c24'
          }}
        >
          {mensaje}
        </div>
      )}

      {preview.length > 0 && (
        <div className="card">
          <h2>Previsualización de Productos</h2>
          <p style={{ marginBottom: '10px', color: '#555' }}>
            {cotizacion && (
              <>Cotización: <strong>dólar oficial ${formatNumber(cotizacion)} ARS/USD</strong>. </>
            )}
            Podés editar cualquier campo antes de confirmar.
          </p>
          <table className="table">
            <thead>
              <tr>
                <th>Categoría</th>
                <th>Marca</th>
                <th>Modelo</th>
                <th>Especificaciones</th>
                <th>Moneda</th>
                <th>Precio</th>
                <th>= USD</th>
                <th>= ARS</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {preview.map((item, idx) => {
                const { usd, ars } = derivarPrecios(item.precio, item.moneda, cotizacion)
                return (
                  <tr key={idx}>
                    <td><input style={inputStyle} value={item.categoria} onChange={(e) => updateItem(idx, 'categoria', e.target.value)} /></td>
                    <td><input style={inputStyle} value={item.marca} onChange={(e) => updateItem(idx, 'marca', e.target.value)} /></td>
                    <td><input style={inputStyle} value={item.modelo} onChange={(e) => updateItem(idx, 'modelo', e.target.value)} /></td>
                    <td><input style={inputStyle} value={item.especificaciones} onChange={(e) => updateItem(idx, 'especificaciones', e.target.value)} /></td>
                    <td>
                      <select style={inputStyle} value={item.moneda} onChange={(e) => updateItem(idx, 'moneda', e.target.value)}>
                        <option value="ARS">ARS</option>
                        <option value="USD">USD</option>
                      </select>
                    </td>
                    <td><input style={{ ...inputStyle, width: '100px' }} type="number" value={item.precio ?? ''} onChange={(e) => updateItem(idx, 'precio', e.target.value)} /></td>
                    <td style={{ whiteSpace: 'nowrap' }}>{usd != null ? `US$ ${formatNumber(usd)}` : '-'}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>{ars != null ? `$ ${formatNumber(ars)}` : '-'}</td>
                    <td>
                      <button
                        onClick={() => removeItem(idx)}
                        className="btn btn-secondary"
                        style={{ padding: '4px 8px', fontSize: '12px' }}
                        title="Quitar de la lista"
                      >✕</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          <button onClick={handleConfirm} className="btn btn-primary" disabled={importando}>
            {importando ? 'Importando...' : `Confirmar e Importar (${preview.length})`}
          </button>
        </div>
      )}
    </div>
  )
}

export default ParsingPage
