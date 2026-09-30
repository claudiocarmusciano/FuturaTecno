import { Link } from 'react-router-dom'
import {
  TITULAR, CONTACTO_EMAIL, INSTAGRAM_URL, WHATSAPP_NUMBER, DEFENSA_CONSUMIDOR_URL, DATA_FISCAL_URL, DATA_FISCAL_IMG
} from '../config'
import './PieLegal.css'

/** Pie de página con los datos del titular y las páginas legales. Va en todas las páginas públicas. */
export default function PieLegal() {
  return (
    <footer className="pie-legal">
      <div className="pie-legal-in">
        <div className="pie-legal-col">
          <b>FuturaTecno</b>
          <span>Futura Tecno es el nombre comercial de {TITULAR.nombre}</span>
          <span>CUIT {TITULAR.cuit} · {TITULAR.condicionIva}</span>
          <span>{TITULAR.domicilioComercial}</span>
          <span><a href={`mailto:${CONTACTO_EMAIL}`}>{CONTACTO_EMAIL}</a></span>
        </div>
        <nav className="pie-legal-col" aria-label="Información legal">
          <Link to="/terminos">Términos y condiciones</Link>
          <Link to="/garantia">Garantía y devoluciones</Link>
          <Link to="/privacidad">Política de privacidad</Link>
          <Link to="/arrepentimiento" className="pie-legal-arr">Botón de arrepentimiento</Link>
          <a href={DEFENSA_CONSUMIDOR_URL} target="_blank" rel="noreferrer">Defensa del Consumidor</a>
        </nav>
        <div className="pie-legal-col">
          <a href={`https://wa.me/${WHATSAPP_NUMBER}`} target="_blank" rel="noreferrer">WhatsApp</a>
          <a href={INSTAGRAM_URL} target="_blank" rel="noreferrer">Instagram</a>
          {DATA_FISCAL_URL && DATA_FISCAL_IMG && (
            <a href={DATA_FISCAL_URL} target="_blank" rel="noreferrer" className="pie-legal-qr">
              <img src={DATA_FISCAL_IMG} alt="Data Fiscal" width="64" />
            </a>
          )}
        </div>
      </div>
      <div className="pie-legal-copy">© {new Date().getFullYear()} FuturaTecno · Tu tecnología. Tu futuro.</div>
    </footer>
  )
}

/**
 * La Res. 424/2020 pide el botón de arrepentimiento en la primera pantalla, visible y de fácil
 * acceso: por eso va en una barra fina arriba de todo y no dentro del menú (en mobile queda
 * escondido detrás de la hamburguesa).
 */
export function BarraArrepentimiento() {
  return (
    <div className="barra-arr">
      <Link to="/arrepentimiento">Botón de arrepentimiento</Link>
    </div>
  )
}
