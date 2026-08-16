import { Link } from 'react-router-dom'
import './BasesSorteoPage.css'

function BasesSorteoPage() {
  return (
    <main className="bases-page">
      <header className="bases-header">
        <Link to="/"><img src="/logo.png?v=2" alt="FuturaTecno" /></Link>
        <Link to="/" className="bases-back">← Volver al sorteo</Link>
      </header>
      <article className="bases-card">
        <span className="bases-eyebrow">SORTEO BIENVENIDA</span>
        <h1>Bases y Condiciones</h1>
        <p className="bases-intro">Participación gratuita y sin obligación de compra.</p>

        <section><h2>1. Organizador</h2><p>El sorteo es organizado por Futura Tecno, nombre comercial de Claudio José Carmusciano, CUIT 20-23128286-7, con domicilio en calle San Martín 2821 de la ciudad de Olavarría, provincia de Buenos Aires.</p></section>
        <section><h2>2. Vigencia</h2><p>La inscripción estará abierta desde el 18 de septiembre de 2026 hasta el 30 de septiembre de 2026 a las 23:59 h, hora de Argentina.</p></section>
        <section><h2>3. Participación</h2><p>Podrán participar personas de 15 años o más, residentes en Argentina, que completen el registro en futuratecno.com.ar y cumplan los requisitos indicados durante la inscripción.</p><p>Las personas menores de 18 años deberán contar con autorización de su madre, padre o tutor legal para recibir el premio.</p><p>La participación es gratuita y sin obligación de compra. Se admitirá una única participación por persona, DNI, correo electrónico y número de celular.</p></section>
        <section><h2>4. Requisitos</h2><p>Para resultar elegible, la persona deberá:</p><ol type="a"><li>completar y confirmar su registro por email;</li><li>enviar el mensaje requerido al WhatsApp oficial de Futura Tecno, +54 9 2284 381111, y tener dicho número agendado como “Futura Tecno”;</li><li>seguir la cuenta oficial @futuratecnoargentina y etiquetar a tres amigos en la publicación oficial del sorteo;</li><li>aceptar estas Bases y Condiciones.</li></ol><p>El Organizador podrá solicitar información razonable para verificar el cumplimiento de los requisitos.</p></section>
        <section><h2>5. Premio</h2><p>Se sorteará una (1) <strong>Silla Gamer Raptor Throne S10 Negra Negro Tela</strong>, conforme a la imagen publicada por Futura Tecno. El premio no es transferible ni canjeable por dinero.</p><p>El envío del premio a cualquier punto de Argentina estará a cargo de Futura Tecno.</p></section>
        <section><h2>6. Padrón y códigos</h2><p>Cada participante elegible recibirá un código único de sorteo. El 01/10/2026 se publicará el padrón anonimizado de códigos válidos, la cantidad total de participantes y el hash de integridad del archivo.</p></section>
        <section><h2>7. Sorteo</h2><p>El sorteo se realizará el 02/10/2026 mediante extracción manual y aleatoria de papeles idénticos con los códigos de sorteo, en un recipiente transparente o visible. Se extraerá un ganador y tres suplentes.</p></section>
        <section><h2>8. Comunicación y entrega</h2><p>El ganador será contactado por email y WhatsApp. Deberá responder dentro de siete (7) días corridos y acreditar su identidad mediante DNI. Si no cumple, se convocará al primer suplente, y así sucesivamente.</p></section>
        <section><h2>9. Datos personales</h2><p>Los datos serán utilizados para gestionar la participación, verificar requisitos, contactar al ganador y entregar el premio. No se publicarán DNI, email, teléfono ni fecha de nacimiento.</p></section>
        <section><h2>10. Instagram</h2><p>Esta promoción no está patrocinada, avalada, administrada ni asociada con Instagram ni Meta.</p></section>
        <section><h2>11. Aceptación</h2><p>La participación implica la aceptación total de estas Bases y Condiciones.</p></section>
      </article>
      <footer className="bases-footer">© {new Date().getFullYear()} Futura Tecno · Tu tecnología. Tu futuro.</footer>
    </main>
  )
}

export default BasesSorteoPage
