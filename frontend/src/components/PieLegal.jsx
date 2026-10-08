import { Link } from 'react-router-dom'
import {
  TITULAR, CONTACTO_EMAIL, INSTAGRAM_URL, WHATSAPP_NUMBER, DEFENSA_CONSUMIDOR_URL, DATA_FISCAL_URL, DATA_FISCAL_IMG,
  NOMBRE_NEGOCIO, NOMBRE_ANTERIOR, HORARIO_LOCAL
} from '../config'
import './PieLegal.css'

/** Pie de página con los datos del titular y las páginas legales. Va en todas las páginas públicas. */
export default function PieLegal() {
  return (
    <footer className="pie-legal">
      <div className="pie-legal-in">
        <div className="pie-legal-col pie-legal-marca">
          <img src="/marca/tecnopolis-olavarria-logo-sobre-blanco.svg" alt={NOMBRE_NEGOCIO} width="168" />
          <span>{TITULAR.domicilioComercial}</span>
          <span>Horario: {HORARIO_LOCAL}</span>
        </div>
        <nav className="pie-legal-col" aria-label="Tienda">
          <b>Tienda</b>
          <Link to="/catalogo">Productos</Link>
          <Link to="/arma-tu-pc">Armá tu PC</Link>
          <Link to="/mis-pedidos">Mis pedidos</Link>
          <Link to="/mis-puntos">Mis puntos</Link>
        </nav>
        <nav className="pie-legal-col" aria-label="Información legal">
          <b>Ayuda</b>
          <Link to="/terminos">Términos y condiciones</Link>
          <Link to="/garantia">Garantía y devoluciones</Link>
          <Link to="/privacidad">Política de privacidad</Link>
          <Link to="/arrepentimiento" className="pie-legal-arr">Botón de arrepentimiento</Link>
          <a href={DEFENSA_CONSUMIDOR_URL} target="_blank" rel="noreferrer">Defensa del Consumidor</a>
        </nav>
        <div className="pie-legal-col">
          <b>Contacto</b>
          <a href={`https://wa.me/${WHATSAPP_NUMBER}`} target="_blank" rel="noreferrer">WhatsApp</a>
          <a href={INSTAGRAM_URL} target="_blank" rel="noreferrer">Instagram</a>
          <a href={`mailto:${CONTACTO_EMAIL}`}>{CONTACTO_EMAIL}</a>
          {DATA_FISCAL_URL && DATA_FISCAL_IMG && (
            <a href={DATA_FISCAL_URL} target="_blank" rel="noreferrer" className="pie-legal-qr">
              <img src={DATA_FISCAL_IMG} alt="Data Fiscal" width="64" />
            </a>
          )}
        </div>
      </div>
      <div className="pie-legal-copy">
        <span>© {new Date().getFullYear()} {NOMBRE_NEGOCIO} (antes {NOMBRE_ANTERIOR}) · nombre comercial de {TITULAR.nombre}</span>
        <span>CUIT {TITULAR.cuit} · {TITULAR.condicionIva}</span>
      </div>
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
