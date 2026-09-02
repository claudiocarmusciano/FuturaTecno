import { useState, useEffect } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import axios from 'axios'
import { EstadoChip } from '../../components/EstadoPedido'
import { WHATSAPP_NUMBER, NOMBRE_NEGOCIO } from '../../config'

const formatNumber = (n) =>
  Number(n).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// Mismos códigos que devuelve Andreani (ver CheckoutPage).
const ETIQUETA_ENVIO = {
  'estándar': 'Envío a domicilio',
  'sucursal': 'Retiro en sucursal Andreani',
  'llega hoy': 'Llega hoy (a domicilio)',
  'bigger': 'Envío de paquete grande'
}
const etiquetaEnvio = (codigo) => ETIQUETA_ENVIO[codigo] || `Envío ${codigo}`

const formatFechaHora = (iso) =>
  iso ? new Date(iso).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'

// Sin "h" al final: el formato es-AR ya agrega "a. m." / "p. m.".
const formatCorte = (iso) =>
  iso ? new Date(iso).toLocaleString('es-AR', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' }) : null

function PedidoDetailPage() {
  const { numero } = useParams()
  const location = useLocation()
  const [pedido, setPedido] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [abriendoPago, setAbriendoPago] = useState(false)
  const [errorPago, setErrorPago] = useState(location.state?.pagoError || '')

  useEffect(() => {
    axios.get(`/api/pedidos/${numero}`)
      .then(res => setPedido(res.data))
      .catch(() => setError('No encontramos ese pedido.'))
      .finally(() => setCargando(false))
  }, [numero])

  const volver = <Link to="/mis-pedidos" style={{ color: 'var(--color-accent)', textDecoration: 'none', fontSize: '14px', fontWeight: 600 }}>← Mis pedidos</Link>

  if (cargando) return (<div>{volver}<div className="card" style={{ marginTop: '16px' }}><p>Cargando...</p></div></div>)
  if (error || !pedido) return (<div>{volver}<div className="card" style={{ marginTop: '16px' }}>{error}</div></div>)

  const mensaje = `Hola ${NOMBRE_NEGOCIO}, necesito ayuda con el pago del pedido ${pedido.numero}.`
  const waLink = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(mensaje)}`
  const pagado = pedido.estadoPago === 'APROBADO'
  const esTransferencia = pedido.medioPago === 'TRANSFERENCIA'
  const puedePagar = !esTransferencia && !pagado && !['VENCIDO', 'CANCELADO', 'ENTREGADO'].includes(pedido.estado)

  const pagar = async () => {
    setErrorPago('')
    setAbriendoPago(true)
    try {
      const { data } = await axios.post(`/api/pedidos/${pedido.numero}/pago/mercadopago`)
      window.location.assign(data.checkoutUrl)
    } catch (err) {
      setErrorPago(err.response?.data?.error || 'No pudimos abrir Mercado Pago. Probá nuevamente.')
      setAbriendoPago(false)
    }
  }

  return (
    <div>
      {volver}

      <div className="card" style={{ marginTop: '16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '14px', flexWrap: 'wrap' }}>
          <div>
            <h1 style={{ margin: '0 0 4px', fontSize: '26px' }}>Pedido {pedido.numero}</h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--color-text-muted)' }}>
              Hecho el {formatFechaHora(pedido.createdAt)}
            </p>
          </div>
          <EstadoChip estado={pedido.estado} />
        </div>
      </div>

      <div className="card" style={{ borderLeft: `4px solid ${pagado ? 'var(--color-lime)' : 'var(--color-border)'}` }}>
        <h2 style={{ fontSize: '17px', margin: '0 0 8px' }}>Pago</h2>
        <p style={{ margin: '0 0 8px', color: 'var(--color-text-muted)', fontSize: '14px' }}>
          {esTransferencia ? 'Transferencia bancaria' : 'Mercado Pago'}
        </p>
        <p style={{ margin: '0 0 10px', fontWeight: 700 }}>
          {pagado ? 'Pago aprobado' : pedido.estadoPago === 'EN_PROCESO' ? 'Pago en revisión' : 'Pago pendiente'}
        </p>
        {pagado && pedido.pagadoEn && (
          <p style={{ margin: 0, fontSize: '14px', color: 'var(--color-text-muted)' }}>Acreditado el {formatFechaHora(pedido.pagadoEn)}</p>
        )}
        {!pagado && (
          <p style={{ margin: '0 0 14px', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Total a cobrar: <strong>$ {formatNumber(pedido.totalCobroArs)}</strong>
          </p>
        )}
        {esTransferencia && !pagado && (
          <p style={{ margin: '0 0 14px', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Contactanos para recibir los datos bancarios y enviar el comprobante. El pedido se procesa cuando confirmamos la acreditación.
          </p>
        )}
        {errorPago && <p style={{ color: 'var(--color-danger, #c0392b)', fontSize: '14px' }}>{errorPago}</p>}
        {puedePagar && (
          <button type="button" onClick={pagar} disabled={abriendoPago} className="btn-primario"
            style={{ border: 0, cursor: abriendoPago ? 'default' : 'pointer', opacity: abriendoPago ? 0.6 : 1 }}>
            {abriendoPago ? 'Abriendo Mercado Pago...' : 'Pagar con Mercado Pago'}
          </button>
        )}
      </div>

      {pedido.estado === 'PENDIENTE' && pedido.venceEn && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-lime)' }}>
          <strong>Vale hasta el {formatCorte(pedido.venceEn)}.</strong>
          <p style={{ margin: '6px 0 0', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Completá el pago antes de ese horario. Después se actualizan los precios y el pedido vence.
          </p>
        </div>
      )}

      {pedido.estado === 'VENCIDO' && (
        <div className="card" style={{ borderLeft: '4px solid var(--color-danger, #c0392b)' }}>
          <strong>Este pedido venció.</strong>
          <p style={{ margin: '6px 0 12px', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Los precios cambiaron desde que lo hiciste. Podés armarlo de nuevo con los valores de hoy.
          </p>
          <Link to="/catalogo" style={{ fontWeight: 600, color: 'var(--color-accent)' }}>Ir al catálogo →</Link>
        </div>
      )}

      <div className="card">
        <h2 style={{ fontSize: '17px', marginTop: 0 }}>Artículos</h2>
        {pedido.items.map(i => (
          <div key={i.id} style={{ display: 'flex', gap: '12px', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid var(--color-border)' }}>
            {i.imagenUrl
              ? <img src={i.imagenUrl} alt="" style={{ width: '52px', height: '52px', objectFit: 'contain', borderRadius: '6px' }} />
              : <div style={{ width: '52px', height: '52px', borderRadius: '6px', background: 'var(--color-accent-light)' }} />}
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 600 }}>{i.productoNombre}</div>
              {i.especificaciones && <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>{i.especificaciones}</div>}
              <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                {i.sku && <>Cód. {i.sku} · </>}{i.cantidad} × US$ {formatNumber(i.precioUnitarioUsd)}
              </div>
            </div>
            <div style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
              <div style={{ fontWeight: 600 }}>US$ {formatNumber(i.subtotalUsd)}</div>
              <div style={{ fontSize: '13px', color: 'var(--color-price)' }}>$ {formatNumber(i.subtotalArs)}</div>
            </div>
          </div>
        ))}

        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '16px', fontSize: '20px', fontWeight: 700 }}>
          <span>Total</span>
          <span>US$ {formatNumber(pedido.totalUsd)}</span>
        </div>
        <div style={{ textAlign: 'right', color: 'var(--color-price)' }}>$ {formatNumber(pedido.totalArs)}</div>
        <p style={{ textAlign: 'right', fontSize: '12px', color: 'var(--color-text-muted)', margin: '4px 0 0' }}>
          Precios congelados al dólar ${formatNumber(pedido.cotizacionUsada)} del día del pedido.
        </p>
      </div>

      {pedido.modoEnvio && (
        <div className="card">
          <h2 style={{ fontSize: '17px', marginTop: 0 }}>Envío</h2>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
            <span>
              {etiquetaEnvio(pedido.modoEnvio)}
              {pedido.cpDestino && <span style={{ color: 'var(--color-text-muted)' }}> · CP {pedido.cpDestino}</span>}
            </span>
            <strong style={{ whiteSpace: 'nowrap' }}>
              {pedido.costoEnvioArs != null ? `$ ${formatNumber(pedido.costoEnvioArs)}` : 'A cotizar'}
            </strong>
          </div>
          {pedido.costoEnvioArs != null && (
            <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--color-text-muted)' }}>
              Costo estimado de Andreani al momento del pedido. Se confirma al cerrarlo.
            </p>
          )}
        </div>
      )}

      {(pedido.nombreContacto || pedido.telefonoContacto || pedido.notas) && (
        <div className="card">
          <h2 style={{ fontSize: '17px', marginTop: 0 }}>Tus datos</h2>
          {pedido.nombreContacto && <p style={{ margin: '0 0 4px' }}>{pedido.nombreContacto}</p>}
          {pedido.telefonoContacto && <p style={{ margin: '0 0 4px', color: 'var(--color-text-muted)' }}>{pedido.telefonoContacto}</p>}
          {pedido.notas && <p style={{ margin: '8px 0 0', fontSize: '14px', color: 'var(--color-text-muted)' }}>{pedido.notas}</p>}
        </div>
      )}

      <a
        href={waLink}
        target="_blank"
        rel="noreferrer"
        style={{
          display: 'inline-flex', alignItems: 'center', gap: '10px',
          background: '#25D366', color: 'white', textDecoration: 'none',
          padding: '12px 24px', borderRadius: '8px', fontSize: '16px', fontWeight: 600
        }}
      >
        <span style={{ fontSize: '20px' }}>💬</span> Consultar por WhatsApp
      </a>
    </div>
  )
}

export default PedidoDetailPage
