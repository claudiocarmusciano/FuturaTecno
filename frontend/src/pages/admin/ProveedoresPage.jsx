import { useState, useEffect } from 'react'
import axios from 'axios'

function ProveedoresPage() {
  const [proveedores, setProveedores] = useState([])
  const [formData, setFormData] = useState({
    nombre: '',
    margenPorcentaje: '',
    costoFleteUsd: ''
  })

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

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await axios.post('/api/admin/proveedores', formData)
      setFormData({ nombre: '', margenPorcentaje: '', costoFleteUsd: '' })
      cargarProveedores()
    } catch (error) {
      console.error('Error al crear proveedor:', error)
    }
  }

  return (
    <div>
      <h1>Gestión de Proveedores</h1>

      <div className="card">
        <h2>Nuevo Proveedor</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Nombre</label>
            <input
              type="text"
              name="nombre"
              value={formData.nombre}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Margen (%)</label>
            <input
              type="number"
              name="margenPorcentaje"
              step="0.01"
              value={formData.margenPorcentaje}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Costo Flete (USD)</label>
            <input
              type="number"
              name="costoFleteUsd"
              step="0.01"
              value={formData.costoFleteUsd}
              onChange={handleChange}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary">Crear</button>
        </form>
      </div>

      <div className="card">
        <h2>Proveedores Existentes</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Margen %</th>
              <th>Costo Flete USD</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {proveedores.map(p => (
              <tr key={p.id}>
                <td>{p.nombre}</td>
                <td>{p.margenPorcentaje}</td>
                <td>${p.costoFleteUsd}</td>
                <td>
                  <button className="btn btn-secondary">Editar</button>
                  <button className="btn btn-secondary">Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default ProveedoresPage
