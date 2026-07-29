import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useCart } from '../../cart/CartContext'
import CartBadge from '../CartBadge'
import './PublicLayout.css'

function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const { cantidadTotal } = useCart()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="public-layout">
      <header className="public-header">
        <div className="header-container">
          <Link to="/" className="logo"><img src="/logo.png?v=2" alt="FuturaTecno" className="header-logo" /></Link>
          <nav className="public-nav">
            <Link to="/catalogo">Catálogo</Link>
            {isAdmin && <Link to="/admin">Panel Admin</Link>}
            {user ? (
              <>
                <Link to="/mis-pedidos">Mis pedidos</Link>
                <span style={{ color: '#9a9d92', fontSize: '14px' }}>Hola, {user.nombre || user.email}</span>
                <a onClick={handleLogout} style={{ cursor: 'pointer' }}>Salir</a>
              </>
            ) : (
              <>
                <Link to="/login">Ingresar</Link>
                <Link to="/registro">Registrarse</Link>
              </>
            )}
            <CartBadge cantidad={cantidadTotal} />
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
