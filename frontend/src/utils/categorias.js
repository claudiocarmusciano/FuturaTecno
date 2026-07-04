// Helpers para el árbol de categorías (GET /api/categorias): [{id, nombre, hijos: [...]}].

// Indexa el árbol para poder ir de un id a su nodo o a su padre (necesario para armar
// selects en cascada o para reconstruir la cadena sección/categoría de una hoja).
export const indexarArbol = (arbol) => {
  const padreDe = {}
  const nodoDe = {}
  const recorrer = (nodos, padreId) => {
    for (const n of nodos) {
      nodoDe[n.id] = n
      padreDe[n.id] = padreId
      if (n.hijos?.length) recorrer(n.hijos, n.id)
    }
  }
  recorrer(arbol || [], null)
  return { padreDe, nodoDe }
}

// Ids de las hojas (subcategorías) bajo un nodo, incluyéndolo si ya es una hoja.
// Sirve para filtrar productos por cualquier nivel del árbol sin comparar por nombre
// (nombres como "Imagen" se repiten en ramas distintas).
export const idsHojaDe = (nodo) => {
  if (!nodo.hijos || nodo.hijos.length === 0) return [nodo.id]
  return nodo.hijos.flatMap(idsHojaDe)
}
