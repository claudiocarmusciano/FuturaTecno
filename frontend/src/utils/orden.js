// Orden alfabético en español. `localeCompare` con sensitivity 'base' resuelve de una las tres
// cosas que rompen un `.sort()` pelado sobre nombres en castellano: la "Ñ" va después de la "N"
// (y no al final, como haría el orden por código), los acentos ordenan junto a su vocal
// ("Cámaras" cae entre "Cables" y "Cargadores") y no separa mayúsculas de minúsculas.
export const compararTexto = (a, b) =>
  String(a ?? '').localeCompare(String(b ?? ''), 'es', { sensitivity: 'base' })

// Devuelve una copia ordenada: `sort` muta, y estas listas suelen venir de props o de estado.
// Sin `selector` ordena por el valor mismo (listas de strings).
export const ordenarPor = (lista, selector = x => x) =>
  [...(lista || [])].sort((a, b) => compararTexto(selector(a), selector(b)))
