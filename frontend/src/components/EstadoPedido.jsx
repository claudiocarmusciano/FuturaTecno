/**
 * Presentación de los estados de pedido. Los valores tienen que coincidir con el enum
 * EstadoPedido del backend (y con el CHECK de la tabla pedidos en V13).
 */
export const ESTADO_LABEL = {
  PENDIENTE: 'Pendiente',
  CONFIRMADO: 'Confirmado',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
  VENCIDO: 'Vencido'
}

const ESTADO_COLOR = {
  PENDIENTE: { bg: '#FFF4CC', fg: '#7A5C00' },
  CONFIRMADO: { bg: '#DCEEFF', fg: '#0B4F86' },
  ENTREGADO: { bg: '#DFF3E0', fg: '#1E6B2A' },
  CANCELADO: { bg: '#F2F2F2', fg: '#5A5A5A' },
  VENCIDO: { bg: '#FBE0DE', fg: '#8A2318' }
}

export function EstadoChip({ estado }) {
  const color = ESTADO_COLOR[estado] || ESTADO_COLOR.CANCELADO
  return (
    <span style={{
      display: 'inline-block', padding: '3px 10px', borderRadius: '999px',
      background: color.bg, color: color.fg, fontSize: '12px', fontWeight: 700, whiteSpace: 'nowrap'
    }}>
      {ESTADO_LABEL[estado] || estado}
    </span>
  )
}

export const ESTADOS = Object.keys(ESTADO_LABEL)
