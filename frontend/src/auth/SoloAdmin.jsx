import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

/**
 * Envuelve rutas públicas que quedaron cerradas mientras dura la pre-landing del
 * sorteo: el catálogo y la ficha de producto. Solo las ve el ADMIN; el visitante
 * cae en la pre-landing.
 *
 * A diferencia de ProtectedRoute, NO manda al login: al visitante no se le pide
 * cuenta, se lo devuelve al embudo del sorteo (esa era la conducta previa, ver
 * b17bf7c). Mandarlo a un formulario de login sería pedirle algo que no puede
 * resolver — no hay cuenta que le dé acceso, el catálogo es solo del admin.
 *
 * OJO: esto oculta el catálogo en la interfaz, no cierra la API. `GET /api/productos`
 * sigue siendo público (SecurityConfig), así que los datos son accesibles para
 * quien sepa pedirlos. Es una cortina, no una cerradura.
 */
function SoloAdmin({ children }) {
  const { isAdmin, listo } = useAuth()

  if (!listo) return null // esperando a recuperar la sesión

  return isAdmin ? children : <Navigate to="/" replace />
}

export default SoloAdmin
