import { Link } from 'react-router-dom'
import { PROMO_ARMADO } from '../config'
import './PromoArmado.css'

const IconRegalo = () => (
  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.5"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="3" y="8" width="18" height="4" rx="1" /><path d="M12 8v13M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7" />
    <path d="M7.5 8a2.5 2.5 0 0 1 0-5C10 3 12 8 12 8s2-5 4.5-5a2.5 2.5 0 0 1 0 5" />
  </svg>
)

/** Aviso del beneficio de Armá tu PC. Con `enlace`, lleva al armador. */
export default function PromoArmado({ enlace = false, className = '' }) {
  const contenido = (
    <>
      <span className="promo-armado-ico"><IconRegalo /></span>
      <span className="promo-armado-txt">
        <strong>{PROMO_ARMADO.titulo}</strong>
        <span>{PROMO_ARMADO.texto}</span>
      </span>
      {enlace && <span className="promo-armado-cta">Armá tu PC ›</span>}
    </>
  )
  return enlace
    ? <Link to="/arma-tu-pc" className={`promo-armado ${className}`.trim()}>{contenido}</Link>
    : <div className={`promo-armado ${className}`.trim()} role="note">{contenido}</div>
}
