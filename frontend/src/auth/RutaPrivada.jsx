import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/**
 * Envuelve rutas que solo requieren estar logueado (historial de pedidos), a diferencia de
 * ProtectedRoute, que además exige rol ADMIN. Guarda de dónde venía para volver ahí post-login.
 */
function RutaPrivada({ children }) {
  const { isAuth, listo } = useAuth()
  const location = useLocation()

  if (!listo) return null // esperando a recuperar la sesión

  if (!isAuth) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  return children
}

export default RutaPrivada
