import { useState, useEffect } from 'react'
import axios from 'axios'

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function UsuariosPage() {
  const [usuarios, setUsuarios] = useState([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    axios.get('/api/admin/usuarios')
      .then(res => setUsuarios(res.data))
      .catch(err => console.error(err))
      .finally(() => setCargando(false))
  }, [])

  const exportarCSV = () => {
    const filas = [['Email', 'Nombre', 'Fecha de registro']]
    usuarios.forEach(u => filas.push([u.email, u.nombre || '', formatFecha(u.fechaRegistro)]))
    const csv = filas.map(f => f.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'usuarios-futuratecno.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div>
      <h1>Usuarios registrados</h1>
      <div className="card">
        <p style={{ marginBottom: '12px' }}>
          <strong>{usuarios.length}</strong> cliente(s) registrado(s).
          {usuarios.length > 0 && (
            <button onClick={exportarCSV} className="btn btn-secondary" style={{ marginLeft: '12px', padding: '4px 12px', fontSize: '12px' }}>
              ⬇ Exportar CSV
            </button>
          )}
        </p>

        {cargando ? (
          <p>Cargando...</p>
        ) : usuarios.length === 0 ? (
          <p>Todavía no hay clientes registrados.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Nombre</th>
                <th>Fecha de registro</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(u => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.nombre || '—'}</td>
                  <td style={{ color: '#888', fontSize: '13px' }}>{formatFecha(u.fechaRegistro)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default UsuariosPage
