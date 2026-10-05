import { useEffect, useState } from 'react'
import axios from 'axios'
import { useAuth } from '../auth/AuthContext'

const formatUsd = (n) => Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/**
 * Costo del mayorista de los productos que se están viendo, SOLO con sesión de ADMIN. El catálogo
 * público no trae el costo (ni debe): se pide aparte a /api/admin, que exige el rol en el servidor.
 * Para cualquier otro usuario no se hace ninguna llamada y devuelve un objeto vacío.
 */
export function useCostosAdmin(ids) {
  const { isAdmin } = useAuth()
  const [costos, setCostos] = useState({})
  const clave = isAdmin ? ids.join(',') : ''
  useEffect(() => {
    if (!clave) { setCostos({}); return }
    let vivo = true
    axios.get('/api/admin/productos/costos', { params: { ids: clave } })
      .then(r => { if (vivo) setCostos(r.data || {}) })
      .catch(() => { if (vivo) setCostos({}) })
    return () => { vivo = false }
  }, [clave])
  return costos
}

/** "Costo: US$ 389,90", visible solo para el admin (sin costo cargado no muestra nada). */
export default function CostoAdmin({ costo, style }) {
  const { isAdmin } = useAuth()
  if (!isAdmin || costo == null) return null
  return (
    <p className="costo-admin" title="Costo del mayorista, antes de flete y margen. Solo lo ve el admin."
      style={{ margin: '0 0 12px', fontSize: '12px', fontWeight: 700, color: '#f0c05a', ...style }}>
      Costo: US$ {formatUsd(costo)} <span style={{ fontWeight: 400, opacity: .75 }}>· solo admin</span>
    </p>
  )
}
