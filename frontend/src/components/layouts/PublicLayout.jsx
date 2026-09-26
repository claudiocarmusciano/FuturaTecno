import { useEffect, useState } from 'react'
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useCart } from '../../cart/CartContext'
import CartBadge from '../CartBadge'
import { IconInfo, IconMenu, IconX } from '../../components/icons'
import './PublicLayout.css'

function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const { cantidadTotal } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const esVistaCatalogo = location.pathname === '/catalogo' || location.pathname.startsWith('/producto/')
  const [menuAbierto, setMenuAbierto] = useState(false)

  // Al navegar (link del menú, atrás del navegador) el menú mobile se cierra.
  useEffect(() => { setMenuAbierto(false) }, [location.pathname, location.hash])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className={`public-layout${esVistaCatalogo ? ' public-layout-catalogo' : ''}`}>
      <header className="public-header">
        <div className="header-container">
          <Link to="/" className="logo"><img src="/logo.png?v=2" alt="FuturaTecno" className="header-logo" /></Link>
          <nav id="public-nav" className={`public-nav${menuAbierto ? ' abierto' : ''}`}>
            {/* Mismo orden que la barra de la home. */}
            <Link to="/catalogo">Productos</Link>
            <Link to="/#categorias">Categorías</Link>
            <Link to="/arma-tu-pc">Armá tu PC</Link>
            <Link to="/#por-que">Por qué</Link>
            {isAdmin && <Link to="/admin">Panel Admin</Link>}
            {user ? (
              <>
                <Link to="/mis-puntos">Mis puntos</Link>
                <Link to="/mis-pedidos">Mis pedidos</Link>
                <span className="public-nav-saludo" title={user.nombre || user.email}>Hola, {user.nombre || user.email}</span>
                <a onClick={handleLogout} style={{ cursor: 'pointer' }}>Salir</a>
              </>
            ) : (
              <>
                <Link to="/login">Ingresar</Link>
                <Link to="/registro" className="public-nav-cta">Registrarse</Link>
              </>
            )}
          </nav>
          {/* El carrito queda fuera del menú: en mobile tiene que verse sin abrirlo. */}
          <div className="public-header-acciones">
            <CartBadge cantidad={cantidadTotal} />
            <button type="button" className="public-nav-toggle" aria-controls="public-nav" aria-expanded={menuAbierto}
              aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'} onClick={() => setMenuAbierto(v => !v)}>
              {menuAbierto ? <IconX size="26px" /> : <IconMenu size="26px" />}
            </button>
          </div>
        </div>
      </header>
      <main className="public-main">
        <div className="public-image-disclaimer" role="note">
          <IconInfo />
          <span><strong>Imágenes meramente ilustrativas.</strong> Confirmá con Futura Tecno las características, el color y la disponibilidad antes de finalizar tu compra. Debido a la alta rotación de stock, la disponibilidad se confirma al procesar el pedido.</span>
        </div>
        <Outlet />
      </main>
    </div>
  )
}

export default PublicLayout
