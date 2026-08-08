import { Link } from 'react-router-dom'
import './PreLanding.css'

const whatsappUrl = 'https://wa.me/5492284381111?text=' + encodeURIComponent(
  '¡Hola FuturaTecno! Ya quiero participar del sorteo de la silla gamer.'
)
const instagramUrl = 'https://www.instagram.com/futuratecnoargentina'

const Icon = ({ children }) => <svg viewBox="0 0 24 24" aria-hidden="true">{children}</svg>

function PreLandingPage() {
  return (
    <main className="prelanding">
      <div className="prelanding-glow prelanding-glow-one" />
      <div className="prelanding-glow prelanding-glow-two" />

      <header className="prelanding-header">
        <Link to="/inicio" aria-label="Ir a FuturaTecno">
          <img src="/logo.png?v=2" alt="FuturaTecno" />
        </Link>
        <Link className="prelanding-skip" to="/inicio">Ya tengo cuenta · Ver catálogo →</Link>
      </header>

      <section className="prelanding-content">
        <div className="prelanding-copy">
          <span className="prelanding-badge"><span /> Sorteo especial de bienvenida</span>
          <h1>Tu próximo upgrade puede ser una <strong>silla gamer ergonómica.</strong></h1>
          <p className="prelanding-lead">
            Si es tu primera vez en FuturaTecno, registrate y completá estos pasos para participar del sorteo.
          </p>

          <ol className="prelanding-steps">
            <li>
              <span className="prelanding-step-number">1</span>
              <div><b>Registrate con tu número de WhatsApp.</b><small>Así podemos identificar tu participación y avisarte las novedades.</small></div>
              <Link className="prelanding-action primary" to="/registro">
                Registrarme <span>→</span>
              </Link>
            </li>
            <li>
              <span className="prelanding-step-number">2</span>
              <div><b>Abrí WhatsApp y agendanos.</b><small>Guardá el número de FuturaTecno para recibir novedades del sorteo.</small></div>
              <a className="prelanding-action whatsapp" href={whatsappUrl} target="_blank" rel="noreferrer">
                <Icon><path d="M20.5 3.5A11.8 11.8 0 0 0 12.1 0C5.6 0 .3 5.3.3 11.8c0 2.1.5 4.1 1.5 5.9L0 24l6.5-1.7a11.8 11.8 0 0 0 5.6 1.4h.1c6.5 0 11.8-5.3 11.8-11.8 0-3.2-1.2-6.1-3.5-8.4ZM12.1 21.7a9.8 9.8 0 0 1-5-1.4l-.4-.2-3.9 1 1-3.8-.2-.4a9.7 9.7 0 0 1-1.5-5.2C2.1 6.3 6.5 2 12 2c2.6 0 5.1 1 7 2.9a9.8 9.8 0 0 1 2.9 7c0 5.4-4.4 9.8-9.8 9.8Zm5.4-7.3c-.3-.1-1.8-.9-2.1-1s-.5-.1-.7.2-.8 1-1 1.2-.3.2-.6.1c-1.8-.9-3-2.5-3.3-3-.2-.3 0-.5.1-.6l.5-.5c.1-.2.2-.3.3-.5s0-.4 0-.5c-.1-.1-.7-1.7-1-2.3-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.8.4s-1 1-1 2.5 1.1 2.9 1.2 3.1c.2.2 2.1 3.3 5.1 4.6.7.3 1.3.5 1.7.6.7.2 1.4.2 1.9.1.6-.1 1.8-.7 2.1-1.4.3-.7.3-1.3.2-1.4-.1-.1-.3-.2-.6-.4Z" /></Icon>
                Abrir WhatsApp
              </a>
            </li>
            <li>
              <span className="prelanding-step-number">3</span>
              <div><b>Seguinos y etiquetá a 3 amigos en Instagram.</b><small>Hacelo en el posteo del sorteo para dejar registrada tu participación.</small></div>
              <a className="prelanding-action instagram" href={instagramUrl} target="_blank" rel="noreferrer">
                <Icon><rect x="2.5" y="2.5" width="19" height="19" rx="5" /><circle cx="12" cy="12" r="4.2" /><circle cx="17.7" cy="6.6" r="1" /></Icon>
                Ir a Instagram
              </a>
            </li>
          </ol>

          <div className="prelanding-reminder">
            <Icon><path d="M12 2 2.8 5.7v5.5c0 5.2 3.9 9.8 9.2 10.8 5.3-1 9.2-5.6 9.2-10.8V5.7L12 2Zm0 17.9c-3.7-.9-6.2-4.2-6.2-8.7V7.7L12 5.2l6.2 2.5v3.5c0 4.5-2.5 7.8-6.2 8.7Z" /><path d="m8.6 11.8 2.1 2.1 4.7-4.8" /></Icon>
            <span><b>¡Importante!</b> Registrarte y completar los tres pasos es condición para participar.</span>
          </div>
        </div>

        <aside className="prelanding-prize" aria-label="Premio del sorteo">
          <div className="prelanding-prize-orbit orbit-one" />
          <div className="prelanding-prize-orbit orbit-two" />
          <p>Premio del sorteo</p>
          <img src="/silla-sorteo.png" alt="Silla gamer ergonómica negra" />
          <div className="prelanding-prize-name">Silla gamer<br /><span>ergonómica</span></div>
          <div className="prelanding-prize-tag">Con apoyacabeza · soporte lumbar · apoyapiés</div>
        </aside>
      </section>

      <footer className="prelanding-footer">© {new Date().getFullYear()} FuturaTecno · Tu tecnología. Tu futuro.</footer>
    </main>
  )
}

export default PreLandingPage
