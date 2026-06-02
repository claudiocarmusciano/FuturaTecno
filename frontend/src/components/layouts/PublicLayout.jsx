import { Outlet, Link } from 'react-router-dom'
import './PublicLayout.css'

function PublicLayout() {
  return (
    <div className="public-layout">
      <header className="public-header">
        <div className="header-container">
          <Link to="/" className="logo">FuturaTecno</Link>
          <nav className="public-nav">
            <Link to="/">Catálogo</Link>
            <Link to="/admin">Panel Admin</Link>
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
