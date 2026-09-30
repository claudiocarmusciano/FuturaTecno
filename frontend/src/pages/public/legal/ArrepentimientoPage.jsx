import { useState } from 'react'
import axios from 'axios'
import LegalLayout from './LegalLayout'
import { useAuth } from '../../../auth/AuthContext'

/**
 * Botón de arrepentimiento (Res. SCI 424/2020): sin registro ni login, con código de trámite
 * al instante. El admin recibe el aviso por mail para responder dentro de las 24 h.
 */
export default function ArrepentimientoPage() {
  const { user } = useAuth()
  const [form, setForm] = useState({
    nombre: user?.nombre || '', email: user?.email || '', telefono: '', numeroPedido: '', detalle: '', sitio: ''
  })
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [codigo, setCodigo] = useState(null)

  const cambiar = (campo) => (e) => setForm(f => ({ ...f, [campo]: e.target.value }))

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const { data } = await axios.post('/api/arrepentimiento', form)
      setCodigo(data.codigo)
    } catch (err) {
      setError(err.response?.data?.error || 'No pudimos registrar la solicitud. Probá de nuevo o escribinos por WhatsApp.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <LegalLayout eyebrow="DERECHO DEL CONSUMIDOR" titulo="Botón de arrepentimiento"
      intro="Podés revocar tu compra dentro de los 10 días corridos desde que recibiste el producto, sin dar explicaciones.">
      <section>
        <p>Completá este formulario y te damos un <strong>código de trámite</strong> en el momento. Te contactamos dentro de las 24 horas para coordinar la devolución y el reintegro.</p>
        <p>El producto tiene que estar sin uso, completo y en su embalaje original. Los costos de la devolución corren por nuestra cuenta y te reintegramos el total pagado.</p>
      </section>

      {codigo ? (
        <div className="arr-ok" role="status">
          Recibimos tu solicitud. Tu código de trámite es
          <strong className="codigo">{codigo}</strong>
          Te lo mandamos también por mail. Guardalo para cualquier consulta.
        </div>
      ) : (
        <form className="arr-form" onSubmit={enviar}>
          <label>Nombre y apellido
            <input value={form.nombre} onChange={cambiar('nombre')} required maxLength={120} autoComplete="name" />
          </label>
          <label>Email
            <input type="email" value={form.email} onChange={cambiar('email')} required maxLength={190} autoComplete="email" />
          </label>
          <label>Teléfono <span className="arr-opcional">(opcional)</span>
            <input value={form.telefono} onChange={cambiar('telefono')} maxLength={40} autoComplete="tel" />
          </label>
          <label>Número de pedido <span className="arr-opcional">(si lo tenés)</span>
            <input value={form.numeroPedido} onChange={cambiar('numeroPedido')} maxLength={40} placeholder="Ej.: FT-000123" />
          </label>
          <label>Qué producto querés devolver <span className="arr-opcional">(opcional)</span>
            <textarea rows={3} value={form.detalle} onChange={cambiar('detalle')} maxLength={1000} />
          </label>
          {/* Campo trampa: invisible para las personas; los bots lo completan. */}
          <label className="arr-trampa" aria-hidden="true">Sitio web
            <input tabIndex={-1} autoComplete="off" value={form.sitio} onChange={cambiar('sitio')} />
          </label>
          {error && <p className="arr-error" role="alert">{error}</p>}
          <button type="submit" disabled={enviando}>{enviando ? 'Enviando…' : 'Solicitar la revocación'}</button>
        </form>
      )}
    </LegalLayout>
  )
}
