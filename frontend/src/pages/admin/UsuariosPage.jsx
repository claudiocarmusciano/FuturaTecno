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

  const validarWhatsapp = async (id) => {
    try {
      const res = await axios.post(`/api/admin/usuarios/${id}/validar-whatsapp`)
      setUsuarios(actuales => actuales.map(u => u.id === id ? res.data : u))
    } catch (err) {
      alert(err.response?.data?.error || 'No se pudo validar el WhatsApp.')
    }
  }

  const exportarCSV = () => {
    const filas = [['Email', 'Nombre', 'Apellido', 'Celular', 'Email activado', 'WhatsApp', 'Código WhatsApp', 'Fecha de registro']]
    usuarios.forEach(u => filas.push([u.email, u.nombre || '', u.apellido || '', u.celular || '', u.emailVerificado ? 'Sí' : 'No', u.whatsappVerificado ? 'Validado' : (u.whatsappAgendado ? 'Pendiente' : 'Sin solicitar'), u.whatsappVerificacionCodigo || '', formatFecha(u.fechaRegistro)]))
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
                <th>Apellido</th>
                <th>Celular</th>
                <th>Validación</th>
                <th>Código WhatsApp</th>
                <th>Fecha de registro</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(u => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.nombre || '—'}</td>
                  <td>{u.apellido || '—'}</td>
                  <td>{u.celular || '—'}</td>
                  <td>{u.whatsappVerificado ? <span style={{ color: 'var(--color-success)' }}>✓ Validado</span> : u.whatsappAgendado ? <button className="btn btn-primary" style={{ padding: '4px 9px', fontSize: '12px' }} onClick={() => validarWhatsapp(u.id)}>Validar</button> : <span style={{ color: 'var(--color-text-muted)' }}>Pendiente</span>}</td>
                  <td><code>{u.whatsappVerificacionCodigo || '—'}</code></td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}>{formatFecha(u.fechaRegistro)}</td>
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
