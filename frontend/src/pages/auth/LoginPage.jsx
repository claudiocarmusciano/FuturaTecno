import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setCargando(true)
    try {
      const data = await login(email, password)
      // Admin va al panel; usuario común al catálogo.
      navigate(data.rol === 'ADMIN' ? (location.state?.from || '/admin') : '/')
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo iniciar sesión.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div style={{ maxWidth: '400px', margin: '60px auto', padding: '0 20px' }}>
      <div className="card">
        <h1 style={{ fontSize: '24px', marginBottom: '4px' }}>Iniciar sesión</h1>
        <p style={{ color: '#888', fontSize: '13px', marginBottom: '20px' }}>FuturaTecno</p>

        {error && (
          <div style={{ background: '#f8d7da', color: '#721c24', padding: '10px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="form-group">
            <label>Contraseña</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required autoComplete="current-password" />
          </div>
          <button type="submit" className="btn btn-primary" disabled={cargando} style={{ width: '100%' }}>
            {cargando ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        <p style={{ fontSize: '14px', marginTop: '16px', textAlign: 'center' }}>
          ¿No tenés cuenta? <Link to="/registro" style={{ color: '#007bff' }}>Registrate</Link>
        </p>
        <p style={{ fontSize: '13px', marginTop: '8px', textAlign: 'center' }}>
          <Link to="/" style={{ color: '#888' }}>← Volver al catálogo</Link>
        </p>
      </div>
    </div>
  )
}

export default LoginPage
