import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import { useCart } from '../../cart/CartContext'
import PaymentPrices from '../../components/PaymentPrices'
import { WHATSAPP_NUMBER } from '../../config'
import { etiquetaEnvio } from '../../utils/envio'
import './ArmaTuPcPage.css'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const STORAGE_KEY = 'armaTuPc'
const POR_PAGINA = 24
const FORMATOS = ['ITX', 'MATX', 'ATX', 'EATX']
const NOMBRE_FORMATO = { ITX: 'Mini-ITX', MATX: 'Micro-ATX', ATX: 'ATX', EATX: 'E-ATX' }
/**
 * Código corto de cada paso en el link para compartir: `?armado=cpu:123,ram:456x2`.
 * El número es el varianteId; si se renombra un código, los links viejos dejan de abrir esa pieza.
 */
const CODIGO_LINK = {
  PROCESADOR: 'cpu', MOTHER: 'mb', MEMORIA: 'ram', VIDEO: 'gpu', ALMACENAMIENTO: 'd1',
  ALMACENAMIENTO_2: 'd2', FUENTE: 'psu', GABINETE: 'gab', COOLER: 'cool',
}
const CLAVE_DE_CODIGO = Object.fromEntries(Object.entries(CODIGO_LINK).map(([k, v]) => [v, k]))

function aLink(sel) {
  return PASOS.filter(p => sel[p.clave])
    .map(p => `${CODIGO_LINK[p.clave]}:${sel[p.clave].item.varianteId}${sel[p.clave].cantidad > 1 ? `x${sel[p.clave].cantidad}` : ''}`)
    .join(',')
}

/** Lee `?armado=`. Devuelve { clave: { varianteId, cantidad } }, ignorando lo que no se entiende. */
function deLink(texto) {
  const out = {}
  for (const parte of (texto || '').split(',')) {
    const m = parte.match(/^([a-z0-9]+):(\d+)(?:x(\d))?$/)
    if (m && CLAVE_DE_CODIGO[m[1]]) out[CLAVE_DE_CODIGO[m[1]]] = { varianteId: Number(m[2]), cantidad: Number(m[3] || 1) }
  }
  return out
}

const NOMBRE_GAMA = { 1: 'entrada', 2: 'básica', 3: 'media', 4: 'alta', 5: 'tope' }
const RAM_DE_SOCKET = { AM4: 'DDR4', AM5: 'DDR5', LGA1851: 'DDR5', LGA1200: 'DDR4' }

/**
 * Orden de armado. Cada paso filtra por lo elegido en los anteriores, así que el orden importa:
 * el socket lo fija el procesador, el tipo de RAM lo fija el mother.
 * `clave`: dónde se guarda lo elegido. Coincide con `tipo` salvo en el 2º disco, que ofrece el
 * mismo tipo de componente que "Almacenamiento" pero es otra elección (un SSD + un rígido).
 * `cantidad`: se puede llevar más de una unidad (dos memorias, dos discos iguales).
 */
const PASOS = [
  { clave: 'PROCESADOR', tipo: 'PROCESADOR', titulo: 'Procesador', icono: '🧠' },
  { clave: 'MOTHER', tipo: 'MOTHER', titulo: 'Motherboard', icono: '🧩' },
  { clave: 'MEMORIA', tipo: 'MEMORIA', titulo: 'Memoria RAM', icono: '💾', cantidad: true },
  { clave: 'VIDEO', tipo: 'VIDEO', titulo: 'Placa de video', icono: '🎮' },
  { clave: 'ALMACENAMIENTO', tipo: 'ALMACENAMIENTO', titulo: 'Almacenamiento', icono: '🗄️', cantidad: true },
  { clave: 'ALMACENAMIENTO_2', tipo: 'ALMACENAMIENTO', titulo: '2º disco', icono: '💽', cantidad: true },
  { clave: 'FUENTE', tipo: 'FUENTE', titulo: 'Fuente', icono: '⚡' },
  { clave: 'GABINETE', tipo: 'GABINETE', titulo: 'Gabinete', icono: '🖥️' },
  { clave: 'COOLER', tipo: 'COOLER', titulo: 'Cooler', icono: '❄️' },
]

/**
 * Compatibilidad de un componente con lo ya elegido.
 * - `bloqueo`: incompatibilidad cierta (socket, tipo de RAM). El componente no se ofrece.
 * - `avisos`: posible problema, o dato que no se pudo leer de la ficha. Se ofrece igual, marcado.
 * Solo se bloquea lo que se sabe con certeza: un dato faltante nunca oculta un producto.
 */
function evaluar(c, sel) {
  const avisos = []
  const cpu = sel.PROCESADOR?.item
  const mother = sel.MOTHER?.item
  const video = sel.VIDEO?.item

  if (c.tipo === 'MOTHER' && cpu) {
    if (c.socket && cpu.socket && c.socket !== cpu.socket) return { bloqueo: `Socket ${c.socket}, el procesador es ${cpu.socket}` }
    if (!c.socket || !cpu.socket) avisos.push('Verificá que el socket coincida con el procesador')
  }
  // Volver a un paso anterior no oculta nada: cambiar de plataforma es válido, y al elegir
  // `depurar` saca lo que deja de encajar. Solo se avisa qué se va a perder.
  if (c.tipo === 'PROCESADOR' && mother && c.socket && mother.socket && c.socket !== mother.socket) {
    avisos.push(`Es ${c.socket}: si lo elegís se quita el mother (${mother.socket})`)
  }
  // Sin mother todavía, la plataforma del procesador ya fija la RAM (salvo LGA1700, que tiene las dos).
  if (c.tipo === 'MEMORIA' && !mother && cpu && RAM_DE_SOCKET[cpu.socket] && c.tipoRam && c.tipoRam !== RAM_DE_SOCKET[cpu.socket]) {
    return { bloqueo: `${c.tipoRam}, la plataforma ${cpu.socket} usa ${RAM_DE_SOCKET[cpu.socket]}` }
  }
  if (c.tipo === 'MEMORIA' && mother) {
    if (c.tipoRam && mother.tipoRam && c.tipoRam !== mother.tipoRam) return { bloqueo: `${c.tipoRam}, el mother usa ${mother.tipoRam}` }
    if (!mother.tipoRam) avisos.push('Verificá si el mother usa DDR4 o DDR5')
    if (mother.ranurasRam && (c.modulos || 1) > mother.ranurasRam) {
      return { bloqueo: `Kit de ${c.modulos} módulos, el mother tiene ${mother.ranurasRam} ranuras` }
    }
  }
  if (c.tipo === 'MOTHER' && sel.MEMORIA?.item) {
    const ram = sel.MEMORIA.item
    if (c.tipoRam && ram.tipoRam && c.tipoRam !== ram.tipoRam) avisos.push(`Usa ${c.tipoRam}: si lo elegís se quita la memoria (${ram.tipoRam})`)
  }
  if (c.tipo === 'GABINETE' && mother) {
    if (c.formato && mother.formato) {
      if (FORMATOS.indexOf(mother.formato) > FORMATOS.indexOf(c.formato)) {
        avisos.push(`Admite hasta ${NOMBRE_FORMATO[c.formato]}: el mother es ${NOMBRE_FORMATO[mother.formato]}`)
      }
    } else {
      avisos.push('Verificá que el mother entre en el gabinete')
    }
  }
  if (c.tipo === 'FUENTE' && video) {
    if (c.potenciaW && video.fuenteRecomendadaW) {
      if (c.potenciaW < video.fuenteRecomendadaW) avisos.push(`La placa de video pide ${video.fuenteRecomendadaW} W o más`)
    } else {
      avisos.push('Verificá que la potencia alcance para la placa de video')
    }
  }
  // Cuello de botella: no es incompatibilidad, es una combinación que no se aprovecha. Se sugiere,
  // nunca se bloquea — la gama es una guía para juegos, y para edición o render puede convenir
  // justamente un procesador fuerte con una placa modesta.
  const sugerencias = []
  let recomendada = false
  if (c.tipo === 'VIDEO' && cpu?.gama && c.gama) {
    const dif = c.gama - cpu.gama
    if (dif >= 2) sugerencias.push('Tu procesador podría limitar a esta placa de video: no vas a aprovechar todo su rendimiento. Te conviene un procesador de gama más alta o una placa más acorde.')
    else if (dif <= -2) sugerencias.push('Para juegos, esta placa queda corta para tu procesador.')
    else recomendada = true
  }
  if (c.tipo === 'PROCESADOR' && video?.gama && c.gama) {
    const dif = video.gama - c.gama
    if (dif >= 2) sugerencias.push('Este procesador podría limitar a tu placa de video: no la vas a aprovechar del todo.')
    else if (dif <= -2) sugerencias.push('Para juegos, tu placa de video queda corta para este procesador.')
  }
  return { avisos, sugerencias, recomendada }
}

/**
 * Cuántas unidades de una memoria entran en el mother elegido. Un kit "2x16GB" ocupa dos
 * ranuras por unidad. Sin mother, o si su ficha no dice cuántas ranuras tiene, se permite hasta
 * 4 (el máximo de un mother de escritorio) y se avisa.
 */
function maxUnidades(memoria, sel) {
  const ranuras = sel.MOTHER?.item?.ranurasRam || 4
  return Math.max(0, Math.floor(ranuras / (memoria.modulos || 1)))
}

/** ¿El paso es obligatorio con lo elegido hasta ahora? */
function esObligatorio(clave, sel) {
  const cpu = sel.PROCESADOR?.item
  if (clave === 'ALMACENAMIENTO_2') return false
  if (clave === 'VIDEO') return cpu?.videoIntegrado === false
  // Sin cooler el equipo no arranca: se exige salvo que se sepa que el procesador lo trae.
  if (clave === 'COOLER') return !!cpu && cpu.incluyeCooler !== true
  return true
}

function notaDelPaso(clave, sel) {
  const cpu = sel.PROCESADOR?.item
  if (clave === 'ALMACENAMIENTO_2') {
    return sel.ALMACENAMIENTO
      ? 'Opcional: sumá un disco distinto al primero, por ejemplo un SSD para el sistema y un rígido para tus archivos.'
      : 'Opcional: elegí primero el disco principal en el paso anterior.'
  }
  if (clave === 'MEMORIA') {
    const mother = sel.MOTHER?.item
    if (!mother) return 'Podés llevar más de un módulo: elegí la cantidad en cada opción.'
    return mother.ranurasRam
      ? `Tu mother tiene ${mother.ranurasRam} ranuras de memoria: podés poner hasta ${mother.ranurasRam} módulos.`
      : 'No sabemos cuántas ranuras de memoria tiene tu mother (suelen ser 2 o 4): verificalo antes de llevar más de dos módulos.'
  }
  if (clave === 'VIDEO' && cpu) {
    return cpu.videoIntegrado === false
      ? 'Tu procesador no tiene video integrado: necesitás una placa de video.'
      : cpu.videoIntegrado ? 'Tu procesador tiene video integrado: la placa de video es opcional.' : null
  }
  if (clave === 'COOLER' && cpu) {
    if (cpu.incluyeCooler === false) return 'Tu procesador viene sin cooler: necesitás uno.'
    if (cpu.incluyeCooler) return 'Tu procesador trae cooler en la caja: este paso es opcional.'
    return 'No pudimos confirmar si tu procesador trae cooler: sumá uno para asegurarte de que el equipo funcione.'
  }
  return null
}

/** Saca lo elegido que dejó de ser compatible después de un cambio (en el orden de armado). */
function depurar(sel) {
  const limpio = {}
  for (const p of PASOS) {
    const actual = sel[p.clave]
    if (!actual) continue
    const { bloqueo } = evaluar(actual.item, limpio)
    if (bloqueo) continue
    limpio[p.clave] = p.clave === 'MEMORIA'
      ? { ...actual, cantidad: Math.min(actual.cantidad, maxUnidades(actual.item, limpio)) }
      : actual
  }
  return limpio
}

function atributos(c) {
  const a = []
  if (c.socket) a.push(c.socket)
  if (c.tipoRam) a.push(c.tipoRam)
  if (c.formato) a.push(NOMBRE_FORMATO[c.formato])
  if (c.ranurasRam) a.push(`${c.ranurasRam} ranuras RAM`)
  if (c.modulos > 1) a.push(`Kit de ${c.modulos} módulos`)
  if (c.potenciaW) a.push(`${c.potenciaW} W`)
  if (c.fuenteRecomendadaW) a.push(`Fuente ${c.fuenteRecomendadaW} W+`)
  if (c.videoIntegrado === true) a.push('Con video')
  if (c.videoIntegrado === false) a.push('Sin video')
  if (c.incluyeCooler === true) a.push('Con cooler')
  if (c.incluyeCooler === false) a.push('Sin cooler')
  if (c.gama) a.push(`Gama ${NOMBRE_GAMA[c.gama]}`)
  return a
}

function leerGuardado() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}
  } catch {
    return {}
  }
}

export default function ArmaTuPcPage() {
  const [componentes, setComponentes] = useState(null)
  const [error, setError] = useState(null)
  const [seleccion, setSeleccion] = useState(leerGuardado)
  const [paso, setPaso] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState('asc')
  const [visibles, setVisibles] = useState(POR_PAGINA)
  const [resumenAbierto, setResumenAbierto] = useState(false)
  const [cantidades, setCantidades] = useState({})
  const { agregar } = useCart()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [avisoLink, setAvisoLink] = useState(null)
  const [cp, setCp] = useState(() => localStorage.getItem('armaTuPcCp') || '')
  const [envio, setEnvio] = useState(null)
  const [cotizando, setCotizando] = useState(false)
  const [copiado, setCopiado] = useState(false)

  useEffect(() => {
    axios.get('/api/arma-tu-pc/componentes')
      .then(res => setComponentes(res.data))
      .catch(() => setError('No pudimos cargar los componentes. Probá de nuevo en unos minutos.'))
  }, [])

  // Lo guardado puede tener precios viejos o productos que ya no están: se reemplaza por la
  // versión de hoy, y lo que desapareció se descarta. Un link compartido (?armado=) pisa lo
  // guardado: quien lo abre quiere ver ESE armado. Después se saca de la URL, para que recargar
  // no deshaga los cambios que haga encima.
  useEffect(() => {
    if (!componentes) return
    const porVariante = new Map(componentes.map(c => [c.varianteId, c]))
    const compartido = searchParams.get('armado')
    if (compartido) {
      const pedido = deLink(compartido)
      const armado = {}
      let faltan = 0
      for (const [clave, { varianteId, cantidad }] of Object.entries(pedido)) {
        const hoy = porVariante.get(varianteId)
        if (hoy) armado[clave] = { item: hoy, cantidad }
        else faltan++
      }
      setSeleccion(depurar(armado))
      setAvisoLink(faltan
        ? `Abriste un armado compartido. ${faltan} ${faltan === 1 ? 'componente ya no está disponible' : 'componentes ya no están disponibles'}: elegí un reemplazo.`
        : 'Abriste un armado compartido. Podés cambiar lo que quieras.')
      setSearchParams({}, { replace: true })
      return
    }
    setSeleccion(prev => {
      const vigente = {}
      for (const [tipo, s] of Object.entries(prev)) {
        const hoy = porVariante.get(s?.item?.varianteId)
        if (hoy) vigente[tipo] = { item: hoy, cantidad: s.cantidad || 1 }
      }
      return depurar(vigente)
    })
  }, [componentes])

  useEffect(() => {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(seleccion)) } catch { /* modo privado */ }
  }, [seleccion])

  useEffect(() => { setBusqueda(''); setVisibles(POR_PAGINA) }, [paso])

  // Una cotización vale para las piezas con que se pidió.
  useEffect(() => { setEnvio(null) }, [seleccion])

  const cotizarEnvio = async () => {
    setCotizando(true)
    setEnvio(null)
    try { localStorage.setItem('armaTuPcCp', cp) } catch { /* modo privado */ }
    try {
      const { data } = await axios.post('/api/envio/cotizar', {
        cpDestino: cp.trim(),
        items: Object.values(seleccion).map(s => ({ varianteId: s.item.varianteId, cantidad: s.cantidad }))
      })
      setEnvio(data)
    } catch (err) {
      setEnvio({ disponible: false, mensaje: err.response?.data?.error || 'No pudimos cotizar el envío. Probá de nuevo o consultanos.' })
    } finally {
      setCotizando(false)
    }
  }

  const link = `${window.location.origin}/arma-tu-pc?armado=${aLink(seleccion)}`

  const compartir = async () => {
    if (navigator.share) {
      try {
        await navigator.share({ title: 'Mi PC armada en FuturaTecno', url: link })
        return
      } catch (e) {
        if (e?.name === 'AbortError') return   // cerró el menú de compartir
      }
    }
    try {
      await navigator.clipboard.writeText(link)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2500)
    } catch {
      window.prompt('Copiá este link:', link)
    }
  }

  const pasoActual = PASOS[paso]

  const { opciones, ocultos } = useMemo(() => {
    if (!componentes) return { opciones: [], ocultos: 0 }
    const q = busqueda.trim().toLowerCase()
    let ocultos = 0
    const lista = []
    for (const c of componentes) {
      if (c.tipo !== pasoActual.tipo) continue
      // El 2º disco es para sumar uno distinto: el mismo modelo se lleva con la cantidad del primero.
      if (pasoActual.clave === 'ALMACENAMIENTO_2' && c.varianteId === seleccion.ALMACENAMIENTO?.item?.varianteId) continue
      if (q && !`${c.marca} ${c.modelo}`.toLowerCase().includes(q)) continue
      const ev = evaluar(c, seleccion)
      if (ev.bloqueo) { ocultos++; continue }
      lista.push({ c, avisos: ev.avisos, sugerencias: ev.sugerencias || [], recomendada: ev.recomendada })
    }
    lista.sort((a, b) => (orden === 'asc' ? 1 : -1) * (a.c.precioUsd - b.c.precioUsd))
    return { opciones: lista, ocultos }
  }, [componentes, pasoActual, busqueda, orden, seleccion])

  const totales = useMemo(() => {
    let usd = 0, ars = 0
    for (const s of Object.values(seleccion)) {
      usd += Number(s.item.precioUsd) * s.cantidad
      ars += Number(s.item.precioArs) * s.cantidad
    }
    return { usd, ars }
  }, [seleccion])

  const faltantes = PASOS.filter(p => esObligatorio(p.clave, seleccion) && !seleccion[p.clave])
  const avisosResumen = PASOS.flatMap(p => {
    const s = seleccion[p.clave]
    if (!s) return []
    const resto = { ...seleccion }
    delete resto[p.clave]
    const ev = evaluar(s.item, resto)
    const avisos = ev.avisos?.map(a => `${p.titulo}: ${a}`) || []
    // El cuello de botella se ve desde los dos lados; en el resumen va una sola vez, del lado de la placa.
    if (p.clave === 'VIDEO') ev.sugerencias?.forEach(a => avisos.push(`Placa de video: ${a}`))
    const modulos = s.cantidad * (s.item.modulos || 1)
    if (p.clave === 'MEMORIA' && seleccion.MOTHER && !seleccion.MOTHER.item.ranurasRam && modulos > 2) {
      avisos.push(`Memoria RAM: son ${modulos} módulos y no sabemos si tu mother tiene esas ranuras`)
    }
    return avisos
  })

  const cantidadDe = (clave, c) => {
    const n = cantidades[`${clave}:${c.varianteId}`]
      ?? (seleccion[clave]?.item?.varianteId === c.varianteId ? seleccion[clave].cantidad : 1)
    return c.tipo === 'MEMORIA' ? Math.max(1, Math.min(n, maxUnidades(c, seleccion))) : n
  }

  const elegir = (clave, c) => {
    const cantidad = PASOS.find(p => p.clave === clave)?.cantidad ? cantidadDe(clave, c) : 1
    setSeleccion(prev => depurar({ ...prev, [clave]: { item: c, cantidad } }))
    if (paso < PASOS.length - 1) setPaso(paso + 1)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const quitar = (clave) => setSeleccion(prev => depurar(Object.fromEntries(Object.entries(prev).filter(([k]) => k !== clave))))

  const cambiarCantidad = (clave, n) => setSeleccion(prev => ({ ...prev, [clave]: { ...prev[clave], cantidad: n } }))

  /** En la tarjeta: si ya es la elegida, cambia la selección; si no, queda para cuando la elija. */
  const cambiarCantidadTarjeta = (clave, c, n) => {
    setCantidades(prev => ({ ...prev, [`${clave}:${c.varianteId}`]: n }))
    if (seleccion[clave]?.item?.varianteId === c.varianteId) cambiarCantidad(clave, n)
  }

  const opcionesCantidad = (tipo, item) => {
    const max = tipo === 'MEMORIA' ? maxUnidades(item, seleccion) : 4
    return Array.from({ length: Math.max(1, max) }, (_, i) => i + 1)
  }

  const etiquetaCantidad = (tipo, item, n) => {
    if (tipo !== 'MEMORIA') return `${n} u.`
    const modulos = n * (item.modulos || 1)
    return item.modulos > 1 ? `${n} kit${n > 1 ? 's' : ''} (${modulos} módulos)` : `${n} módulo${n > 1 ? 's' : ''}`
  }

  const agregarTodo = () => {
    for (const p of PASOS) {
      const s = seleccion[p.clave]
      if (!s) continue
      const c = s.item
      agregar(
        { id: c.productoId, marca: c.marca, modelo: c.modelo, sku: c.sku, imagenUrl: c.imagenUrl },
        { id: c.varianteId, especificaciones: c.especificaciones, precioUsd: c.precioUsd, precioArs: c.precioArs },
        s.cantidad
      )
    }
    navigate('/carrito')
  }

  const empezarDeNuevo = () => { setSeleccion({}); setPaso(0); setAvisoLink(null) }

  const mensajeWa = [
    'Hola FuturaTecno, armé esta PC y quiero consultar:',
    ...PASOS.filter(p => seleccion[p.clave]).map(p => {
      const s = seleccion[p.clave]
      return `• ${p.titulo}: ${s.item.modelo}${s.cantidad > 1 ? ` (x${s.cantidad})` : ''}`
    }),
    `Total: US$ ${formatNumber(totales.usd)}`,
    link
  ].join('\n')
  const waLink = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(mensajeWa)}`
  const hayAlgo = Object.keys(seleccion).length > 0

  const nota = notaDelPaso(pasoActual.clave, seleccion)
  const opcional = !esObligatorio(pasoActual.clave, seleccion)

  return (
    <div className="atp">
      <header className="atp-intro">
        <span className="atp-eyebrow">Armá tu PC</span>
        <h1>Elegí cada componente, nosotros cuidamos la compatibilidad.</h1>
        <p>En cada paso te mostramos solo lo que es compatible con lo que ya elegiste.</p>
      </header>

      <nav className="atp-pasos" aria-label="Pasos del armado">
        {PASOS.map((p, i) => {
          const elegido = seleccion[p.clave]
          return (
            <button
              key={p.clave}
              className={`atp-paso${i === paso ? ' activo' : ''}${elegido ? ' hecho' : ''}`}
              onClick={() => setPaso(i)}
              aria-current={i === paso ? 'step' : undefined}
            >
              <span className="atp-paso-icono" aria-hidden="true">{elegido ? '✓' : p.icono}</span>
              <span className="atp-paso-titulo">{p.titulo}</span>
            </button>
          )
        })}
      </nav>

      <div className="atp-layout">
        <section className="atp-lista">
          <div className="atp-lista-header">
            <h2>{pasoActual.icono} {pasoActual.titulo}{opcional && <span className="atp-opcional">opcional</span>}</h2>
            {nota && <p className="atp-nota">{nota}</p>}
            <div className="atp-filtros">
              <input
                type="search"
                placeholder={`Buscar ${pasoActual.titulo.toLowerCase()}…`}
                value={busqueda}
                onChange={e => { setBusqueda(e.target.value); setVisibles(POR_PAGINA) }}
              />
              <select value={orden} onChange={e => setOrden(e.target.value)} aria-label="Ordenar">
                <option value="asc">Menor precio</option>
                <option value="desc">Mayor precio</option>
              </select>
              {opcional && paso < PASOS.length - 1 && (
                <button className="btn btn-secondary" onClick={() => setPaso(paso + 1)}>Omitir paso →</button>
              )}
            </div>
            {avisoLink && <p className="atp-aviso-link">🔗 {avisoLink}</p>}
            {ocultos > 0 && (
              <p className="atp-ocultos">Ocultamos {ocultos} {ocultos === 1 ? 'opción incompatible' : 'opciones incompatibles'} con lo que elegiste.</p>
            )}
          </div>

          {error && <div className="card"><p>{error}</p></div>}
          {!componentes && !error && <div className="card"><p>Cargando componentes…</p></div>}
          {componentes && opciones.length === 0 && (
            <div className="card"><p>No hay opciones compatibles{busqueda ? ' que coincidan con la búsqueda' : ''}.</p></div>
          )}

          <div className="atp-grid">
            {opciones.slice(0, visibles).map(({ c, avisos, sugerencias, recomendada }) => {
              const elegido = seleccion[pasoActual.clave]?.item?.varianteId === c.varianteId
              return (
                <article key={c.varianteId} className={`atp-card${elegido ? ' elegido' : ''}`}>
                  <div className="atp-card-img">
                    {c.imagenUrl
                      ? <img src={c.imagenUrl} alt={`${c.marca} ${c.modelo}`} loading="lazy" width="160" height="120"
                             onError={e => { e.currentTarget.style.visibility = 'hidden' }} />
                      : <span>Sin imagen</span>}
                  </div>
                  <h3>{c.modelo}</h3>
                  <div className="atp-atributos">
                    {atributos(c).map(a => <span key={a}>{a}</span>)}
                  </div>
                  {recomendada && <p className="atp-recomendada">✓ Recomendada para tu procesador</p>}
                  {avisos.map(a => <p key={a} className="atp-aviso">⚠ {a}</p>)}
                  {sugerencias.map(a => <p key={a} className="atp-sugerencia">💡 {a}</p>)}
                  <div className="atp-card-precio">
                    <strong>US$ {formatNumber(c.precioUsd)}</strong>
                    <PaymentPrices transferPrice={c.precioArs} compact />
                  </div>
                  <div className="atp-card-acciones">
                    {pasoActual.cantidad && (
                      <select
                        value={cantidadDe(pasoActual.clave, c)}
                        onChange={e => cambiarCantidadTarjeta(pasoActual.clave, c, Number(e.target.value))}
                        aria-label="Cantidad"
                      >
                        {opcionesCantidad(c.tipo, c).map(n => <option key={n} value={n}>{etiquetaCantidad(c.tipo, c, n)}</option>)}
                      </select>
                    )}
                    <button className={`btn ${elegido ? 'btn-secondary' : 'btn-primary'}`} onClick={() => elegir(pasoActual.clave, c)}>
                      {elegido ? 'Elegido ✓' : 'Elegir'}
                    </button>
                  </div>
                </article>
              )
            })}
          </div>
          {opciones.length > visibles && (
            <button className="btn btn-secondary atp-mas" onClick={() => setVisibles(v => v + POR_PAGINA)}>
              Ver más ({opciones.length - visibles})
            </button>
          )}
        </section>

        <aside className={`atp-resumen${resumenAbierto ? ' abierto' : ''}`}>
          <button className="atp-resumen-barra" onClick={() => setResumenAbierto(v => !v)}>
            <span>Tu PC · {Object.keys(seleccion).length} de {PASOS.length}</span>
            <strong>US$ {formatNumber(totales.usd)}</strong>
            <span aria-hidden="true">{resumenAbierto ? '▾' : '▴'}</span>
          </button>
          <div className="atp-resumen-cuerpo">
            <h2>Tu PC</h2>
            <ul>
              {PASOS.map((p, i) => {
                const s = seleccion[p.clave]
                return (
                  <li key={p.clave}>
                    <button className="atp-resumen-paso" onClick={() => { setPaso(i); setResumenAbierto(false) }}>{p.titulo}</button>
                    {s ? (
                      <div className="atp-resumen-item">
                        <span className="atp-resumen-nombre">{s.item.modelo}</span>
                        <div className="atp-resumen-fila">
                          {p.cantidad ? (
                            <select value={s.cantidad} onChange={e => cambiarCantidadTarjeta(p.clave, s.item, Number(e.target.value))} aria-label="Cantidad">
                              {opcionesCantidad(p.tipo, s.item).map(n => <option key={n} value={n}>{etiquetaCantidad(p.tipo, s.item, n)}</option>)}
                            </select>
                          ) : <span />}
                          <span>US$ {formatNumber(s.item.precioUsd * s.cantidad)}</span>
                          <button className="atp-quitar" onClick={() => quitar(p.clave)} aria-label={`Quitar ${p.titulo}`}>✕</button>
                        </div>
                      </div>
                    ) : (
                      <span className="atp-resumen-vacio">{esObligatorio(p.clave, seleccion) ? 'Falta elegir' : 'Opcional'}</span>
                    )}
                  </li>
                )
              })}
            </ul>

            {avisosResumen.length > 0 && (
              <div className="atp-resumen-avisos">
                {avisosResumen.map(a => <p key={a}>⚠ {a}</p>)}
              </div>
            )}

            <div className="atp-total">
              <span>Total</span>
              <strong>US$ {formatNumber(totales.usd)}</strong>
              <PaymentPrices transferPrice={totales.ars} compact />
            </div>
            <button className="btn btn-primary atp-agregar" disabled={faltantes.length > 0} onClick={agregarTodo}>
              Agregar todo al carrito
            </button>
            {faltantes.length > 0 && (
              <p className="atp-faltan">Falta: {faltantes.map(f => f.titulo).join(', ')}.</p>
            )}
            <p className="atp-legal">Stock sujeto a disponibilidad. El precio final se confirma al hacer el pedido.</p>

            {hayAlgo && (
              <div className="atp-envio">
                <span className="atp-envio-titulo">Calcular envío</span>
                <div className="atp-envio-form">
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="Código postal"
                    value={cp}
                    onChange={e => setCp(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    onKeyDown={e => { if (e.key === 'Enter' && cp.length === 4) cotizarEnvio() }}
                    aria-label="Código postal"
                  />
                  <button className="btn btn-secondary" onClick={cotizarEnvio} disabled={cotizando || cp.length !== 4}>
                    {cotizando ? 'Cotizando…' : 'Cotizar'}
                  </button>
                </div>
                {envio && !envio.disponible && <p className="atp-envio-msg">{envio.mensaje}</p>}
                {envio?.disponible && (
                  <>
                    <ul className="atp-envio-opciones">
                      {envio.opciones.map(o => (
                        <li key={o.codigo}>
                          <span>{etiquetaEnvio(o.codigo)}</span>
                          <strong>{Number(o.totalArs) === 0 ? 'Gratis' : `$ ${formatNumber(o.totalArs)}`}</strong>
                        </li>
                      ))}
                    </ul>
                    <p className="atp-envio-msg">Cotizado para todo el armado junto. La modalidad se elige al confirmar el pedido.</p>
                  </>
                )}
              </div>
            )}

            {hayAlgo && (
              <div className="atp-compartir">
                <button className="btn btn-secondary" onClick={compartir}>{copiado ? '✓ Link copiado' : '🔗 Compartir armado'}</button>
                <a className="btn btn-secondary" href={waLink} target="_blank" rel="noreferrer">💬 Consultar por WhatsApp</a>
              </div>
            )}

            {hayAlgo && (
              <button className="atp-reiniciar" onClick={empezarDeNuevo}>Empezar de nuevo</button>
            )}
          </div>
        </aside>
      </div>
    </div>
  )
}
