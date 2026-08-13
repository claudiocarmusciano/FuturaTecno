import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'
import './PreLanding.css'

const instagramUrl = 'https://www.instagram.com/futuratecnoargentina'

function PreLandingPage() {
  const { isAuth, logout } = useAuth()
  const [estado, setEstado] = useState(null)
  const [estadoCargado, setEstadoCargado] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [modalCerrado, setModalCerrado] = useState(false)
  const actualizar = () => {
    if (!isAuth) {
      setEstado(null)
      setEstadoCargado(true)
      return
    }
    setEstadoCargado(false)
    axios.get('/api/auth/onboarding')
      .then(r => setEstado(r.data))
      .catch(e => {
        setEstado(null)
        if ([401, 403].includes(e.response?.status)) logout()
      })
      .finally(() => setEstadoCargado(true))
  }
  useEffect(() => { actualizar() }, [isAuth])
  const completar = async paso => {
    setMensaje(''); setGuardando(true)
    try { const r = await axios.post(`/api/auth/onboarding/paso/${paso}`); setEstado(r.data); if (paso === 3) setModalCerrado(false) }
    catch (e) { setMensaje(e.response?.data?.error || 'No se pudo guardar el paso.') } finally { setGuardando(false) }
  }
  // La API expone el estado base como `emailVerificado`; los métodos calculados del DTO no viajan en JSON.
  const pasoUno = Boolean(estado?.emailVerificado)
  const pasoDos = estado?.whatsappAgendado
  const pasoTres = estado?.instagramCompletado
  const necesitaRegistro = estadoCargado && (!isAuth || !estado)
  const whatsappUrl = estado?.whatsappVerificacionCodigo
    ? 'https://wa.me/5492284381111?text=' + encodeURIComponent(`Hola FuturaTecno, verifico mi registro para el sorteo: ${estado.whatsappVerificacionCodigo}`)
    : undefined
  return <main className="prelanding">
    <div className="prelanding-glow prelanding-glow-one" /><div className="prelanding-glow prelanding-glow-two" />
    <header className="prelanding-header"><Link to="/"><img src="/logo.png?v=2" alt="FuturaTecno" /></Link>{!isAuth && <Link className="prelanding-skip" to="/login">Ya tengo cuenta →</Link>}</header>
    <section className="prelanding-content"><div className="prelanding-copy">
      <span className="prelanding-badge"><span /> Sorteo especial de bienvenida</span>
      <h1>Tu próximo upgrade puede ser una <strong>silla gamer ergonómica.</strong></h1>
      <p className="prelanding-lead">Si es tu primera vez en FuturaTecno, registrate y completá estos pasos para participar del sorteo.</p>
      <ol className="prelanding-steps">
        <li className={pasoUno ? 'completo' : ''}><span className="prelanding-step-number">1</span><div><b>{pasoUno ? 'Email activado.' : 'Registrate con tu número de WhatsApp.'}</b><small>{pasoUno ? 'Tu cuenta ya está confirmada.' : 'Te enviaremos un botón de activación por email.'}</small>
          {isAuth && estado && !pasoUno && <small className="prelanding-email-note">Revisá tu email y hacé clic en <b>Activar cuenta</b>. Después volvé acá.</small>}</div>
          {necesitaRegistro ? <Link className="prelanding-action primary" to="/registro">Comenzar registro →</Link> : pasoUno ? <span className="prelanding-done">✓ Listo</span> : null}</li>
        <li className={estado?.whatsappVerificado ? 'completo' : (!pasoUno ? 'bloqueado' : '')}><span className="prelanding-step-number">2</span><div><b>{estado?.whatsappVerificado ? 'WhatsApp validado.' : pasoDos ? 'Estamos validando tu WhatsApp.' : 'Agendanos y mandanos un mensaje.'}</b><small>{estado?.whatsappVerificado ? 'Confirmamos que el número ingresado es tuyo.' : pasoDos ? 'Buscaremos tu código en WhatsApp y confirmaremos el paso.' : 'Guardá nuestro número y enviá el mensaje precompletado para validar tu celular.'}</small></div>
          {estado?.whatsappVerificado ? <span className="prelanding-done">✓ Listo</span> : <div className="prelanding-actions"><a className={`prelanding-action whatsapp${pasoDos ? ' disabled' : ''}`} href={pasoUno && !pasoDos ? whatsappUrl : undefined} target="_blank" rel="noreferrer" onClick={e => (!pasoUno || pasoDos) && e.preventDefault()}>Abrir WhatsApp</a><button className="prelanding-action outline" disabled={!pasoUno || pasoDos || guardando} onClick={() => completar(2)}>Ya lo hice</button></div>}</li>
        <li className={pasoTres ? 'completo' : (!pasoDos ? 'bloqueado' : '')}><span className="prelanding-step-number">3</span><div><b>{pasoTres ? 'Instagram completado.' : 'Seguinos y etiquetá a 3 amigos en Instagram.'}</b><small>Hacelo en el posteo del sorteo para dejar registrada tu participación.</small></div>
          {pasoTres ? <span className="prelanding-done">✓ Listo</span> : <div className="prelanding-actions"><a className="prelanding-action instagram" href={pasoDos ? instagramUrl : undefined} target="_blank" rel="noreferrer" onClick={e => !pasoDos && e.preventDefault()}>Ir a Instagram</a><button className="prelanding-action outline" disabled={!pasoDos || guardando} onClick={() => completar(3)}>Ya lo hice</button></div>}</li>
      </ol>
      {mensaje && <p className="prelanding-message">{mensaje}</p>}
      {pasoTres && !modalCerrado && <div className="prelanding-modal-backdrop" role="presentation"><section className="prelanding-modal" role="dialog" aria-modal="true" aria-labelledby="prelanding-modal-title"><span className="prelanding-modal-check">✓</span><span className="prelanding-modal-label">PREINSCRIPCIÓN CONFIRMADA</span><h2 id="prelanding-modal-title">¡Ya estás participando!</h2><p>Próximamente tendrás acceso al gran catálogo tecnológico y con precios increíbles.</p><p><strong>Ah!</strong> Y también tendrás regalos para cada cumpleaños tuyo.</p><button className="prelanding-modal-close" onClick={() => setModalCerrado(true)}>Entendido</button><span className="prelanding-modal-note">Gracias por sumarte a FuturaTecno.</span></section></div>}
      <div className="prelanding-reminder"><span>✓</span><span><b>¡Importante!</b> Registrarte y completar los tres pasos es condición para participar.</span></div>
    </div><aside className="prelanding-prize" aria-label="Sorteo Bienvenida"><div className="prelanding-prize-orbit orbit-one" /><div className="prelanding-prize-orbit orbit-two" /><p>Sorteo Bienvenida</p><img src="/silla-sorteo.png" alt="Silla gamer ergonómica negra" /><div className="prelanding-prize-name">Silla gamer<br /><span>ergonómica</span></div><div className="prelanding-prize-tag">Con apoyacabeza · soporte lumbar · apoyapiés</div></aside></section>
    <footer className="prelanding-footer">© {new Date().getFullYear()} FuturaTecno · Tu tecnología. Tu futuro.</footer>
  </main>
}
export default PreLandingPage
