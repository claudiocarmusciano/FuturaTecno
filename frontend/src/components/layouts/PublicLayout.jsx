import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './PublicLayout.css'

function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="public-layout">
      <header className="public-header">
        <div className="header-container">
          <Link to="/" className="logo"><img src="/logo.png" alt="FuturaTecno" style={{ height: '34px', width: 'auto', display: 'block' }} /></Link>
          <nav className="public-nav">
            <Link to="/">Catálogo</Link>
            {isAdmin && <Link to="/admin">Panel Admin</Link>}
            {user ? (
              <>
                <span style={{ color: '#888', fontSize: '14px' }}>Hola, {user.nombre || user.email}</span>
                <a onClick={handleLogout} style={{ cursor: 'pointer' }}>Salir</a>
              </>
            ) : (
              <>
                <Link to="/login">Ingresar</Link>
                <Link to="/registro">Registrarse</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="public-main">
        <Outlet />
      </main>
    </div>
  )
}

export default PublicLayout
