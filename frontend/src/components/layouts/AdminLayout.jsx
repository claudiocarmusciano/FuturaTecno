import { Outlet, Link, useLocation } from 'react-router-dom'
import './AdminLayout.css'

function AdminLayout() {
  const location = useLocation()

  const isActive = (path) => location.pathname === path ? 'active' : ''

  return (
    <div className="app-container">
      <aside className="sidebar">
        <h2>FuturaTecno</h2>
        <h3>Panel Admin</h3>
        <nav>
          <Link to="/admin" className={`nav-link ${isActive('/admin')}`}>
            Dashboard
          </Link>
          <Link to="/admin/proveedores" className={`nav-link ${isActive('/admin/proveedores')}`}>
            Proveedores
          </Link>
          <Link to="/admin/parsing" className={`nav-link ${isActive('/admin/parsing')}`}>
            Parsing IA
          </Link>
          <Link to="/admin/imagenes" className={`nav-link ${isActive('/admin/imagenes')}`}>
            Imágenes
          </Link>
          <Link to="/" className="nav-link">
            Ver Catálogo
          </Link>
        </nav>
      </aside>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}

export default AdminLayout
