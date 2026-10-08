import { useEffect, useState } from 'react'
import axios from 'axios'

// Compra mínima en productos. El backend la valida en USD (`PedidoService`); al cliente se le
// muestra solo en pesos, con la cotización del día.
export const MONTO_MINIMO_PEDIDO_USD = 250

// La cotización se pide una sola vez por carga de página y se comparte entre pantallas.
let pedido = null
const pedirCotizacion = () => {
  if (!pedido) pedido = axios.get('/api/cotizacion').then(r => Number(r.data?.valor) || null).catch(() => { pedido = null; return null })
  return pedido
}

/** Cotización del dólar que usa la tienda (null mientras carga o si falla). */
export function useCotizacion() {
  const [valor, setValor] = useState(null)
  useEffect(() => {
    let vivo = true
    pedirCotizacion().then(v => { if (vivo) setValor(v) })
    return () => { vivo = false }
  }, [])
  return valor
}

/**
 * Mínimo en pesos, redondeado a miles HACIA ARRIBA: si se redondeara para abajo, un carrito que
 * llega a la cifra mostrada podría quedar unos pesos por debajo de los US$ 250 reales y el
 * backend lo rechazaría.
 */
export const minimoArs = (cotizacion) =>
  cotizacion ? Math.ceil((MONTO_MINIMO_PEDIDO_USD * cotizacion) / 1000) * 1000 : null

export const formatPesos = (n) => Number(n).toLocaleString('es-AR', { maximumFractionDigits: 0 })

/** Texto del aviso: los precios publicados tienen descuento y rigen desde la compra mínima. */
export const textoMinimo = (cotizacion) => {
  const min = minimoArs(cotizacion)
  return min
    ? `Los precios publicados tienen descuento y son válidos para compras desde $\u00a0${formatPesos(min)} en productos.`
    : 'Los precios publicados tienen descuento y son válidos a partir de una compra mínima en productos.'
}
