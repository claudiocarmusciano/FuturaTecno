// Analítica: Google Analytics 4 y Pixel de Meta.
//
// Los IDs vienen de GET /api/config (variables GA4_MEASUREMENT_ID y META_PIXEL_ID del backend),
// igual que el Client ID de Google: se cambian en Railway sin rehornear el frontend. Si están
// vacíos no se carga ningún script y todas las funciones de acá no hacen nada.
//
// No se mide al admin (ni /admin ni una sesión con rol ADMIN): sus visitas inflarían los números y
// le enseñarían a Meta que "el comprador típico" es el dueño revisando el catálogo.
//
// Los importes van en USD, la moneda en la que se fija el precio (el peso cambia con la
// cotización del día). La compra (Purchase) se manda SOLO cuando Mercado Pago aprobó el pago, una
// vez por pedido, con el número de pedido como id: así Meta y GA4 descartan un duplicado si el
// cliente recarga la página de resultado.

import axios from 'axios'

let estado = 'pendiente'   // pendiente → cargando → listo | apagado
let ga4 = null
let pixel = null
const cola = []

const esAdmin = () => {
  if (window.location.pathname.startsWith('/admin')) return true
  try { return JSON.parse(localStorage.getItem('auth') || 'null')?.rol === 'ADMIN' } catch { return false }
}

function cargarScript(src) {
  const s = document.createElement('script')
  s.async = true
  s.src = src
  document.head.appendChild(s)
}

function iniciarGa4(id) {
  window.dataLayer = window.dataLayer || []
  window.gtag = function () { window.dataLayer.push(arguments) }
  window.gtag('js', new Date())
  // Las páginas vistas las manda el router (es una SPA): sin esto GA4 contaría solo la primera.
  window.gtag('config', id, { send_page_view: false })
  cargarScript(`https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(id)}`)
}

function iniciarPixel(id) {
  /* eslint-disable */
  !function (f, b, e, v, n, t, s) {
    if (f.fbq) return; n = f.fbq = function () { n.callMethod ? n.callMethod.apply(n, arguments) : n.queue.push(arguments) }
    if (!f._fbq) f._fbq = n; n.push = n; n.loaded = !0; n.version = '2.0'; n.queue = []
    t = b.createElement(e); t.async = !0; t.src = v; s = b.getElementsByTagName(e)[0]; s.parentNode.insertBefore(t, s)
  }(window, document, 'script', 'https://connect.facebook.net/en_US/fbevents.js')
  /* eslint-enable */
  window.fbq('init', id)
}

/** Carga los scripts una sola vez. Lo que se mida antes de que termine queda en cola. */
export function iniciarAnalitica() {
  if (estado !== 'pendiente') return
  estado = 'cargando'
  axios.get('/api/config')
    .then(({ data }) => {
      ga4 = data?.ga4MeasurementId || null
      pixel = data?.metaPixelId || null
      if (!ga4 && !pixel) { estado = 'apagado'; cola.length = 0; return }
      if (ga4) iniciarGa4(ga4)
      if (pixel) iniciarPixel(pixel)
      estado = 'listo'
      cola.splice(0).forEach(fn => fn())
    })
    .catch(() => { estado = 'apagado'; cola.length = 0 })
}

function enviar(fn) {
  if (estado === 'apagado' || esAdmin()) return
  if (estado !== 'listo') { cola.push(fn); return }
  try { fn() } catch { /* la medición nunca debe romper la tienda */ }
}

const itemGa4 = (i) => ({
  item_id: String(i.productoId ?? i.id ?? ''),
  item_name: i.nombre,
  price: Number(i.precioUsd) || 0,
  quantity: i.cantidad ?? 1
})

// ------------------------------------------------------------------ eventos

export function paginaVista(ruta) {
  enviar(() => {
    if (ga4) window.gtag('event', 'page_view', { page_path: ruta, page_location: window.location.href, page_title: document.title })
    if (pixel) window.fbq('track', 'PageView')
  })
}

/** Ficha de producto. {id, nombre, precioUsd} */
export function productoVisto(p) {
  enviar(() => {
    if (ga4) window.gtag('event', 'view_item', { currency: 'USD', value: Number(p.precioUsd) || 0, items: [itemGa4(p)] })
    if (pixel) window.fbq('track', 'ViewContent', { content_ids: [String(p.id)], content_type: 'product', content_name: p.nombre, value: Number(p.precioUsd) || 0, currency: 'USD' })
  })
}

/** Agregado al carrito. {productoId, nombre, precioUsd, cantidad} */
export function agregadoAlCarrito(i) {
  const valor = (Number(i.precioUsd) || 0) * (i.cantidad || 1)
  enviar(() => {
    if (ga4) window.gtag('event', 'add_to_cart', { currency: 'USD', value: valor, items: [itemGa4(i)] })
    if (pixel) window.fbq('track', 'AddToCart', { content_ids: [String(i.productoId)], content_type: 'product', value: valor, currency: 'USD' })
  })
}

/** Entró al checkout con este carrito. */
export function checkoutIniciado(items, totalUsd) {
  enviar(() => {
    if (ga4) window.gtag('event', 'begin_checkout', { currency: 'USD', value: Number(totalUsd) || 0, items: items.map(itemGa4) })
    if (pixel) window.fbq('track', 'InitiateCheckout', { content_ids: items.map(i => String(i.productoId)), num_items: items.length, value: Number(totalUsd) || 0, currency: 'USD' })
  })
}

/**
 * Pedido confirmado que todavía NO está pagado (transferencia, efectivo o yendo a Mercado Pago).
 * Es una intención firme, pero no una venta: se manda aparte para no inflar las compras.
 */
export function pedidoConfirmado(pedido, medioPago) {
  enviar(() => {
    const valor = Number(pedido.totalUsd) || 0
    if (ga4) window.gtag('event', 'pedido_confirmado', { currency: 'USD', value: valor, medio_pago: medioPago, transaction_id: pedido.numero })
    if (pixel) window.fbq('trackCustom', 'PedidoConfirmado', { value: valor, currency: 'USD', medio_pago: medioPago }, { eventID: `pedido-${pedido.numero}` })
  })
}

/** Pago aprobado por Mercado Pago. Una sola vez por pedido, aunque se recargue la página. */
export function compraRealizada(pedido) {
  const clave = `analitica_compra_${pedido.numero}`
  try { if (localStorage.getItem(clave)) return } catch { /* sin storage: se manda igual */ }
  enviar(() => {
    const valor = Number(pedido.totalUsd) || 0
    const items = (pedido.items || []).map(i => ({ productoId: i.productoId, nombre: i.productoNombre, precioUsd: i.precioUnitarioUsd, cantidad: i.cantidad }))
    if (ga4) window.gtag('event', 'purchase', { transaction_id: pedido.numero, currency: 'USD', value: valor, items: items.map(itemGa4) })
    if (pixel) window.fbq('track', 'Purchase', { content_ids: items.map(i => String(i.productoId)), content_type: 'product', num_items: items.length, value: valor, currency: 'USD' }, { eventID: `compra-${pedido.numero}` })
    try { localStorage.setItem(clave, '1') } catch { /* nada */ }
  })
}

/** Cuenta creada. */
export function registroCompleto(metodo) {
  enviar(() => {
    if (ga4) window.gtag('event', 'sign_up', { method: metodo })
    if (pixel) window.fbq('track', 'CompleteRegistration', { status: metodo })
  })
}

/** Clic a WhatsApp desde cualquier parte del sitio: hoy es el canal por donde más se vende. */
export function contactoWhatsapp(origen) {
  enviar(() => {
    if (ga4) window.gtag('event', 'contacto_whatsapp', { origen })
    if (pixel) window.fbq('track', 'Contact', { content_name: origen })
  })
}
