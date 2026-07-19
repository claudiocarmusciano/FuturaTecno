import { useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setCargando(true)
    try {
      const res = await axios.post('/api/auth/forgot-password', { email })
      setEnviado(res.data?.mensaje || 'Si el email está registrado, te enviamos un enlace para restablecer la contraseña.')
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo procesar la solicitud. Intentá de nuevo.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div style={{ maxWidth: '400px', margin: '60px auto', padding: '0 20px' }}>
      <div className="card">
        <div style={{ textAlign: 'center', marginBottom: '26px' }}>
          <span style={{ display: 'inline-block', background: '#16181d', borderRadius: '16px', padding: '11px 16px' }}>
            <img src="/logo.png?v=2" alt="FuturaTecno" style={{ height: '66px', width: 'auto', display: 'block' }} />
          </span>
        </div>
        <h1 style={{ fontSize: '22px', marginBottom: '4px', textAlign: 'center' }}>Recuperar contraseña</h1>
        <p style={{ color: 'var(--color-text-muted)', fontSize: '13px', marginBottom: '24px', textAlign: 'center' }}>
          Ingresá tu email y te mandamos un enlace para restablecerla.
        </p>

        {enviado ? (
          <div style={{ background: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '14px', borderRadius: '10px', fontSize: '14px', textAlign: 'center' }}>
            📧 {enviado}
            <p style={{ fontSize: '12px', color: '#4b5563', marginTop: '10px' }}>
              Revisá tu casilla (y la carpeta de spam). El enlace vence en 1 hora.
            </p>
          </div>
        ) : (
          <>
            {error && (
              <div style={{ background: '#fff1f0', color: '#d70015', border: '1px solid #ffd9d6', padding: '11px 14px', borderRadius: '10px', marginBottom: '16px', fontSize: '14px' }}>
                {error}
              </div>
            )}
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} required autoComplete="email" />
              </div>
              <button type="submit" className="btn btn-primary" disabled={cargando} style={{ width: '100%' }}>
                {cargando ? 'Enviando...' : 'Enviar enlace'}
              </button>
            </form>
          </>
        )}

        <p style={{ fontSize: '14px', marginTop: '16px', textAlign: 'center' }}>
          <Link to="/login" style={{ color: 'var(--color-accent)', fontWeight: 600 }}>← Volver a iniciar sesión</Link>
        </p>
      </div>
    </div>
  )
}

export default ForgotPasswordPage
