import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { IconEdit } from './icons'

/**
 * Acceso directo del admin a la edición completa de un producto desde la tienda (catálogo y
 * detalle). Solo se muestra con sesión de ADMIN; igual, la API de edición exige el rol en el
 * servidor. Al guardar o cancelar, Admin → Productos vuelve a esta misma URL (con filtros y página).
 */
function EditarComoAdmin({ productoId, className = '', style }) {
  const { isAdmin } = useAuth()
  const location = useLocation()
  if (!isAdmin) return null
  const params = new URLSearchParams({ editar: String(productoId), origen: 'tienda', volver: location.pathname + location.search })
  return (
    <Link to={`/admin/productos?${params}`} className={`editar-como-admin ${className}`.trim()} style={style}
      title="Editar este producto en el admin" onClick={e => e.stopPropagation()}>
      <IconEdit /> Editar
    </Link>
  )
}

export default EditarComoAdmin
