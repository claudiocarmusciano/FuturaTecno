/**
 * Comparación de los buscadores: sin distinguir mayúsculas ni tildes
 * ("Computación" = "COMPUTACION" = "computacion"), y por palabras sueltas en cualquier orden
 * ("lenovo 15" encuentra "Lenovo IdeaPad Slim 3 15.6").
 */
export const normalizarBusqueda = (texto) =>
  String(texto ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()

export const coincideBusqueda = (texto, termino) => {
  const palabras = normalizarBusqueda(termino).split(/\s+/).filter(Boolean)
  if (palabras.length === 0) return true
  const donde = normalizarBusqueda(texto)
  return palabras.every(p => donde.includes(p))
}
