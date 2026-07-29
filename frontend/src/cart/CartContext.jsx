import { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react'
import axios from 'axios'

const CartContext = createContext(null)

const STORAGE_KEY = 'carrito'

// El carrito vive en localStorage y NO requiere sesión: cualquiera lo arma, la cuenta se pide
// recién al confirmar. Por eso sobrevive al login (se asocia al usuario al crear el pedido).
const leerGuardado = () => {
  try {
    const crudo = localStorage.getItem(STORAGE_KEY)
    if (!crudo) return []
    const items = JSON.parse(crudo)
    return Array.isArray(items) ? items.filter(i => i && i.varianteId && i.cantidad > 0) : []
  } catch {
    return []
  }
}

export function CartProvider({ children }) {
  const [items, setItems] = useState(leerGuardado)

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
    } catch {
      // Sin localStorage (modo privado en algunos navegadores) el carrito vive solo en memoria.
    }
  }, [items])

  /**
   * Agrega una variante. Si ya estaba, suma la cantidad en vez de duplicar el renglón.
   * Los precios que se guardan son solo para mostrar: el pedido los recalcula en el backend.
   */
  const agregar = useCallback((producto, variante, cantidad = 1) => {
    setItems(prev => {
      const i = prev.findIndex(x => x.varianteId === variante.id)
      if (i >= 0) {
        const copia = [...prev]
        copia[i] = { ...copia[i], cantidad: copia[i].cantidad + cantidad }
        return copia
      }
      return [...prev, {
        varianteId: variante.id,
        productoId: producto.id,
        nombre: [producto.marca, producto.modelo].filter(Boolean).join(' '),
        sku: producto.sku,
        especificaciones: variante.especificaciones || null,
        imagenUrl: producto.imagenUrl || (producto.imagenes && producto.imagenes[0]) || null,
        precioUsd: Number(variante.precioUsd),
        precioArs: Number(variante.precioArs),
        cantidad
      }]
    })
  }, [])

  const quitar = useCallback((varianteId) => {
    setItems(prev => prev.filter(i => i.varianteId !== varianteId))
  }, [])

  const cambiarCantidad = useCallback((varianteId, cantidad) => {
    const n = Number(cantidad)
    if (!Number.isFinite(n) || n < 1) return
    setItems(prev => prev.map(i => i.varianteId === varianteId ? { ...i, cantidad: n } : i))
  }, [])

  const vaciar = useCallback(() => setItems([]), [])

  /**
   * Vuelve a pedir los precios al backend y actualiza el carrito.
   *
   * Hace falta porque el precio de venta se recalcula con la cotización del día y los costos del
   * mayorista: un carrito armado ayer muestra números viejos. Devuelve el detalle de lo que cambió
   * para poder avisarle al usuario antes de que confirme.
   */
  const revalidar = useCallback(async () => {
    if (items.length === 0) return { cambios: [], removidos: [] }

    const idsProducto = [...new Set(items.map(i => i.productoId))].filter(Boolean)
    const productos = await Promise.all(
      idsProducto.map(id =>
        axios.get(`/api/productos/${id}`).then(r => r.data).catch(() => null))
    )

    const porVariante = new Map()
    for (const p of productos) {
      if (!p) continue
      for (const v of (p.variantes || [])) porVariante.set(v.id, { producto: p, variante: v })
    }

    const cambios = []
    const removidos = []
    const actualizados = []

    for (const item of items) {
      const encontrado = porVariante.get(item.varianteId)
      if (!encontrado) {
        // El artículo se dio de baja o quedó sin variantes activas.
        removidos.push(item)
        continue
      }
      const nuevoUsd = Number(encontrado.variante.precioUsd)
      if (nuevoUsd !== item.precioUsd) {
        cambios.push({ nombre: item.nombre, anterior: item.precioUsd, nuevo: nuevoUsd })
      }
      actualizados.push({
        ...item,
        nombre: [encontrado.producto.marca, encontrado.producto.modelo].filter(Boolean).join(' '),
        sku: encontrado.producto.sku,
        especificaciones: encontrado.variante.especificaciones || null,
        imagenUrl: encontrado.producto.imagenUrl || (encontrado.producto.imagenes || [])[0] || null,
        precioUsd: nuevoUsd,
        precioArs: Number(encontrado.variante.precioArs)
      })
    }

    setItems(actualizados)
    return { cambios, removidos }
  }, [items])

  const { cantidadTotal, totalUsd, totalArs } = useMemo(() => ({
    cantidadTotal: items.reduce((acc, i) => acc + i.cantidad, 0),
    totalUsd: items.reduce((acc, i) => acc + i.precioUsd * i.cantidad, 0),
    totalArs: items.reduce((acc, i) => acc + i.precioArs * i.cantidad, 0)
  }), [items])

  const value = {
    items, agregar, quitar, cambiarCantidad, vaciar, revalidar,
    cantidadTotal, totalUsd, totalArs,
    vacio: items.length === 0
  }

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export const useCart = () => useContext(CartContext)
