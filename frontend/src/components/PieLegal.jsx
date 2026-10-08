import { Link } from 'react-router-dom'
import {
  TITULAR, CONTACTO_EMAIL, INSTAGRAM_URL, WHATSAPP_NUMBER, DATA_FISCAL_URL, DATA_FISCAL_IMG,
  NOMBRE_NEGOCIO, NOMBRE_ANTERIOR, HORARIO_LOCAL
} from '../config'
import './PieLegal.css'

/**
 * Pie de página con los datos del titular. Va en todas las páginas públicas.
 * Los links legales (términos, garantía, privacidad, botón de arrepentimiento, Defensa del
 * Consumidor) y la barra del botón de arrepentimiento se sacaron por decisión del usuario
 * (2026-10-08), advertido de que la Res. 424/2020 exige el botón visible en la primera pantalla.
 * Las páginas siguen existiendo en sus rutas.
 */
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
