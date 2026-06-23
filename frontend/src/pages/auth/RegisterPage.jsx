import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'

function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres.')
      return
    }
    setCargando(true)
    try {
      await register(email, password, nombre)
      navigate('/') // queda logueado, va al catálogo
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo registrar.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div style={{ maxWidth: '400px', margin: '60px auto', padding: '0 20px' }}>
      <div className="card">
        <h1 style={{ fontSize: '24px', marginBottom: '4px' }}>Crear cuenta</h1>
        <p style={{ color: '#888', fontSize: '13px', marginBottom: '20px' }}>
          Registrate para recibir novedades y ofertas de FuturaTecno.
        </p>

        {error && (
          <div style={{ background: '#f8d7da', color: '#721c24', padding: '10px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Nombre (opcional)</label>
            <input type="text" value={nombre} onChange={e => setNombre(e.target.value)} autoComplete="name" />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="form-group">
            <label>Contraseña (mín. 6 caracteres)</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required autoComplete="new-password" />
          </div>
          <button type="submit" className="btn btn-primary" disabled={cargando} style={{ width: '100%' }}>
            {cargando ? 'Creando...' : 'Crear cuenta'}
          </button>
        </form>

        <p style={{ fontSize: '14px', marginTop: '16px', textAlign: 'center' }}>
          ¿Ya tenés cuenta? <Link to="/login" style={{ color: '#007bff' }}>Iniciá sesión</Link>
        </p>
        <p style={{ fontSize: '13px', marginTop: '8px', textAlign: 'center' }}>
          <Link to="/" style={{ color: '#888' }}>← Volver al catálogo</Link>
        </p>
      </div>
    </div>
  )
}

export default RegisterPage
