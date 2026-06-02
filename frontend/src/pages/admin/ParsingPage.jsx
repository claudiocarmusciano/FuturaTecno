import { useState, useEffect } from 'react'
import axios from 'axios'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function ParsingPage() {
  const [textoWhatsapp, setTextoWhatsapp] = useState('')
  const [proveedorId, setProveedorId] = useState('')
  const [preview, setPreview] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [cargando, setCargando] = useState(false)
  const [cotizacion, setCotizacion] = useState(null)

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
      alert('Por favor completa todos los campos')
      return
    }

    setCargando(true)
    try {
      const response = await axios.post('/api/admin/parsing', {
        texto: textoWhatsapp,
        proveedorId: parseInt(proveedorId)
      })
      setPreview(response.data.productos || [])
      setCotizacion(response.data.cotizacionUsdArs || null)
    } catch (error) {
      console.error('Error al parsear:', error)
      alert('Error al procesar la lista')
    } finally {
      setCargando(false)
    }
  }

  const handleConfirm = async () => {
    // TODO: Implementar confirmación con API para guardar en BD
    console.log('Confirmar parsing')
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

      {preview.length > 0 && (
        <div className="card">
          <h2>Previsualización de Productos</h2>
          {cotizacion && (
            <p style={{ marginBottom: '15px', color: '#555' }}>
              Cotización usada: <strong>dólar oficial ${formatNumber(cotizacion)} ARS/USD</strong>
            </p>
          )}
          <table className="table">
            <thead>
              <tr>
                <th>Marca</th>
                <th>Modelo</th>
                <th>Especificaciones</th>
                <th>Origen</th>
                <th>Precio USD</th>
                <th>Precio ARS</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {preview.map((item, idx) => (
                <tr key={idx}>
                  <td>{item.marca}</td>
                  <td>{item.modelo}</td>
                  <td>{item.especificaciones}</td>
                  <td>{item.monedaOrigen || '-'}</td>
                  <td>{item.precioUsd != null ? `US$ ${formatNumber(item.precioUsd)}` : '-'}</td>
                  <td>{item.precioArs != null ? `$ ${formatNumber(item.precioArs)}` : '-'}</td>
                  <td>{item.estado === 'exitoso' ? '✓' : '✗'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <button onClick={handleConfirm} className="btn btn-primary">
            Confirmar e Importar
          </button>
        </div>
      )}
    </div>
  )
}

export default ParsingPage
