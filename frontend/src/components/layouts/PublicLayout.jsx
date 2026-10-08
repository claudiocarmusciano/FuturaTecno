import { useEffect, useState } from 'react'
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useCart } from '../../cart/CartContext'
import CartBadge from '../CartBadge'
import { IconMenu, IconX } from '../../components/icons'
import './PublicLayout.css'
import PieLegal, { BarraArrepentimiento } from '../PieLegal'

function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const { cantidadTotal } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  // La home ocupa todo el ancho (franjas de color de borde a borde); el resto va en la columna centrada.
  const esHome = location.pathname === '/'
  const [menuAbierto, setMenuAbierto] = useState(false)

  // Al navegar (link del menú, atrás del navegador) el menú mobile se cierra.
  useEffect(() => { setMenuAbierto(false) }, [location.pathname, location.hash])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="public-layout">
      <BarraArrepentimiento />
      <header className="public-header">
        <div className="header-container">
          <Link to="/" className="logo"><img src="/marca/tecnopolis-olavarria-logo-sobre-blanco.svg" alt="Tecnópolis Olavarría" className="header-logo" width="176" height="44" /></Link>
          <nav id="public-nav" className={`public-nav${menuAbierto ? ' abierto' : ''}`}>
            {/* Mismo orden que la barra de la home. */}
            <Link to="/catalogo">Productos</Link>
            <Link to="/#categorias">Categorías</Link>
            <Link to="/arma-tu-pc">Armá tu PC</Link>
                        {isAdmin && <Link to="/admin">Panel Admin</Link>}
            {user ? (
              <>
                <span className="public-nav-saludo public-nav-cuenta" title={user.nombre || user.email}>Hola, {user.nombre || user.email}</span>
                <Link to="/mis-pedidos">Mis pedidos</Link>
                <Link to="/mis-puntos">Mis puntos</Link>
                <a onClick={handleLogout} style={{ cursor: 'pointer' }}>Salir</a>
              </>
            ) : (
              <>
                <Link to="/login" className="public-nav-cuenta">Ingresar</Link>
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
      <main className={esHome ? 'public-main-home' : 'public-main'}>
        <Outlet />
      </main>
      <PieLegal />
    </div>
  )
}

export default PublicLayout
