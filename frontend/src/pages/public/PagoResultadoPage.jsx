import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from 'axios'

function PagoResultadoPage() {
  const [params] = useSearchParams()
  const numero = params.get('pedido')
  const paymentId = params.get('payment_id') || params.get('collection_id')
  const [pedido, setPedido] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!numero) {
      setError('No encontramos el número de pedido en el regreso de Mercado Pago.')
      setCargando(false)
      return
    }
    const confirmar = paymentId
      ? axios.post(`/api/pedidos/${numero}/pago/mercadopago/confirmar-retorno`, null, { params: { paymentId } })
      : axios.get(`/api/pedidos/${numero}`)
    confirmar
      .then(res => setPedido(res.data))
      .catch(err => setError(err.response?.data?.error || 'No pudimos comprobar el pago. Revisá el pedido en unos instantes.'))
      .finally(() => setCargando(false))
  }, [numero, paymentId])

  const aprobado = pedido?.estadoPago === 'APROBADO'
  const titulo = cargando ? 'Comprobando tu pago...' : aprobado ? '¡Pago aprobado!' : 'El pago todavía no está aprobado'

  return (
    <div className="card" style={{ maxWidth: '620px', margin: '30px auto', textAlign: 'center', padding: '38px 24px', borderTop: '5px solid var(--color-lime)' }}>
      <h1 style={{ marginTop: 0 }}>{titulo}</h1>
      {cargando && <p style={{ color: 'var(--color-text-muted)' }}>Estamos consultando el estado directamente en Mercado Pago.</p>}
      {!cargando && aprobado && <p>Recibimos el pago del pedido <strong>{numero}</strong>. Ya podemos comenzar a procesarlo.</p>}
      {!cargando && !aprobado && !error && (
        <p style={{ color: 'var(--color-text-muted)' }}>Puede estar pendiente, en revisión o no haberse completado. Podés revisarlo y volver a intentarlo.</p>
      )}
      {error && <p style={{ color: 'var(--color-danger, #c0392b)' }}>{error}</p>}
      {numero && <Link to={`/pedido/${numero}`} className="btn-primario" style={{ display: 'inline-block', textDecoration: 'none', marginTop: '12px' }}>Ver mi pedido</Link>}
    </div>
  )
}

export default PagoResultadoPage
