import { useState, useEffect, useMemo } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'
import { WHATSAPP_NUMBER, NOMBRE_NEGOCIO } from '../../config'
import PieLegal, { BarraArrepentimiento } from '../../components/PieLegal'
import './Landing.css'
import PromotionsCarousel from '../../components/PromotionsCarousel'

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

// La manzana no se puede armar con texto como las demás: es el SVG de Diseño/Base/LogosMarcas.
const LOGO_APPLE = (
  <svg viewBox="0 0 814 1000" role="img" aria-label="Apple">
    <path d="M788.1 340.9c-5.8 4.5-108.2 62.2-108.2 190.5 0 148.4 130.3 200.9 134.2 202.2-.6 3.2-20.7 71.9-68.7 141.9-42.8 61.6-87.5 123.1-155.5 123.1s-85.5-39.5-164-39.5c-76.5 0-103.7 40.8-165.9 40.8s-105.6-57-155.5-127C46.7 790.7 0 663 0 541.8c0-194.4 126.4-297.5 250.8-297.5 66.1 0 121.2 43.4 162.7 43.4 39.5 0 101.1-46 176.3-46 28.5 0 130.9 2.6 198.3 99.2zm-234-181.5c31.1-36.9 53.1-88.1 53.1-139.3 0-7.1-.6-14.3-1.9-20.1-50.6 1.9-110.8 33.7-147.1 75.8-28.5 32.4-55.1 83.6-55.1 135.5 0 7.8 1.3 15.6 1.9 18.1 3.2.6 8.4 1.3 13.6 1.3 45.4 0 102.5-30.4 135.5-71.3z" />
  </svg>
)

const BRANDS = [
  { key: 'apple', name: 'Apple', mark: LOGO_APPLE },
  { key: 'hp', name: 'HP', mark: 'hp' },
  { key: 'samsung', name: 'Samsung', mark: 'SAMSUNG' },
  { key: 'razer', name: 'Razer', mark: 'RAZER' },
  { key: 'amd', name: 'AMD', mark: 'AMD' },
  { key: 'sony', name: 'Sony', mark: 'SONY' },
  { key: 'intel', name: 'Intel', mark: 'intel.' },
  { key: 'hyperx', name: 'HyperX', mark: 'HyperX' },
  { key: 'motorola', name: 'Motorola', mark: 'motorola' },
  { key: 'corsair', name: 'Corsair', mark: 'CORSAIR' },
  { key: 'jbl', name: 'JBL', mark: 'JBL' },
  { key: 'lenovo', name: 'Lenovo', mark: 'Lenovo' },
  { key: 'lg', name: 'LG', mark: 'LG' },
  { key: 'msi', name: 'MSI', mark: 'msi' },
  { key: 'epson', name: 'Epson', mark: 'EPSON' },
  { key: 'logitech', name: 'Logitech', mark: 'logitech' },
  { key: 'tplink', name: 'TP-Link', mark: 'tp-link' },
]

const FEATURES = [
  { t: 'Precios en USD y pesos', d: 'Cada producto muestra su valor en dólares y en pesos, calculado con la cotización del día.',
    svg: <><line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" /></> },
  { t: 'Envíos a todo el país', d: 'Te lo llevamos a donde estés, con una estimación clara de cuándo llega tu pedido.',
    svg: <><rect x="1" y="3" width="15" height="13" /><polygon points="16 8 20 8 23 11 23 16 16 16 16 8" /><circle cx="5.5" cy="18.5" r="2.5" /><circle cx="18.5" cy="18.5" r="2.5" /></> },
  { t: 'Atención por WhatsApp', d: 'Consultá disponibilidad y comprá con atención personalizada del otro lado.',
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
  // Resumen del catálogo para la home (GET /api/productos/portada): total, cantidades por
  // categoría, destacados y hero. Antes se bajaba el catálogo entero para elegir estos pocos.
  const [portada, setPortada] = useState(null)
  const [arbol, setArbol] = useState([])
  const [menuAbierto, setMenuAbierto] = useState(false)
  // Ids de productos cuya foto no cargó: se usan para descartarlos de las tarjetas del hero.
  const [imgsRotas, setImgsRotas] = useState(() => new Set())
  const marcarImagenRota = (id) =>
    setImgsRotas(prev => (prev.has(id) ? prev : new Set(prev).add(id)))
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // La barra de las otras páginas lleva a /#categorias y /#por-que. React Router no baja solo a
  // la sección al cambiar de página, así que se hace acá una vez montada la home.
  useEffect(() => {
    if (!location.hash) return
    const el = document.getElementById(decodeURIComponent(location.hash.slice(1)))
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [location.hash])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  useEffect(() => {
    axios.get('/api/productos/portada').then(r => setPortada(r.data)).catch(() => {})
    axios.get('/api/categorias').then(r => setArbol(r.data)).catch(() => {})
  }, [])

  // Chips: solo las categorías con productos, ordenadas por cantidad.
  const categorias = useMemo(() => {
    const cuenta = portada?.cantidadPorCategoriaRaiz || {}
    if (!arbol.length || !Object.keys(cuenta).length) return CATS_FALLBACK
    const conProd = arbol.filter(c => cuenta[c.id])
    return conProd.length ? [...conProd].sort((a, b) => cuenta[b.id] - cuenta[a.id]) : CATS_FALLBACK
  }, [arbol, portada])

  // Hero: un producto real de cada categoría, elegido por id del árbol (no por nombre). El
  // servidor ya sortea uno con foto y precio por categoría raíz.
  //
  // Pero "tiene imagenUrl" no es lo mismo que "la imagen carga": muchas fotos son hotlinks a
  // miniaturas de Google Shopping o .webp del mayorista que devuelven contenido inválido. Antes,
  // cuando una fallaba se ocultaba el <img> y quedaba un recuadro blanco en la portada. Ahora
  // anotamos el producto como roto y la tarjeta pasa sola al siguiente candidato con foto.
  const heroCards = useMemo(() => {
    const porRaiz = portada?.heroPorCategoriaRaiz || {}
    if (!arbol.length) return HERO_FALLBACK

    // Repuestos: el resto de los hero (uno por categoría) y los destacados. Todos vienen del
    // servidor ya filtrados por foto y precio, así que sirven tal cual.
    const repuestos = [...Object.values(porRaiz), ...(portada?.destacados || [])].filter(Boolean)
    const usados = new Set()

    return ['Notebooks', 'Placas de video', 'Monitores'].map((nombreCat, i) => {
      const cat = arbol.find(c => c.nombre === nombreCat)
      const propio = cat ? porRaiz[cat.id] : null
      const candidatos = [propio, ...repuestos].filter(Boolean)
      const elegido = candidatos.find(
        p => p.id && p.imagenUrl && !imgsRotas.has(p.id) && !usados.has(p.id),
      )
      if (!elegido) return HERO_FALLBACK[i]
      usados.add(elegido.id)
      // Si el reemplazo vino de otro rubro, la etiqueta tiene que decir la verdad.
      const chip =
        elegido === propio ? nombreCat : elegido.categoriaPadre || elegido.categoria || nombreCat
      return {
        chip,
        nombre: cortar(nombreDe(elegido), 34),
        usd: precioDesde(elegido),
        id: elegido.id,
        img: elegido.imagenUrl,
      }
    })
  }, [arbol, portada, imgsRotas])

  const destacados = portada?.destacados || []

  const totalProductos = portada?.totalCatalogo ? `+${Math.floor(portada.totalCatalogo / 10) * 10}` : '+600'

  const cerrarMenu = () => setMenuAbierto(false)

  return (
    <div className="lp">
      <BarraArrepentimiento />
      {/* NAV */}
      <header className="lp-header">
        <div className="lp-wrap lp-nav">
          <a className="lp-nav-logo" href="#top"><img src="/logo.png?v=2" alt="FuturaTecno" /></a>
          <nav className={`lp-nav-links${menuAbierto ? ' abierto' : ''}`} onClick={cerrarMenu}>
            {/* Un solo acceso a los productos: el catálogo completo. "Ver catálogo" llevaba al mismo lugar. */}
            <Link to="/catalogo">Productos</Link>
            <a href="#categorias">Categorías</a>
            <Link to="/arma-tu-pc">Armá tu PC</Link>
            <a href="#por-que">Por qué</a>
            {isAdmin && <Link to="/admin">Panel Admin</Link>}
            {user ? (
              <>
                <Link to="/mis-puntos">Mis puntos</Link>
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
                <Link to="/registro" className="lp-nav-cta">Registrarse</Link>
              </>
            )}
          </nav>
          <button className="lp-nav-toggle" aria-label="Abrir menú" onClick={() => setMenuAbierto(v => !v)}>
            <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
        </div>
      </header>

      <PromotionsCarousel />

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
                  {/* eager, NO lazy: estas tres tarjetas están apiladas con transform y la
                      tercera queda lo bastante abajo como para que el navegador nunca dispare
                      la carga diferida. Resultado: no cargaba, quedaba un recuadro vacío, y
                      como tampoco fallaba, el reemplazo por error nunca se activaba. Son las
                      imágenes principales de la portada: se cargan sí o sí. */}
                  <img
                    key={c.id}
                    src={c.img}
                    alt={c.nombre}
                    loading="eager"
                    onError={() => marcarImagenRota(c.id)}
                  />
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

      {/* ARMÁ TU PC */}
      <section className="lp-armador" aria-labelledby="armador-title">
        <div className="lp-wrap">
          <Link to="/arma-tu-pc" className="lp-armador-card">
            <div>
              <span className="lp-eyebrow">Nuevo</span>
              <h2 className="lp-title" id="armador-title">Armá tu PC a medida.</h2>
              <p className="lp-armador-texto">
                Elegí procesador, mother, memoria y el resto, paso a paso. Te mostramos solo lo que es compatible entre sí.
              </p>
              <span className="lp-btn lp-btn-primary">Empezar a armar →</span>
            </div>
            <ul className="lp-armador-pasos" aria-hidden="true">
              {['Procesador', 'Motherboard', 'Memoria', 'Video', 'Disco', 'Fuente', 'Gabinete', 'Cooler'].map(p => <li key={p}>{p}</li>)}
            </ul>
          </Link>
        </div>
      </section>

      {/* MARCAS */}
      <section className="lp-brands" aria-labelledby="marcas-title">
        <div className="lp-wrap">
          <p className="lp-brands-kicker" id="marcas-title">Algunas de las marcas que trabajamos</p>
          <div className="lp-brand-marquee">
            <div className="lp-brand-track">
              {[...BRANDS, ...BRANDS].map((brand, index) => (
              <span className={`lp-brand lp-brand-${brand.key}`} key={`${brand.key}-${index}`} aria-hidden={index >= BRANDS.length}>
                {brand.mark}
              </span>
            ))}
            </div>
          </div>
        </div>
      </section>

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

      <PieLegal />
    </div>
  )
}

export default LandingPage
