import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import { useCart } from '../../cart/CartContext'
import PaymentPrices from '../../components/PaymentPrices'
import './ArmaTuPcPage.css'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const STORAGE_KEY = 'armaTuPc'
const POR_PAGINA = 24
const FORMATOS = ['ITX', 'MATX', 'ATX', 'EATX']
const NOMBRE_FORMATO = { ITX: 'Mini-ITX', MATX: 'Micro-ATX', ATX: 'ATX', EATX: 'E-ATX' }
const RAM_DE_SOCKET = { AM4: 'DDR4', AM5: 'DDR5', LGA1851: 'DDR5', LGA1200: 'DDR4' }

/**
 * Orden de armado. Cada paso filtra por lo elegido en los anteriores, así que el orden importa:
 * el socket lo fija el procesador, el tipo de RAM lo fija el mother.
 * `cantidad`: se puede llevar más de una unidad (dos memorias, dos discos).
 */
const PASOS = [
  { tipo: 'PROCESADOR', titulo: 'Procesador', icono: '🧠' },
  { tipo: 'MOTHER', titulo: 'Motherboard', icono: '🧩' },
  { tipo: 'MEMORIA', titulo: 'Memoria RAM', icono: '💾', cantidad: true },
  { tipo: 'VIDEO', titulo: 'Placa de video', icono: '🎮' },
  { tipo: 'ALMACENAMIENTO', titulo: 'Almacenamiento', icono: '🗄️', cantidad: true },
  { tipo: 'FUENTE', titulo: 'Fuente', icono: '⚡' },
  { tipo: 'GABINETE', titulo: 'Gabinete', icono: '🖥️' },
  { tipo: 'COOLER', titulo: 'Cooler', icono: '❄️' },
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
  return { avisos }
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
function esObligatorio(tipo, sel) {
  const cpu = sel.PROCESADOR?.item
  if (tipo === 'VIDEO') return cpu?.videoIntegrado === false
  if (tipo === 'COOLER') return cpu?.incluyeCooler === false
  return true
}

function notaDelPaso(tipo, sel) {
  const cpu = sel.PROCESADOR?.item
  if (tipo === 'MEMORIA') {
    const mother = sel.MOTHER?.item
    if (!mother) return 'Podés llevar más de un módulo: elegí la cantidad en cada opción.'
    return mother.ranurasRam
      ? `Tu mother tiene ${mother.ranurasRam} ranuras de memoria: podés poner hasta ${mother.ranurasRam} módulos.`
      : 'No sabemos cuántas ranuras de memoria tiene tu mother (suelen ser 2 o 4): verificalo antes de llevar más de dos módulos.'
  }
  if (tipo === 'VIDEO' && cpu) {
    return cpu.videoIntegrado === false
      ? 'Tu procesador no tiene video integrado: necesitás una placa de video.'
      : cpu.videoIntegrado ? 'Tu procesador tiene video integrado: la placa de video es opcional.' : null
  }
  if (tipo === 'COOLER' && cpu) {
    if (cpu.incluyeCooler === false) return 'Tu procesador viene sin cooler: necesitás uno.'
    if (cpu.incluyeCooler) return 'Tu procesador trae cooler en la caja: este paso es opcional.'
    return 'No sabemos si tu procesador trae cooler: si no lo trae, sumá uno.'
  }
  return null
}

/** Saca lo elegido que dejó de ser compatible después de un cambio (en el orden de armado). */
function depurar(sel) {
  const limpio = {}
  for (const p of PASOS) {
    const actual = sel[p.tipo]
    if (!actual) continue
    const { bloqueo } = evaluar(actual.item, limpio)
    if (bloqueo) continue
    limpio[p.tipo] = p.tipo === 'MEMORIA'
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

  useEffect(() => {
    axios.get('/api/arma-tu-pc/componentes')
      .then(res => setComponentes(res.data))
      .catch(() => setError('No pudimos cargar los componentes. Probá de nuevo en unos minutos.'))
  }, [])

  // Lo guardado puede tener precios viejos o productos que ya no están: se reemplaza por la
  // versión de hoy, y lo que desapareció se descarta.
  useEffect(() => {
    if (!componentes) return
    const porVariante = new Map(componentes.map(c => [c.varianteId, c]))
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

  const pasoActual = PASOS[paso]

  const { opciones, ocultos } = useMemo(() => {
    if (!componentes) return { opciones: [], ocultos: 0 }
    const q = busqueda.trim().toLowerCase()
    let ocultos = 0
    const lista = []
    for (const c of componentes) {
      if (c.tipo !== pasoActual.tipo) continue
      if (q && !`${c.marca} ${c.modelo}`.toLowerCase().includes(q)) continue
      const ev = evaluar(c, seleccion)
      if (ev.bloqueo) { ocultos++; continue }
      lista.push({ c, avisos: ev.avisos })
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

  const faltantes = PASOS.filter(p => esObligatorio(p.tipo, seleccion) && !seleccion[p.tipo])
  const avisosResumen = PASOS.flatMap(p => {
    const s = seleccion[p.tipo]
    if (!s) return []
    const resto = { ...seleccion }
    delete resto[p.tipo]
    const avisos = evaluar(s.item, resto).avisos?.map(a => `${p.titulo}: ${a}`) || []
    const modulos = s.cantidad * (s.item.modulos || 1)
    if (p.tipo === 'MEMORIA' && seleccion.MOTHER && !seleccion.MOTHER.item.ranurasRam && modulos > 2) {
      avisos.push(`Memoria RAM: son ${modulos} módulos y no sabemos si tu mother tiene esas ranuras`)
    }
    return avisos
  })

  const cantidadDe = (c) => {
    const n = cantidades[c.varianteId]
      ?? (seleccion[c.tipo]?.item?.varianteId === c.varianteId ? seleccion[c.tipo].cantidad : 1)
    return c.tipo === 'MEMORIA' ? Math.max(1, Math.min(n, maxUnidades(c, seleccion))) : n
  }

  const elegir = (c) => {
    const cantidad = PASOS.find(p => p.tipo === c.tipo)?.cantidad ? cantidadDe(c) : 1
    setSeleccion(prev => depurar({ ...prev, [c.tipo]: { item: c, cantidad } }))
    if (paso < PASOS.length - 1) setPaso(paso + 1)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const quitar = (tipo) => setSeleccion(prev => depurar(Object.fromEntries(Object.entries(prev).filter(([t]) => t !== tipo))))

  const cambiarCantidad = (tipo, n) => setSeleccion(prev => ({ ...prev, [tipo]: { ...prev[tipo], cantidad: n } }))

  /** En la tarjeta: si ya es la elegida, cambia la selección; si no, queda para cuando la elija. */
  const cambiarCantidadTarjeta = (c, n) => {
    setCantidades(prev => ({ ...prev, [c.varianteId]: n }))
    if (seleccion[c.tipo]?.item?.varianteId === c.varianteId) cambiarCantidad(c.tipo, n)
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
      const s = seleccion[p.tipo]
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

  const empezarDeNuevo = () => { setSeleccion({}); setPaso(0) }

  const nota = notaDelPaso(pasoActual.tipo, seleccion)
  const opcional = !esObligatorio(pasoActual.tipo, seleccion)

  return (
    <div className="atp">
      <header className="atp-intro">
        <span className="atp-eyebrow">Armá tu PC</span>
        <h1>Elegí cada componente, nosotros cuidamos la compatibilidad.</h1>
        <p>En cada paso te mostramos solo lo que es compatible con lo que ya elegiste.</p>
      </header>

      <nav className="atp-pasos" aria-label="Pasos del armado">
        {PASOS.map((p, i) => {
          const elegido = seleccion[p.tipo]
          return (
            <button
              key={p.tipo}
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
            {opciones.slice(0, visibles).map(({ c, avisos }) => {
              const elegido = seleccion[c.tipo]?.item?.varianteId === c.varianteId
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
                  {avisos.map(a => <p key={a} className="atp-aviso">⚠ {a}</p>)}
                  <div className="atp-card-precio">
                    <strong>US$ {formatNumber(c.precioUsd)}</strong>
                    <PaymentPrices transferPrice={c.precioArs} compact />
                  </div>
                  <div className="atp-card-acciones">
                    {pasoActual.cantidad && (
                      <select
                        value={cantidadDe(c)}
                        onChange={e => cambiarCantidadTarjeta(c, Number(e.target.value))}
                        aria-label="Cantidad"
                      >
                        {opcionesCantidad(c.tipo, c).map(n => <option key={n} value={n}>{etiquetaCantidad(c.tipo, c, n)}</option>)}
                      </select>
                    )}
                    <button className={`btn ${elegido ? 'btn-secondary' : 'btn-primary'}`} onClick={() => elegir(c)}>
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
                const s = seleccion[p.tipo]
                return (
                  <li key={p.tipo}>
                    <button className="atp-resumen-paso" onClick={() => { setPaso(i); setResumenAbierto(false) }}>{p.titulo}</button>
                    {s ? (
                      <div className="atp-resumen-item">
                        <span className="atp-resumen-nombre">{s.item.modelo}</span>
                        <div className="atp-resumen-fila">
                          {p.cantidad ? (
                            <select value={s.cantidad} onChange={e => cambiarCantidadTarjeta(s.item, Number(e.target.value))} aria-label="Cantidad">
                              {opcionesCantidad(p.tipo, s.item).map(n => <option key={n} value={n}>{etiquetaCantidad(p.tipo, s.item, n)}</option>)}
                            </select>
                          ) : <span />}
                          <span>US$ {formatNumber(s.item.precioUsd * s.cantidad)}</span>
                          <button className="atp-quitar" onClick={() => quitar(p.tipo)} aria-label={`Quitar ${p.titulo}`}>✕</button>
                        </div>
                      </div>
                    ) : (
                      <span className="atp-resumen-vacio">{esObligatorio(p.tipo, seleccion) ? 'Falta elegir' : 'Opcional'}</span>
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
            {Object.keys(seleccion).length > 0 && (
              <button className="atp-reiniciar" onClick={empezarDeNuevo}>Empezar de nuevo</button>
            )}
          </div>
        </aside>
      </div>
    </div>
  )
}
