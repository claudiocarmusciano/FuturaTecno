import { useState, useEffect, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'
import { WHATSAPP_NUMBER, NOMBRE_NEGOCIO } from '../../config'
import './Landing.css'

const waLink = `https://wa.me/${WHATSAPP_NUMBER}?text=` +
  encodeURIComponent(`Hola ${NOMBRE_NEGOCIO}, quería hacer una consulta sobre el catálogo.`)

const fmt = (n) => Number(n).toLocaleString('es-AR', { maximumFractionDigits: 0 })

// Precio "desde": el menor precio USD entre las variantes del producto.
const precioDesde = (p) => {
  const vs = (p.variantes || []).map(v => Number(v.precioUsd)).filter(n => n > 0)
  return vs.length ? Math.min(...vs) : null
}
const precioArsDesde = (p) => {
  const min = precioDesde(p)
  const v = (p.variantes || []).find(v => Number(v.precioUsd) === min)
  return v ? Number(v.precioArs) : null
}
const nombreDe = (p) => [p.marca, p.modelo].filter(Boolean).join(' ')
const cortar = (s, n) => (s && s.length > n ? s.slice(0, n) + '…' : s)

// Tarjetas del hero por defecto (si la API todavía no respondió).
const HERO_FALLBACK = [
  { chip: 'Notebooks', nombre: 'ASUS Vivobook Go 14', usd: 519, id: 1650, img: 'https://www.technoworld.com/media/catalog/product/cache/45de336bea698e7537f680c633de112a/e/1/e1404ga-nk002w.jpg' },
  { chip: 'Placas de video', nombre: 'MSI GeForce RTX 3050', usd: 309, id: 909, img: 'https://invidcomputers.com/images/000000000041654277364RTX-3050-LP-6G-OC.png' },
  { chip: 'Monitores', nombre: 'LG UltraGear 27" 240Hz', usd: 400, id: 863, img: 'https://invidcomputers.com/images/000000000041396572263Diseno-sin-titulo--12-.png' },
]

const CATS_FALLBACK = [
  { id: 71, nombre: 'Notebooks' }, { id: 5, nombre: 'Computadoras' },
  { id: 85, nombre: 'Placas de video' }, { id: 64, nombre: 'Monitores' },
  { id: 61, nombre: 'Microprocesadores' }, { id: 55, nombre: 'Memorias RAM' },
  { id: 34, nombre: 'Discos Rígidos / SSD' }, { id: 75, nombre: 'Periféricos' },
  { id: 10, nombre: 'Conectividad' }, { id: 51, nombre: 'Impresoras' }, { id: 93, nombre: 'Tablets' },
]

const FEATURES = [
  { t: 'Precios en USD y pesos', d: 'Cada producto muestra su valor en dólares y en pesos, calculado con la cotización del día.',
    svg: <><line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" /></> },
  { t: 'Envíos a todo el país', d: 'Te lo llevamos a donde estés, con una estimación clara de cuándo llega tu pedido.',
    svg: <><rect x="1" y="3" width="15" height="13" /><polygon points="16 8 20 8 23 11 23 16 16 16 16 8" /><circle cx="5.5" cy="18.5" r="2.5" /><circle cx="18.5" cy="18.5" r="2.5" /></> },
  { t: 'Atención por WhatsApp', d: 'Consultá disponibilidad y comprá hablando con una persona, no con un bot.',
    svg: <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" /> },
  { t: 'Catálogo amplio', d: 'Notebooks, PC, componentes, monitores, periféricos, redes, almacenamiento y más.',
    svg: <><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /></> },
  { t: 'Stock actualizado', d: 'El catálogo se sincroniza a diario para reflejar precios y disponibilidad al día.',
    svg: <><polyline points="23 4 23 10 17 10" /><polyline points="1 20 1 14 7 14" /><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" /></> },
  { t: 'Compra confiable', d: 'Productos identificados con su código y precio claro. Lo que ves es lo que pagás.',
    svg: <><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><polyline points="9 12 11 14 15 10" /></> },
]

const IconWhatsApp = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347M12.05 21.785h-.004a9.87 9.87 0 0 1-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 0 1-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 0 1 2.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0 0 12.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 0 0 5.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 0 0-3.48-8.413" /></svg>
)

function LandingPage() {
  const [productos, setProductos] = useState([])
  const [arbol, setArbol] = useState([])
  const [menuAbierto, setMenuAbierto] = useState(false)
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  useEffect(() => {
    axios.get('/api/productos').then(r => setProductos(r.data)).catch(() => {})
    axios.get('/api/categorias').then(r => setArbol(r.data)).catch(() => {})
  }, [])

  // id de cualquier nodo (hoja incluida) -> id de su categoría de primer nivel.
  const topDe = useMemo(() => {
    const map = {}
    const marcar = (n, topId) => { map[n.id] = topId; (n.hijos || []).forEach(h => marcar(h, topId)) }
    arbol.forEach(top => marcar(top, top.id))
    return map
  }, [arbol])

  const conImagen = useMemo(
    () => productos.filter(p => p.imagenUrl && precioDesde(p)),
    [productos]
  )

  // Chips: solo las categorías con productos, ordenadas por cantidad.
  const categorias = useMemo(() => {
    if (!arbol.length || !productos.length) return CATS_FALLBACK
    const cuenta = {}
    productos.forEach(p => {
      const t = topDe[p.categoriaId]
      if (t) cuenta[t] = (cuenta[t] || 0) + 1
    })
    const conProd = arbol.filter(c => cuenta[c.id])
    return conProd.length ? [...conProd].sort((a, b) => cuenta[b.id] - cuenta[a.id]) : CATS_FALLBACK
  }, [arbol, productos, topDe])

  // Hero: un producto real de cada categoría, elegido por id del árbol (no por nombre).
  const heroCards = useMemo(() => {
    if (!arbol.length || !conImagen.length) return HERO_FALLBACK
    return ['Notebooks', 'Placas de video', 'Monitores'].map((nombreCat, i) => {
      const cat = arbol.find(c => c.nombre === nombreCat)
      const deLaCat = cat ? conImagen.filter(p => topDe[p.categoriaId] === cat.id) : []
      if (!deLaCat.length) return HERO_FALLBACK[i]
      const p = deLaCat[Math.floor(Math.random() * deLaCat.length)]
      return { chip: nombreCat, nombre: cortar(nombreDe(p), 34), usd: precioDesde(p), id: p.id, img: p.imagenUrl }
    })
  }, [arbol, conImagen, topDe])

  const destacados = useMemo(() => {
    if (!conImagen.length) return []
    return [...conImagen].sort(() => Math.random() - 0.5).slice(0, 8)
  }, [conImagen])

  const totalProductos = productos.length ? `+${Math.floor(productos.length / 10) * 10}` : '+600'

  const cerrarMenu = () => setMenuAbierto(false)

  return (
    <div className="lp">
      {/* NAV */}
      <header className="lp-header">
        <div className="lp-wrap lp-nav">
          <a className="lp-nav-logo" href="#top"><img src="/logo.png?v=2" alt="FuturaTecno" /></a>
          <nav className={`lp-nav-links${menuAbierto ? ' abierto' : ''}`} onClick={cerrarMenu}>
            <a href="#por-que">Por qué</a>
            <a href="#categorias">Categorías</a>
            <a href="#productos">Productos</a>
            {isAdmin && <Link to="/admin">Panel Admin</Link>}
            {user ? (
              <>
                <Link to="/mis-pedidos">Mis pedidos</Link>
                {/* max-width + ellipsis: un nombre largo no debe poder romper el layout del nav. */}
                <span style={{ color: 'var(--lp-muted)', fontSize: '14.5px', maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={user.nombre || user.email}>
                  Hola, {user.nombre || user.email}
                </span>
                <a onClick={handleLogout} style={{ cursor: 'pointer' }}>Salir</a>
              </>
            ) : (
              <>
                <Link to="/login">Ingresar</Link>
                <Link to="/registro">Registrarse</Link>
              </>
            )}
            <Link to="/catalogo" className="lp-nav-cta">Ver catálogo →</Link>
          </nav>
          <button className="lp-nav-toggle" aria-label="Abrir menú" onClick={() => setMenuAbierto(v => !v)}>
            <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
        </div>
      </header>

      {/* HERO */}
      <section className="lp-hero" id="top">
        <div className="lp-glow lp-glow-1" />
        <div className="lp-wrap lp-hero-grid">
          <div>
            <span className="lp-badge"><span className="lp-dot" /> Tecnología · Argentina</span>
            <h1 className="lp-hero-title">La tecnología que buscás, <span className="lp-accent">al mejor precio.</span></h1>
            <p className="lp-hero-sub">
              Catálogo de notebooks, PC, componentes, periféricos y más. Precios actualizados en{' '}
              <strong>USD y pesos</strong>, envíos a todo el país y atención directa por WhatsApp.
            </p>
            <div className="lp-cta-row">
              <Link className="lp-btn lp-btn-primary" to="/catalogo">Ver catálogo →</Link>
              <a className="lp-btn lp-btn-wa" href={waLink} target="_blank" rel="noreferrer">
                <IconWhatsApp /> Consultar por WhatsApp
              </a>
            </div>
          </div>
          <div className="lp-mockup">
            {heroCards.map((c, i) => (
              <Link key={i} to={`/producto/${c.id}`} className={`lp-mock-card lp-mc${i + 1}`}>
                <div className="lp-ph">
                  <img src={c.img} alt={c.nombre} loading="lazy" onError={e => { e.target.style.display = 'none' }} />
                </div>
                <span className="lp-chip">{c.chip}</span>
                <div className="lp-name">{c.nombre}</div>
                <div className="lp-price">US$ {fmt(c.usd)} <small>· y en pesos</small></div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* STATS */}
      <div className="lp-strip">
        <div className="lp-wrap lp-stats">
          <div><div className="lp-stat-n">{totalProductos}</div><div className="lp-stat-l">productos en catálogo</div></div>
          <div><div className="lp-stat-n">USD + $</div><div className="lp-stat-l">precio en vivo</div></div>
          <div><div className="lp-stat-n">Todo el país</div><div className="lp-stat-l">envíos a domicilio</div></div>
          <div><div className="lp-stat-n">WhatsApp</div><div className="lp-stat-l">atención directa</div></div>
        </div>
      </div>

      {/* POR QUÉ */}
      <section className="lp-block" id="por-que">
        <div className="lp-wrap">
          <span className="lp-eyebrow">Por qué FuturaTecno</span>
          <h2 className="lp-title">Comprar tecnología, simple y transparente.</h2>
          <p className="lp-lead">Un catálogo claro, precios reales y una atención personalizada del otro lado. Sin vueltas.</p>
          <div className="lp-features">
            {FEATURES.map(f => (
              <div className="lp-feature" key={f.t}>
                <div className="lp-ico"><svg viewBox="0 0 24 24">{f.svg}</svg></div>
                <h3>{f.t}</h3>
                <p>{f.d}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CATEGORÍAS */}
      <section className="lp-block" id="categorias" style={{ paddingTop: 0 }}>
        <div className="lp-wrap">
          <span className="lp-eyebrow">Categorías</span>
          <h2 className="lp-title">Todo lo que necesitás, en un solo lugar.</h2>
          <div className="lp-cats">
            {categorias.map(c => (
              <Link key={c.id} className="lp-cat" to={`/catalogo?cat=${c.id}`}>{c.nombre}</Link>
            ))}
            <Link className="lp-cat" to="/catalogo">Ver todo →</Link>
          </div>
        </div>
      </section>

      {/* PRODUCTOS */}
      <section className="lp-block lp-productos" id="productos">
        <div className="lp-wrap">
          <span className="lp-eyebrow">Destacados</span>
          <h2 className="lp-title">Algunos productos del catálogo</h2>
          <p className="lp-lead">Una muestra en vivo. Entrá al catálogo para ver todo, con precios actualizados.</p>
          <div className="lp-prod-grid">
            {destacados.length === 0 ? (
              <div className="lp-prod-loading">Cargando productos…</div>
            ) : destacados.map(p => {
              const usd = precioDesde(p), ars = precioArsDesde(p)
              return (
                <Link key={p.id} className="lp-prod" to={`/producto/${p.id}`}>
                  <div className="lp-prod-img">
                    <img src={p.imagenUrl} alt={nombreDe(p)} loading="lazy" onError={e => { e.target.style.display = 'none' }} />
                  </div>
                  <div className="lp-prod-body">
                    {p.categoria && <span className="lp-chip">{p.categoria}</span>}
                    <div className="lp-name">{nombreDe(p)}</div>
                    <div className="lp-price">US$ {fmt(usd)}{ars ? <small>$ {fmt(ars)}</small> : null}</div>
                  </div>
                </Link>
              )
            })}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="lp-block" style={{ paddingTop: 0 }}>
        <div className="lp-wrap">
          <div className="lp-cta-band">
            <div className="lp-glow lp-glow-2" />
            <h2>¿Listo para tu próxima compra tech?</h2>
            <p>Explorá el catálogo completo o escribinos por WhatsApp y te ayudamos a elegir.</p>
            <div className="lp-cta-row">
              <Link className="lp-btn lp-btn-primary" to="/catalogo">Ir al catálogo →</Link>
              <a className="lp-btn lp-btn-wa" href={waLink} target="_blank" rel="noreferrer">
                <IconWhatsApp /> Escribir por WhatsApp
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer className="lp-footer">
        <div className="lp-wrap lp-foot">
          <div>© {new Date().getFullYear()} FuturaTecno · Tu tecnología. Tu futuro.</div>
          <div className="lp-foot-links">
            <Link to="/catalogo">Catálogo</Link>
            <a href={waLink} target="_blank" rel="noreferrer">WhatsApp</a>
            {user ? <Link to="/mis-pedidos">Mis pedidos</Link> : <Link to="/login">Ingresar</Link>}
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage
