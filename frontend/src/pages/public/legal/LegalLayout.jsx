import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { TITULAR } from '../../../config'
import './LegalBase.css'
import './legal.css'

/** Marco común de las páginas legales: tarjeta centrada sobre fondo oscuro. */
export default function LegalLayout({ eyebrow = 'FUTURATECNO', titulo, intro, actualizado, children }) {
  useEffect(() => {
    const anterior = document.title
    document.title = `${titulo} — FuturaTecno`
    window.scrollTo(0, 0)
    return () => { document.title = anterior }
  }, [titulo])

  return (
    <main className="bases-page">
      <header className="bases-header">
        <Link to="/"><img src="/logo.png?v=2" alt="FuturaTecno" /></Link>
        <Link to="/catalogo" className="bases-back">← Volver a la tienda</Link>
      </header>
      <article className="bases-card legal-card">
        <span className="bases-eyebrow">{eyebrow}</span>
        <h1>{titulo}</h1>
        {intro && <p className="bases-intro">{intro}</p>}
        {children}
        {actualizado && <p className="legal-actualizado">Última actualización: {actualizado}.</p>}
        <nav className="legal-otros" aria-label="Otras páginas legales">
          <Link to="/terminos">Términos y condiciones</Link>
          <Link to="/garantia">Garantía y devoluciones</Link>
          <Link to="/privacidad">Privacidad</Link>
          <Link to="/arrepentimiento">Botón de arrepentimiento</Link>
        </nav>
      </article>
      <p className="bases-footer">Futura Tecno es el nombre comercial de {TITULAR.nombre} · CUIT {TITULAR.cuit}</p>
    </main>
  )
}
