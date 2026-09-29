import { useState } from 'react'

// Filtros por características de la categoría elegida (iPhone, Celulares…). Los arma el servidor:
// cada opción trae cuántos productos quedan con los OTROS filtros aplicados, así que solo se
// muestra lo que existe. Dentro de un filtro se pueden elegir varias opciones (256GB o 512GB).

const COLOR_A_CONSULTAR = 'Color a consultar'
const MAX_VISIBLES = 8   // si hay más opciones (colores de celulares), se esconden detrás de "Ver más"

// Punto de color para los nombres que se pueden dibujar con certeza. Un nombre de fabricante
// sin equivalente claro ("Lily Pad", "Zephyr") va sin punto: mejor nada que un color engañoso.
const MUESTRAS = {
  black: '#111', white: '#f5f5f5', silver: '#c9ccd1', gray: '#8a8d91', grey: '#8a8d91', 'light gray': '#bfc2c6',
  graphite: '#4a4b4f', blue: '#2f6fd6', 'sky blue': '#8ec5f0', 'ice blue': '#b9dcf2', iceblue: '#b9dcf2',
  'icy blue': '#b9dcf2', navy: '#1f2f5c', ultramarine: '#3f4fd8', teal: '#2a8c8c', cyan: '#27c2d6',
  green: '#3a9a4f', sage: '#9caf88', olive: '#7a7d3a', lime: '#b7d63a', emerald: '#1f8a5b', mint: '#9fe3c4',
  pink: '#f2a7c3', red: '#d33a3a', cherry: '#8e1b2e', burgundy: '#6d1a2a', orange: '#f28a2e', yellow: '#f2d23a',
  gold: '#d4af37', violet: '#8a5cd6', purple: '#7a4fc2', lavender: '#c6b3ef', lavander: '#c6b3ef',
  desert: '#c9a57d', cream: '#f1e6cf', midnight: '#1d2433', starlight: '#efe6d6', titanium: '#9b9b98',
  'natural titanium': '#b8b2a7', 'black titanium': '#3a3a3c', 'white titanium': '#e8e6e1', 'blue titanium': '#44546a',
  'desert titanium': '#bfa48a', blueberry: '#3b3f8f', glacier: '#d9e8f0', 'charcoal black': '#2b2b2b', walnut: '#6b4a32',
}

function Opcion({ filtro, opcion, onToggle }) {
  const muestra = filtro.clave === 'color' ? MUESTRAS[opcion.valor.toLowerCase()] : null
  const consultar = opcion.valor === COLOR_A_CONSULTAR
  return (
    <button
      type="button"
      className={`filtro-opcion${opcion.seleccionada ? ' activa' : ''}${consultar ? ' consultar' : ''}`}
      aria-pressed={opcion.seleccionada}
      onClick={() => onToggle(filtro.clave, opcion.valor)}
    >
      {muestra && <span className="filtro-muestra" style={{ background: muestra }} aria-hidden="true" />}
      <span>{opcion.valor}</span>
      <span className="filtro-cantidad" aria-label={`${opcion.cantidad} productos`}>{opcion.cantidad}</span>
    </button>
  )
}

function Grupo({ filtro, onToggle }) {
  const [verTodo, setVerTodo] = useState(false)
  // Las elegidas siempre a la vista, aunque estén más abajo en la lista.
  const visibles = verTodo ? filtro.opciones
    : filtro.opciones.filter((o, i) => i < MAX_VISIBLES || o.seleccionada)
  const ocultas = filtro.opciones.length - visibles.length
  return (
    <div className="filtro-grupo" role="group" aria-label={filtro.nombre}>
      <div className="filtro-titulo">{filtro.nombre}</div>
      <div className="filtro-opciones">
        {visibles.map(o => <Opcion key={o.valor} filtro={filtro} opcion={o} onToggle={onToggle} />)}
        {ocultas > 0 && (
          <button type="button" className="filtro-ver-mas" onClick={() => setVerTodo(true)}>Ver {ocultas} más</button>
        )}
        {verTodo && filtro.opciones.length > MAX_VISIBLES && (
          <button type="button" className="filtro-ver-mas" onClick={() => setVerTodo(false)}>Ver menos</button>
        )}
      </div>
    </div>
  )
}

export default function FiltrosAtributos({ filtros, onToggle, onLimpiar }) {
  if (!filtros?.length) return null
  const activos = filtros.some(f => f.opciones.some(o => o.seleccionada))
  return (
    <div className="filtros-atributos">
      <div className="filtros-atributos-encabezado">
        <span>Filtrá por características</span>
        {activos && <button type="button" className="filtro-ver-mas" onClick={onLimpiar}>Quitar estos filtros</button>}
      </div>
      {filtros.map(f => <Grupo key={f.clave} filtro={f} onToggle={onToggle} />)}
    </div>
  )
}
