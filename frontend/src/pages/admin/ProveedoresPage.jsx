import { useState, useEffect } from 'react'
import axios from 'axios'
import { IconEdit, IconTrash } from '../../components/icons'

const formVacio = { nombre: '', codigo: '', margenPorcentaje: '', fletePorcentaje: '' }

function ProveedoresPage() {
  const [proveedores, setProveedores] = useState([])
  const [formData, setFormData] = useState(formVacio)
  const [editId, setEditId] = useState(null)   // null = creando; id = editando
  const [mensaje, setMensaje] = useState('')

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
    setMensaje('')
    try {
      if (editId) {
        await axios.put(`/api/admin/proveedores/${editId}`, formData)
        setMensaje('Proveedor actualizado ✓')
      } else {
        await axios.post('/api/admin/proveedores', formData)
        setMensaje('Proveedor creado ✓')
      }
      cancelarEdicion()
      cargarProveedores()
    } catch (error) {
      console.error('Error al guardar proveedor:', error)
      const detalle = typeof error.response?.data === 'string' ? error.response.data : null
      setMensaje(detalle || 'Error al guardar el proveedor.')
    }
  }

  const handleEditar = (p) => {
    setEditId(p.id)
    setFormData({
      nombre: p.nombre ?? '',
      codigo: p.codigo ?? '',
      margenPorcentaje: p.margenPorcentaje ?? '',
      fletePorcentaje: p.fletePorcentaje ?? ''
    })
    setMensaje('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const cancelarEdicion = () => {
    setEditId(null)
    setFormData(formVacio)
  }

  const handleEliminar = async (id, nombre) => {
    if (!window.confirm(`¿Eliminar el proveedor "${nombre}"? Sus productos dejarán de mostrarse.`)) return
    try {
      await axios.delete(`/api/admin/proveedores/${id}`)
      if (editId === id) cancelarEdicion()
      setMensaje('Proveedor eliminado.')
      cargarProveedores()
    } catch (error) {
      console.error('Error al eliminar proveedor:', error)
      setMensaje('Error al eliminar el proveedor.')
    }
  }

  return (
    <div>
      <h1>Gestión de Proveedores</h1>

      {mensaje && (() => {
        const esError = /error|ya existe/i.test(mensaje)
        return (
          <div className="card" style={{ borderLeft: `4px solid ${esError ? '#dc3545' : '#28a745'}`, color: esError ? '#721c24' : '#155724' }}>
            {mensaje}
          </div>
        )
      })()}

      <div className="card" style={editId ? { borderLeft: '4px solid var(--color-lime)' } : {}}>
        <h2>{editId ? 'Editar Proveedor' : 'Nuevo Proveedor'}</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Nombre</label>
            <input type="text" name="nombre" value={formData.nombre} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Código</label>
            <input type="text" name="codigo" value={formData.codigo} onChange={handleChange} maxLength={10} placeholder="Ej: INV — se deriva del nombre si lo dejás vacío" />
          </div>
          <div className="form-group">
            <label>Margen (%)</label>
            <input type="number" name="margenPorcentaje" step="0.01" value={formData.margenPorcentaje} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Flete (%)</label>
            <input type="number" name="fletePorcentaje" step="0.01" value={formData.fletePorcentaje} onChange={handleChange} required />
          </div>
          <button type="submit" className="btn btn-primary" style={{ marginRight: '10px' }}>
            {editId ? 'Guardar cambios' : 'Crear'}
          </button>
          {editId && (
            <button type="button" onClick={cancelarEdicion} className="btn btn-secondary">Cancelar</button>
          )}
        </form>
      </div>

      <div className="card">
        <h2>Proveedores Existentes</h2>
        {proveedores.length === 0 ? (
          <p>No hay proveedores cargados.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Código</th>
                <th>Margen %</th>
                <th>Flete %</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {proveedores.map(p => (
                <tr key={p.id}>
                  <td>{p.nombre}</td>
                  <td style={{ color: '#888', fontSize: '13px' }}>{p.codigo}</td>
                  <td>{p.margenPorcentaje}%</td>
                  <td>{p.fletePorcentaje}%</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <button onClick={() => handleEditar(p)} className="btn-accion"><IconEdit /> Editar</button>
                    <button onClick={() => handleEliminar(p.id, p.nombre)} className="btn-accion danger"><IconTrash /> Eliminar</button>
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

export default ProveedoresPage
