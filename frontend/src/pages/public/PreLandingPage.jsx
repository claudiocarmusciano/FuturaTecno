import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'
import './PreLanding.css'

const whatsappUrl = 'https://wa.me/5492284381111?text=' + encodeURIComponent('¡Hola FuturaTecno! Ya quiero participar del sorteo de la silla gamer.')
const instagramUrl = 'https://www.instagram.com/futuratecnoargentina'

function PreLandingPage() {
  const { isAuth } = useAuth()
  const [estado, setEstado] = useState(null)
  const [codigo, setCodigo] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [guardando, setGuardando] = useState(false)
  const actualizar = () => isAuth && axios.get('/api/auth/onboarding').then(r => setEstado(r.data)).catch(() => {})
  useEffect(() => { actualizar() }, [isAuth])
  const verificar = async () => {
    setMensaje(''); setGuardando(true)
    try { const r = await axios.post('/api/auth/onboarding/verificar-whatsapp', { codigo }); setEstado(r.data); setCodigo('') }
    catch (e) { setMensaje(e.response?.data?.error || 'No se pudo verificar el código.') } finally { setGuardando(false) }
  }
  const completar = async paso => {
    setMensaje(''); setGuardando(true)
    try { const r = await axios.post(`/api/auth/onboarding/paso/${paso}`); setEstado(r.data) }
    catch (e) { setMensaje(e.response?.data?.error || 'No se pudo guardar el paso.') } finally { setGuardando(false) }
  }
  const pasoUno = estado?.pasoUnoCompleto
  const pasoDos = estado?.whatsappAgendado
  const pasoTres = estado?.instagramCompletado
  return <main className="prelanding">
    <div className="prelanding-glow prelanding-glow-one" /><div className="prelanding-glow prelanding-glow-two" />
    <header className="prelanding-header"><Link to="/inicio"><img src="/logo.png?v=2" alt="FuturaTecno" /></Link><Link className="prelanding-skip" to="/inicio">Ya tengo cuenta · Ver catálogo →</Link></header>
    <section className="prelanding-content"><div className="prelanding-copy">
      <span className="prelanding-badge"><span /> Sorteo especial de bienvenida</span>
      <h1>Tu próximo upgrade puede ser una <strong>silla gamer ergonómica.</strong></h1>
      <p className="prelanding-lead">Si es tu primera vez en FuturaTecno, registrate y completá estos pasos para participar del sorteo.</p>
      <ol className="prelanding-steps">
        <li className={pasoUno ? 'completo' : ''}><span className="prelanding-step-number">1</span><div><b>{pasoUno ? 'WhatsApp y email verificados.' : 'Registrate con tu número de WhatsApp.'}</b><small>{pasoUno ? 'Tu registro ya está confirmado.' : 'Te enviaremos un código por WhatsApp y un botón de activación por email.'}</small>
          {isAuth && !pasoUno && <><div className="prelanding-verify"><input value={codigo} onChange={e => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" placeholder="Código de 6 dígitos" /><button className="prelanding-action primary" disabled={guardando || codigo.length !== 6} onClick={verificar}>Ya lo hice</button></div>{!estado?.emailVerificado && <small className="prelanding-email-note">Revisá tu email y hacé clic en <b>Activar cuenta</b>. Después volvé acá.</small>}</>}</div>
          {!isAuth ? <Link className="prelanding-action primary" to="/registro">Registrarme →</Link> : pasoUno ? <span className="prelanding-done">✓ Listo</span> : null}</li>
        <li className={pasoDos ? 'completo' : (!pasoUno ? 'bloqueado' : '')}><span className="prelanding-step-number">2</span><div><b>{pasoDos ? 'WhatsApp agendado.' : 'Abrí WhatsApp y agendanos.'}</b><small>Guardá el número de FuturaTecno para recibir novedades del sorteo.</small></div>
          {pasoDos ? <span className="prelanding-done">✓ Listo</span> : <div className="prelanding-actions"><a className="prelanding-action whatsapp" href={pasoUno ? whatsappUrl : undefined} target="_blank" rel="noreferrer" onClick={e => !pasoUno && e.preventDefault()}>Abrir WhatsApp</a><button className="prelanding-action outline" disabled={!pasoUno || guardando} onClick={() => completar(2)}>Ya lo hice</button></div>}</li>
        <li className={pasoTres ? 'completo' : (!pasoDos ? 'bloqueado' : '')}><span className="prelanding-step-number">3</span><div><b>{pasoTres ? 'Instagram completado.' : 'Seguinos y etiquetá a 3 amigos en Instagram.'}</b><small>Hacelo en el posteo del sorteo para dejar registrada tu participación.</small></div>
          {pasoTres ? <span className="prelanding-done">✓ Listo</span> : <div className="prelanding-actions"><a className="prelanding-action instagram" href={pasoDos ? instagramUrl : undefined} target="_blank" rel="noreferrer" onClick={e => !pasoDos && e.preventDefault()}>Ir a Instagram</a><button className="prelanding-action outline" disabled={!pasoDos || guardando} onClick={() => completar(3)}>Ya lo hice</button></div>}</li>
      </ol>
      {mensaje && <p className="prelanding-message">{mensaje}</p>}
      {pasoTres && <Link className="prelanding-finish" to="/inicio">¡Listo! Ir a la landing principal →</Link>}
      <div className="prelanding-reminder"><span>✓</span><span><b>¡Importante!</b> Registrarte y completar los tres pasos es condición para participar.</span></div>
    </div><aside className="prelanding-prize" aria-label="Sorteo Bienvenida"><div className="prelanding-prize-orbit orbit-one" /><div className="prelanding-prize-orbit orbit-two" /><p>Sorteo Bienvenida</p><img src="/silla-sorteo.png" alt="Silla gamer ergonómica negra" /><div className="prelanding-prize-name">Silla gamer<br /><span>ergonómica</span></div><div className="prelanding-prize-tag">Con apoyacabeza · soporte lumbar · apoyapiés</div></aside></section>
    <footer className="prelanding-footer">© {new Date().getFullYear()} FuturaTecno · Tu tecnología. Tu futuro.</footer>
  </main>
}
export default PreLandingPage
