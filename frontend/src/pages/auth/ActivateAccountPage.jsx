import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../auth/AuthContext'

function ActivateAccountPage() {
  const { guardarSesion } = useAuth()
  const [params] = useSearchParams()
  const [estado, setEstado] = useState('activando')
  const token = params.get('token')
  useEffect(() => {
    if (!token) { setEstado('invalido'); return }
    axios.get(`/api/auth/activar-cuenta?token=${encodeURIComponent(token)}`)
      .then(res => { guardarSesion(res.data); setEstado('ok') }).catch(() => setEstado('invalido'))
  }, [token, guardarSesion])
  return <div style={{ maxWidth: 480, margin: '80px auto', padding: '0 20px', textAlign: 'center' }} className="card">
    <img src="/logo.png?v=2" alt="FuturaTecno" style={{ height: 64, background: '#16181d', borderRadius: 12, padding: 8, marginBottom: 20 }} />
    {estado === 'activando' && <><h1 style={{ fontSize: 25 }}>Activando tu cuenta…</h1><p>Un momento, estamos verificando el enlace.</p></>}
    {estado === 'ok' && <><h1 style={{ fontSize: 25 }}>¡Email activado!</h1><p style={{ margin: '10px 0 22px', color: 'var(--color-text-muted)' }}>Volvé a los pasos del sorteo para continuar.</p><Link className="btn btn-primary" to="/">Volver al sorteo</Link></>}
    {estado === 'invalido' && <><h1 style={{ fontSize: 25 }}>El enlace no es válido</h1><p style={{ margin: '10px 0 22px', color: 'var(--color-text-muted)' }}>Puede haber vencido. Volvé a registrarte para recibir uno nuevo.</p><Link className="btn btn-primary" to="/registro">Ir al registro</Link></>}
  </div>
}
export default ActivateAccountPage
