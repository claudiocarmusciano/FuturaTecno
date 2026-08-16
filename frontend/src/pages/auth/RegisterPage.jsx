import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import PasswordInput from '../../components/PasswordInput'

const celularArgentinoValido = (valor) => {
  let digitos = valor.replace(/\D/g, '')
  if (digitos.startsWith('549')) digitos = digitos.slice(3)
  else if (digitos.startsWith('54')) {
    digitos = digitos.slice(2)
    if (digitos.startsWith('9')) digitos = digitos.slice(1)
  } else if (digitos.startsWith('0')) digitos = digitos.slice(1)
  return /^[1-9]\d{9}$/.test(digitos)
}

function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // Si llegó desde una pantalla que exige sesión (ej. el checkout), vuelve ahí al registrarse.
  const destino = location.state?.from || '/'
  const [nombre, setNombre] = useState('')
  const [apellido, setApellido] = useState('')
  const [celular, setCelular] = useState('')
  const [dni, setDni] = useState('')
  const [fechaNacimiento, setFechaNacimiento] = useState('')
  const [instagramUsuario, setInstagramUsuario] = useState('')
  const [aceptaBases, setAceptaBases] = useState(false)
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
    if (!celularArgentinoValido(celular)) {
      setError('Ingresá un celular argentino válido, con código de área y sin 0 ni 15.')
      return
    }
    if (!/^\d{7,8}$/.test(dni)) {
      setError('Ingresá un DNI argentino válido, sin puntos.')
      return
    }
    if (!fechaNacimiento || new Date(`${fechaNacimiento}T00:00:00`) > new Date()) {
      setError('Ingresá una fecha de nacimiento válida.')
      return
    }
    if (new Date(`${fechaNacimiento}T00:00:00`).setFullYear(new Date(`${fechaNacimiento}T00:00:00`).getFullYear() + 15) > new Date()) {
      setError('Para participar del sorteo debés tener al menos 15 años.')
      return
    }
    if (!/^@?[A-Za-z0-9._]{1,30}$/.test(instagramUsuario.trim())) {
      setError('Ingresá tu usuario de Instagram, sin enlaces.')
      return
    }
    if (!aceptaBases) {
      setError('Debés aceptar las Bases y Condiciones para participar.')
      return
    }
    setCargando(true)
    try {
      await register(email, password, nombre, apellido, celular, dni, fechaNacimiento, instagramUsuario, aceptaBases)
      navigate(destino) // queda logueado
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo registrar.')
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
        <h1 style={{ fontSize: '22px', marginBottom: '4px', textAlign: 'center' }}>Preinscripción al sorteo</h1>
        <p style={{ color: 'var(--color-text-muted)', fontSize: '13px', marginBottom: '24px', textAlign: 'center' }}>
          Dejanos tus datos, activá tu email y completá los pasos para participar del sorteo.
        </p>

        {error && (
          <div style={{ background: 'var(--color-danger-bg)', color: 'var(--color-danger)', border: '1px solid rgba(255,107,94,0.3)', padding: '11px 14px', borderRadius: '10px', marginBottom: '16px', fontSize: '14px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '12px' }}>
            <div className="form-group">
              <label>Nombre</label>
              <input type="text" value={nombre} onChange={e => setNombre(e.target.value)} autoComplete="given-name" required />
            </div>
            <div className="form-group">
              <label>Apellido</label>
              <input type="text" value={apellido} onChange={e => setApellido(e.target.value)} autoComplete="family-name" required />
            </div>
          </div>
          <div className="form-group">
            <label>Celular</label>
            <input type="tel" value={celular} onChange={e => setCelular(e.target.value)} autoComplete="tel" inputMode="tel" placeholder="11 1234-5678" required />
            <small style={{ display: 'block', color: 'var(--color-text-muted)', marginTop: '6px', lineHeight: 1.45 }}>
              Ingresalo con código de área, sin 0 ni 15. Ej.: 11 1234-5678.
            </small>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '12px' }}>
            <div className="form-group">
              <label>DNI</label>
              <input type="text" value={dni} onChange={e => setDni(e.target.value.replace(/\D/g, '').slice(0, 8))} inputMode="numeric" autoComplete="off" placeholder="Sin puntos" required />
            </div>
            <div className="form-group">
              <label>Fecha de nacimiento</label>
              <input type="date" value={fechaNacimiento} onChange={e => setFechaNacimiento(e.target.value)} max={new Date().toISOString().slice(0, 10)} autoComplete="bday" required />
            </div>
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div className="form-group">
            <label>Usuario de Instagram</label>
            <input type="text" value={instagramUsuario} onChange={e => setInstagramUsuario(e.target.value)} autoComplete="off" placeholder="@tu_usuario" required />
            <small style={{ display: 'block', color: 'var(--color-text-muted)', marginTop: '6px', lineHeight: 1.45 }}>Lo usaremos únicamente para verificar el requisito del sorteo.</small>
          </div>
          <div className="form-group">
            <label>Contraseña (mín. 6 caracteres)</label>
            <PasswordInput value={password} onChange={e => setPassword(e.target.value)} autoComplete="new-password" />
          </div>
          <label style={{ display: 'flex', alignItems: 'flex-start', gap: '9px', margin: '18px 0', color: 'var(--color-text-muted)', fontSize: '12.5px', lineHeight: 1.45, cursor: 'pointer' }}>
            <input type="checkbox" checked={aceptaBases} onChange={e => setAceptaBases(e.target.checked)} required style={{ marginTop: '3px' }} />
            <span>Leí y acepto las <Link to="/bases-y-condiciones" target="_blank" rel="noreferrer" style={{ color: 'var(--color-accent)', fontWeight: 700 }}>Bases y Condiciones del Sorteo Bienvenida</Link>.</span>
          </label>
          <button type="submit" className="btn btn-primary" disabled={cargando} style={{ width: '100%' }}>
            {cargando ? 'Registrando...' : 'Completar preinscripción'}
          </button>
        </form>

        <div style={{ marginTop: '16px', padding: '12px 14px', borderRadius: '10px', background: 'var(--color-bg-alt)', color: 'var(--color-text-muted)', fontSize: '12.5px', lineHeight: 1.5 }}>
          Tu celular se utiliza para recibir notificaciones, ofertas, promociones y regalos. Es requisito para participar de los sorteos; podés darte de baja del grupo de difusión cuando quieras.<br /><br />
          Para participar del sorteo necesitás seguirnos en Instagram, etiquetar a tres amigos en la publicación oficial y completar la validación. <strong style={{ color: 'var(--color-text)' }}>Sin obligación de compra.</strong>
        </div>

        <p style={{ fontSize: '14px', marginTop: '16px', textAlign: 'center' }}>
          ¿Ya tenés cuenta? <Link to="/login" style={{ color: 'var(--color-accent)', fontWeight: 600 }}>Iniciá sesión</Link>
        </p>
        <p style={{ fontSize: '13px', marginTop: '8px', textAlign: 'center' }}>
          <Link to="/" style={{ color: 'var(--color-text-muted)' }}>← Volver al sorteo</Link>
        </p>
      </div>
    </div>
  )
}

export default RegisterPage
