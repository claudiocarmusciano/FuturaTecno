import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useCart } from '../../cart/CartContext'
import CartBadge from '../CartBadge'
import './PublicLayout.css'

function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const { cantidadTotal } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const esVistaCatalogo = location.pathname === '/catalogo' || location.pathname.startsWith('/producto/')

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className={`public-layout${esVistaCatalogo ? ' public-layout-catalogo' : ''}`}>
      <header className="public-header">
        <div className="header-container">
          <Link to="/" className="logo"><img src="/logo.png?v=2" alt="FuturaTecno" className="header-logo" /></Link>
          <nav className="public-nav">
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
        <div className="public-image-disclaimer" role="note">
          <span aria-hidden="true">ⓘ</span>
          <span><strong>Imágenes meramente ilustrativas.</strong> Confirmá con Futura Tecno las características, el color y la disponibilidad de la variante antes de finalizar tu compra.</span>
        </div>
        <Outlet />
      </main>
    </div>
  )
}

export default PublicLayout
