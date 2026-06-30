import { useState } from 'react'
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './AdminLayout.css'

function AdminLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [menuAbierto, setMenuAbierto] = useState(false)

  const isActive = (path) => location.pathname === path ? 'active' : ''

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const cerrarMenu = () => setMenuAbierto(false)

  return (
    <div className="app-container">
      {/* Barra superior (solo mobile) */}
      <div className="admin-topbar">
        <Link to="/admin" onClick={cerrarMenu}><img src="/logo.png?v=2" alt="FuturaTecno" /></Link>
        <button className="hamburger" onClick={() => setMenuAbierto(true)} aria-label="Abrir menú">☰</button>
      </div>

      {menuAbierto && <div className="sidebar-backdrop" onClick={cerrarMenu} />}

      <aside className={`sidebar${menuAbierto ? ' abierto' : ''}`}>
        <Link to="/admin" onClick={cerrarMenu}><img src="/logo.png?v=2" alt="FuturaTecno" className="logo-admin" /></Link>
        <h3>Panel Admin</h3>
        <nav onClick={cerrarMenu}>
          <Link to="/admin" className={`nav-link ${isActive('/admin')}`}>Dashboard</Link>
          <Link to="/admin/proveedores" className={`nav-link ${isActive('/admin/proveedores')}`}>Proveedores</Link>
          <Link to="/admin/parsing" className={`nav-link ${isActive('/admin/parsing')}`}>Parsing IA</Link>
          <Link to="/admin/importar-elit" className={`nav-link ${isActive('/admin/importar-elit')}`}>Importar de Elit</Link>
          <Link to="/admin/productos" className={`nav-link ${isActive('/admin/productos')}`}>Productos</Link>
          <Link to="/admin/imagenes" className={`nav-link ${isActive('/admin/imagenes')}`}>Imágenes</Link>
          <Link to="/admin/usuarios" className={`nav-link ${isActive('/admin/usuarios')}`}>Usuarios</Link>
          <Link to="/" className="nav-link">Ver Catálogo</Link>
        </nav>

        <div style={{ marginTop: '30px', paddingTop: '16px', borderTop: '1px solid #2c2f38' }}>
          <p style={{ color: '#9a9d92', fontSize: '12px', marginBottom: '10px', wordBreak: 'break-all' }}>{user?.email}</p>
          <button onClick={handleLogout} className="btn btn-secondary" style={{ width: '100%', fontSize: '13px', padding: '9px' }}>
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}

export default AdminLayout
