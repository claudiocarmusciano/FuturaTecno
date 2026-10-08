import { useState, useEffect, useMemo } from 'react'
import { Link, useLocation } from 'react-router-dom'
import axios from 'axios'
import { WHATSAPP_NUMBER, NOMBRE_NEGOCIO, TITULAR, HORARIO_LOCAL } from '../../config'
import './Landing.css'
import PromotionsCarousel from '../../components/PromotionsCarousel'
import PromoArmado from '../../components/PromoArmado'

const waLink = `https://wa.me/${WHATSAPP_NUMBER}?text=` +
  encodeURIComponent(`Hola ${NOMBRE_NEGOCIO}, quería hacer una consulta sobre el catálogo.`)

const mapaLink = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(TITULAR.domicilioComercial)}`

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

// Íconos de línea (24 px, trazo 1.5) para la fila de categorías. Heredan el color del texto.
const Ico = ({ children }) => (
  <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" strokeWidth="1.5"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>
)

// Fila de categorías de la home: las más buscadas, en este orden. Se buscan por NOMBRE en el
// árbol real para tomar el id vigente; si una no existe (o quedó sin productos), se saltea.
const CATEGORIAS_HOME = [
  { nombre: 'Celulares', ico: <><rect x="7" y="2.5" width="10" height="19" rx="2" /><path d="M11 18.5h2" /></> },
  { nombre: 'Apple', ico: <><path d="M12 7c-1.5-1.2-4-1.3-5.4.4C5 9.4 5.5 13 7 15.6 8 17.4 9.3 19.5 11 19c.6-.2 1-.4 1-.4s.4.2 1 .4c1.7.5 3-1.6 4-3.4.5-.9.9-1.9 1.1-2.9-1.5-.6-2.4-2-2.4-3.6 0-1.3.6-2.4 1.6-3.1C15.8 5.6 13.6 5.8 12 7Z" /><path d="M12 7c0-1.8 1-3.4 2.6-4" /></> },
  { nombre: 'Notebooks', ico: <><rect x="4" y="5" width="16" height="11" rx="1" /><path d="M2 19h20" /></> },
  { nombre: 'Computadoras', ico: <><rect x="3" y="4" width="18" height="12" rx="1" /><path d="M8 20h8M12 16v4" /></> },
  { nombre: 'Monitores', ico: <><rect x="2" y="4" width="20" height="13" rx="1" /><path d="M9 21h6M12 17v4" /></> },
  { nombre: 'Placas de video', ico: <><rect x="2" y="6" width="20" height="10" rx="1" /><circle cx="8" cy="11" r="2.5" /><circle cx="16" cy="11" r="2.5" /><path d="M4 16v3M8 16v2" /></> },
  { nombre: 'Periféricos', ico: <><rect x="7" y="3" width="10" height="18" rx="5" /><path d="M12 3v6" /></> },
  { nombre: 'Consolas', ico: <><path d="M6 8h12a4 4 0 0 1 4 4v1a4 4 0 0 1-7 2.6L14 15h-4l-1 .6A4 4 0 0 1 2 13v-1a4 4 0 0 1 4-4Z" /><path d="M7 11v3M5.5 12.5h3" /><circle cx="16.5" cy="11.5" r=".6" /><circle cx="18" cy="13" r=".6" /></> },
  { nombre: 'Audio', ico: <><path d="M3 14v-2a9 9 0 0 1 18 0v2" /><rect x="2" y="14" width="4" height="7" rx="1" /><rect x="18" y="14" width="4" height="7" rx="1" /></> },
  { nombre: 'Impresoras', ico: <><path d="M6 9V3h12v6" /><rect x="2" y="9" width="20" height="8" rx="1" /><path d="M6 14h12v7H6z" /></> },
]

const IcoLocal = () => <Ico><path d="M3 10 12 3l9 7" /><path d="M5 9v12h14V9" /><path d="M10 21v-6h4v6" /></Ico>
const IcoEnvio = () => <Ico><rect x="1" y="4" width="14" height="12" /><path d="M15 8h4l3 3v5h-7z" /><circle cx="5.5" cy="18.5" r="2" /><circle cx="18.5" cy="18.5" r="2" /></Ico>
const IcoChat = () => <Ico><path d="M21 11.5a8.4 8.4 0 0 1-12.2 7.5L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5Z" /></Ico>
const IcoPrecio = () => <Ico><path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0L2 12V2h10l8.6 8.6a2 2 0 0 1 0 2.8Z" /><circle cx="7" cy="7" r="1.5" /></Ico>
const IcoPago = () => <Ico><rect x="2" y="5" width="20" height="14" rx="1" /><path d="M2 10h20M6 15h4" /></Ico>
const IcoGarantia = () => <Ico><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" /><path d="m9 12 2 2 4-4" /></Ico>

const VENTAJAS = [
  { ico: <IcoLocal />, t: 'Local en Olavarría', d: `Vení a vernos a ${TITULAR.domicilioComercial.split(',')[0]} o retirá tu compra en el local.` },
  { ico: <IcoEnvio />, t: 'Envíos a todo el país', d: 'Enviamos por Andreani a cualquier punto del país.' },
  { ico: <IcoPrecio />, t: 'Precios del día', d: 'Cada producto muestra su precio en pesos y en dólares, con la cotización de hoy.' },
  { ico: <IcoPago />, t: 'Pagá como quieras', d: 'Mercado Pago, transferencia o efectivo, con descuento pagando en efectivo.' },
  { ico: <IcoGarantia />, t: 'Garantía oficial', d: 'Productos nuevos y con garantía. Las condiciones están en Garantía y devoluciones.' },
  { ico: <IcoChat />, t: 'Atención por WhatsApp', d: 'Te asesoramos antes y después de la compra, con una persona del otro lado.' },
]

// Rubros que se prefieren para la vidriera del hero (los productos los sortea el servidor).
const VIDRIERA_PREFERIDA = ['Apple', 'Notebooks', 'Celulares', 'Consolas', 'Monitores', 'Placas de video']

const MARCAS = ['Apple', 'Samsung', 'Motorola', 'Xiaomi', 'Lenovo', 'HP', 'ASUS', 'MSI', 'LG', 'Logitech', 'HyperX', 'Epson', 'AMD', 'Intel', 'Sony', 'JBL', 'TP-Link', 'Corsair']

const IconWhatsApp = () => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347M12.05 21.785h-.004a9.87 9.87 0 0 1-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 0 1-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 0 1 2.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0 0 12.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 0 0 5.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 0 0-3.48-8.413" /></svg>
)

function TarjetaProducto({ p }) {
  const usd = precioDesde(p), ars = precioArsDesde(p)
  return (
    <Link className="lp-prod" to={`/producto/${p.id}`}>
      <div className="lp-prod-img">
        <img src={p.imagenUrl} alt={nombreDe(p)} loading="lazy" onError={e => { e.target.style.visibility = 'hidden' }} />
      </div>
      <div className="lp-prod-body">
        {p.categoria && <span className="lp-prod-cat">{p.categoria}</span>}
        <h3 className="lp-prod-name">{nombreDe(p)}</h3>
        <div className="lp-prod-price">
          {ars ? <strong>$ {fmt(ars)}</strong> : null}
          {usd ? <span>US$ {fmt(usd)}</span> : null}
        </div>
        <span className="lp-link">Ver producto ›</span>
      </div>
    </Link>
  )
}

function LandingPage() {
  // Resumen del catálogo para la home (GET /api/productos/portada): total, cantidades por
  // categoría, destacados y un producto con foto por categoría raíz.
  const [portada, setPortada] = useState(null)
  const [arbol, setArbol] = useState([])
  // Ids de productos cuya foto no cargó: se descartan de la vidriera del hero.
  const [imgsRotas, setImgsRotas] = useState(() => new Set())
  const marcarImagenRota = (id) =>
    setImgsRotas(prev => (prev.has(id) ? prev : new Set(prev).add(id)))
  const location = useLocation()

  // Los links "/#categorias" de otras páginas: React Router no baja solo a la sección.
  useEffect(() => {
    if (!location.hash) return
    const el = document.getElementById(decodeURIComponent(location.hash.slice(1)))
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [location.hash])

  useEffect(() => {
    axios.get('/api/productos/portada').then(r => setPortada(r.data)).catch(() => {})
    axios.get('/api/categorias').then(r => setArbol(r.data)).catch(() => {})
  }, [])

  const categorias = useMemo(() => {
    const cuenta = portada?.cantidadPorCategoriaRaiz || {}
    return CATEGORIAS_HOME
      .map(c => ({ ...c, cat: arbol.find(a => a.nombre === c.nombre) }))
      .filter(c => c.cat && (!portada || cuenta[c.cat.id]))
  }, [arbol, portada])

  // Vidriera del hero: tres productos reales con foto, de rubros distintos. "Tiene imagenUrl" no
  // garantiza que la foto cargue (hay hotlinks que fallan), así que una foto rota pasa al siguiente.
  const vidriera = useMemo(() => {
    const porRaiz = portada?.heroPorCategoriaRaiz || {}
    const preferidos = VIDRIERA_PREFERIDA
      .map(nombre => arbol.find(c => c.nombre === nombre))
      .map(c => c && porRaiz[c.id])
    const candidatos = [...preferidos, ...Object.values(porRaiz), ...(portada?.destacados || [])]
    const vistos = new Set()
    return candidatos.filter(p => {
      if (!p?.id || !p.imagenUrl || imgsRotas.has(p.id) || vistos.has(p.id)) return false
      vistos.add(p.id)
      return true
    }).slice(0, 3)
  }, [portada, arbol, imgsRotas])

  const destacados = (portada?.destacados || []).slice(0, 8)

  return (
    <div className="lp">
      {/* HERO: franja cyan de borde a borde con la tarjeta blanca de texto a la izquierda. */}
      <section className="lp-hero" id="top">
        <div className="lp-wrap lp-hero-grid">
          <div className="lp-hero-card">
            <span className="lp-rotulo">Tecnología en Olavarría</span>
            <h1>La tecnología que buscás, al mejor precio.</h1>
            <p>
              Celulares, notebooks, PC, componentes y más. Precios del día en pesos y dólares,
              envíos a todo el país y atención por WhatsApp.
            </p>
            <div className="lp-cta-row">
              <Link className="lp-btn" to="/catalogo">Ver productos</Link>
              <a className="lp-link lp-link-wa" href={waLink} target="_blank" rel="noreferrer">
                <IconWhatsApp /> Consultar por WhatsApp
              </a>
            </div>
          </div>
          <div className="lp-vidriera" aria-label="Algunos productos">
            {vidriera.map((p, i) => (
              <Link key={p.id} to={`/producto/${p.id}`} className={`lp-vidriera-item lp-v${i + 1}`}>
                {/* eager: son las imágenes principales de la portada. */}
                <img src={p.imagenUrl} alt={nombreDe(p)} loading="eager" onError={() => marcarImagenRota(p.id)} />
                <span>{cortar(nombreDe(p), 30)}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Beneficio destacado: el armado y la instalación de regalo. */}
      <div className="lp-wrap lp-promo-armado"><PromoArmado enlace /></div>

      {/* CATEGORÍAS: fila de íconos de línea con el nombre en azul de acción. */}
      <section className="lp-block lp-cats-block" id="categorias" aria-label="Categorías">
        <div className="lp-wrap">
          <div className="lp-cats">
            {categorias.map(c => (
              <Link key={c.cat.id} className="lp-cat" to={`/catalogo?cat=${c.cat.id}`}>
                <Ico>{c.ico}</Ico>
                <span>{c.nombre}</span>
              </Link>
            ))}
          </div>
          <div className="lp-cats-todas"><Link className="lp-link" to="/catalogo">Ver todas las categorías ›</Link></div>
        </div>
      </section>

      <PromotionsCarousel />

      {/* DESTACADOS: grilla de 4 columnas. */}
      <section className="lp-block" id="productos">
        <div className="lp-wrap">
          <div className="lp-head">
            <h2>Destacados</h2>
            <Link className="lp-link" to="/catalogo">Ver todos los productos ›</Link>
          </div>
          <div className="lp-prod-grid">
            {destacados.length === 0
              ? <div className="lp-prod-loading">Cargando productos…</div>
              : destacados.map(p => <TarjetaProducto key={p.id} p={p} />)}
          </div>
        </div>
      </section>

      {/* ARMÁ TU PC: foto del rincón gamer del local, de borde a borde, con la tarjeta blanca encima. */}
      <section className="lp-banda" aria-labelledby="armador-title">
        <img className="lp-banda-foto" src="/local/rincon-gamer.jpg" alt="" aria-hidden="true" loading="lazy" />
        <div className="lp-wrap lp-banda-grid">
          <div className="lp-banda-card">
            <span className="lp-rotulo">Armá tu PC</span>
            <h2 id="armador-title">Tu PC a medida, pieza por pieza.</h2>
            <p>Elegí procesador, mother, memoria y el resto, paso a paso. Te mostramos solo lo que es compatible entre sí.</p>
            <p className="lp-banda-regalo"><strong>De regalo:</strong> si comprás todos los componentes acá, te la armamos y te instalamos Windows + Office.</p>
            <Link className="lp-btn" to="/arma-tu-pc">Empezar a armar</Link>
          </div>
        </div>
      </section>

      {/* POR QUÉ */}
      <section className="lp-block" id="por-que">
        <div className="lp-wrap">
          <div className="lp-head"><h2>Por qué comprar en {NOMBRE_NEGOCIO}</h2></div>
          <div className="lp-ventajas">
            {VENTAJAS.map(v => (
              <div className="lp-ventaja" key={v.t}>
                {v.ico}
                <h3>{v.t}</h3>
                <p>{v.d}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* NUESTRO LOCAL */}
      <section className="lp-block" id="local" aria-labelledby="local-title">
        <div className="lp-wrap lp-local">
          <img className="lp-local-foto" src="/local/fachada.jpg" alt={`Frente del local de ${NOMBRE_NEGOCIO}`} loading="lazy" width="1280" height="874" />
          <div className="lp-local-texto">
            <span className="lp-rotulo">Nuestro local</span>
            <h2 id="local-title">Vení a conocernos.</h2>
            <p>Estamos en {TITULAR.domicilioComercial}. Podés ver los productos, retirar tu compra o consultarnos lo que necesites.</p>
            <p className="lp-local-horario"><strong>Horario:</strong> {HORARIO_LOCAL}</p>
            <div className="lp-cta-row">
              <a className="lp-btn" href={mapaLink} target="_blank" rel="noreferrer">Cómo llegar</a>
              <a className="lp-link lp-link-wa" href={waLink} target="_blank" rel="noreferrer"><IconWhatsApp /> Escribinos</a>
            </div>
          </div>
        </div>
      </section>

      {/* MARCAS */}
      <section className="lp-marcas" aria-labelledby="marcas-title">
        <div className="lp-wrap">
          <p className="lp-rotulo" id="marcas-title">Marcas que trabajamos</p>
          <ul className="lp-marcas-lista">
            {MARCAS.map(m => <li key={m}>{m}</li>)}
          </ul>
        </div>
      </section>
    </div>
  )
}

export default LandingPage
