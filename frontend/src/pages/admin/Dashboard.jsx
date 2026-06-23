import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatFecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'

function StatCard({ label, valor, sub, to }) {
  const contenido = (
    <div className="card" style={{ marginBottom: 0, height: '100%' }}>
      <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', fontWeight: 500, marginBottom: '8px' }}>{label}</div>
      <div style={{ fontSize: '34px', fontWeight: 600, letterSpacing: '-0.03em' }}>{valor}</div>
      {sub && <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginTop: '6px' }}>{sub}</div>}
    </div>
  )
  return to ? <Link to={to} style={{ textDecoration: 'none', color: 'inherit' }}>{contenido}</Link> : contenido
}

function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState({ productos: [], usuarios: [], proveedores: [], cotizacion: null })
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    Promise.all([
      axios.get('/api/admin/productos').then(r => r.data).catch(() => []),
      axios.get('/api/admin/usuarios').then(r => r.data).catch(() => []),
      axios.get('/api/admin/proveedores').then(r => r.data).catch(() => []),
      axios.get('/api/cotizacion').then(r => r.data).catch(() => null),
    ]).then(([productos, usuarios, proveedores, cotizacion]) => {
      setData({ productos, usuarios, proveedores, cotizacion })
    }).finally(() => setCargando(false))
  }, [])

  const { productos, usuarios, proveedores, cotizacion } = data
  const sinImagen = productos.filter(p => !p.imagenUrl).length

  return (
    <div>
      <h1 style={{ marginBottom: '4px' }}>Hola, {user?.nombre || 'Administrador'} 👋</h1>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: '28px' }}>Resumen de tu tienda</p>

      {cargando ? (
        <div className="card"><p>Cargando métricas...</p></div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '18px', marginBottom: '18px' }}>
            <StatCard label="Productos publicados" valor={productos.length} to="/admin/productos" />
            <StatCard label="Clientes registrados" valor={usuarios.length} sub="Base de emails" to="/admin/usuarios" />
            <StatCard label="Proveedores" valor={proveedores.length} to="/admin/proveedores" />
            <StatCard
              label="Dólar oficial"
              valor={cotizacion?.valor ? `$${formatNumber(cotizacion.valor)}` : '—'}
              sub="Usado para los precios"
            />
          </div>

          {sinImagen > 0 && (
            <Link to="/admin/imagenes" style={{ textDecoration: 'none' }}>
              <div className="card" style={{ marginBottom: '18px', borderLeft: '3px solid var(--color-accent)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'var(--color-text)' }}>
                  🖼️ Tenés <strong>{sinImagen}</strong> producto(s) sin imagen.
                </span>
                <span style={{ color: 'var(--color-accent)', fontSize: '14px', fontWeight: 500 }}>Buscar imágenes →</span>
              </div>
            </Link>
          )}

          <div className="card">
            <h2 style={{ marginBottom: '14px' }}>Últimos clientes registrados</h2>
            {usuarios.length === 0 ? (
              <p style={{ color: 'var(--color-text-muted)' }}>Todavía no hay clientes registrados.</p>
            ) : (
              <table className="table">
                <thead>
                  <tr><th>Email</th><th>Nombre</th><th>Fecha</th></tr>
                </thead>
                <tbody>
                  {usuarios.slice(0, 5).map(u => (
                    <tr key={u.id}>
                      <td>{u.email}</td>
                      <td>{u.nombre || '—'}</td>
                      <td style={{ color: 'var(--color-text-muted)' }}>{formatFecha(u.fechaRegistro)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            {usuarios.length > 5 && (
              <Link to="/admin/usuarios" style={{ fontSize: '14px', display: 'inline-block', marginTop: '12px' }}>Ver todos →</Link>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default Dashboard
