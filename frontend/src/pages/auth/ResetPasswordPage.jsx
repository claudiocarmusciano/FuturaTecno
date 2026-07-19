import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import PasswordInput from '../../components/PasswordInput'

function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''

  const [password, setPassword] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState(false)
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres.')
      return
    }
    if (password !== confirmar) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setCargando(true)
    try {
      await axios.post('/api/auth/reset-password', { token, password })
      setOk(true)
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo restablecer la contraseña.')
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
        <h1 style={{ fontSize: '22px', marginBottom: '4px', textAlign: 'center' }}>Nueva contraseña</h1>

        {ok ? (
          <div style={{ background: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '14px', borderRadius: '10px', fontSize: '14px', textAlign: 'center' }}>
            ✅ Tu contraseña se actualizó.
            <p style={{ marginTop: '12px' }}>
              <Link to="/login" style={{ color: 'var(--color-accent)', fontWeight: 600 }}>Iniciá sesión</Link>
            </p>
          </div>
        ) : !token ? (
          <div style={{ background: '#fff1f0', color: '#d70015', border: '1px solid #ffd9d6', padding: '14px', borderRadius: '10px', fontSize: '14px', textAlign: 'center' }}>
            Enlace inválido: falta el token. Volvé a pedir el reseteo.
            <p style={{ marginTop: '12px' }}>
              <Link to="/recuperar" style={{ color: 'var(--color-accent)', fontWeight: 600 }}>Pedir un nuevo enlace</Link>
            </p>
          </div>
        ) : (
          <>
            <p style={{ color: 'var(--color-text-muted)', fontSize: '13px', marginBottom: '24px', textAlign: 'center' }}>
              Elegí una contraseña nueva para tu cuenta.
            </p>
            {error && (
              <div style={{ background: '#fff1f0', color: '#d70015', border: '1px solid #ffd9d6', padding: '11px 14px', borderRadius: '10px', marginBottom: '16px', fontSize: '14px' }}>
                {error}
              </div>
            )}
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Nueva contraseña (mín. 6 caracteres)</label>
                <PasswordInput value={password} onChange={e => setPassword(e.target.value)} autoComplete="new-password" />
              </div>
              <div className="form-group">
                <label>Repetir contraseña</label>
                <PasswordInput value={confirmar} onChange={e => setConfirmar(e.target.value)} autoComplete="new-password" />
              </div>
              <button type="submit" className="btn btn-primary" disabled={cargando} style={{ width: '100%' }}>
                {cargando ? 'Guardando...' : 'Guardar contraseña'}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  )
}

export default ResetPasswordPage
