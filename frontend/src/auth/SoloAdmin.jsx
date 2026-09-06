import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

/**
 * Envuelve rutas públicas que quedaron cerradas mientras dura la pre-landing del
 * sorteo: el catálogo y la ficha de producto. Hasta el 7/9/2026 solo las ve el
 * ADMIN; después de la apertura quedan disponibles automáticamente al público.
 *
 * A diferencia de ProtectedRoute, NO manda al login: al visitante no se le pide
 * cuenta, se lo devuelve al embudo del sorteo. Mandarlo a un formulario de login
 * sería pedirle algo que no puede resolver durante la etapa previa.
 *
 * OJO: esto oculta el catálogo en la interfaz, no cierra la API. `GET /api/productos`
 * sigue siendo público (SecurityConfig), así que los datos son accesibles para
 * quien sepa pedirlos. Es una cortina, no una cerradura.
 */
function SoloAdmin({ children }) {
  const { isAdmin, listo } = useAuth()
  const catalogoAbierto = Date.now() >= new Date('2026-09-07T00:00:00-03:00').getTime()

  if (!listo) return null // esperando a recuperar la sesión

  return isAdmin || catalogoAbierto ? children : <Navigate to="/" replace />
}

export default SoloAdmin
