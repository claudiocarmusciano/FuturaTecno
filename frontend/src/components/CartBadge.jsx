import { Link } from 'react-router-dom'
import { IconCart } from '../components/icons'

/**
 * Acceso al carrito con el contador de artículos. Se usa tanto en el header del catálogo
 * (PublicLayout) como en el de la landing, que tiene su propio markup.
 */
function CartBadge({ cantidad = 0, className }) {
  return (
    <Link
      to="/carrito"
      className={className}
      aria-label={cantidad > 0 ? `Carrito, ${cantidad} artículo(s)` : 'Carrito vacío'}
      style={{ position: 'relative', display: 'inline-flex', alignItems: 'center', textDecoration: 'none' }}
    >
      <IconCart size="20px" />
      {cantidad > 0 && (
        <span
          style={{
            position: 'absolute', top: '-7px', right: '-9px',
            background: 'var(--color-accion)', color: 'var(--color-sobre-accion)',
            fontSize: '11px', fontWeight: 700, lineHeight: 1,
            minWidth: '18px', height: '18px', borderRadius: '2px',
            display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 5px'
          }}
        >
          {cantidad > 99 ? '99+' : cantidad}
        </span>
      )}
    </Link>
  )
}

export default CartBadge
