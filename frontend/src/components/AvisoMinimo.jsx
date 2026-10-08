import { useCotizacion, textoMinimo } from '../utils/minimoCompra'

/** Aviso de compra mínima, en pesos. */
export default function AvisoMinimo({ className = '', style }) {
  const cotizacion = useCotizacion()
  return <p className={`aviso-minimo ${className}`.trim()} style={style} role="note">{textoMinimo(cotizacion)}</p>
}
