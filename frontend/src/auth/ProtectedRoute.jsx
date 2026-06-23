import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Envuelve rutas que requieren ADMIN. Si no hay sesión admin, redirige al login. */
function ProtectedRoute({ children }) {
  const { isAdmin, listo } = useAuth()
  const location = useLocation()

  if (!listo) return null // esperando a recuperar la sesión

  if (!isAdmin) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  return children
}

export default ProtectedRoute
