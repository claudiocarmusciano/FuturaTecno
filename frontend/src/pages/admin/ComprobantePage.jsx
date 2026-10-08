import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import axios from 'axios'
import { TITULAR, CONTACTO_EMAIL, WHATSAPP_NUMBER } from '../../config'
import { etiquetaEnvio } from '../../utils/envio'
import { textoDemora } from '../../utils/demora'
import './ComprobantePage.css'

const formatNumber = (n) =>
  Number(n || 0).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatFecha = (iso) => iso ? new Date(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : ''

const MEDIO = { EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', MERCADO_PAGO: 'Mercado Pago' }

/** Teléfono argentino → número para wa.me (54 9 + área + número). Null si no se puede afirmar. */
function telefonoWhatsapp(t) {
  let d = (t || '').replace(/\D/g, '')
  if (!d) return null
  if (d.startsWith('54')) return d.startsWith('549') ? d : '549' + d.slice(2)
  d = d.replace(/^0/, '').replace(/^(\d{2,4})15/, '$1')   // "0 2284 15 55-1234" → "2284551234"
  return d.length === 10 ? '549' + d : null
}

/**
 * Comprobante de un pedido para entregarle al cliente (sirve como respaldo de la garantía). NO es
 * una factura ni un remito fiscal: lo dice el propio documento.
 */
export default function ComprobantePage() {
  const { numero } = useParams()
  const [p, setP] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    axios.get(`/api/admin/pedidos/numero/${encodeURIComponent(numero)}`)
      .then(r => setP(r.data))
      .catch(err => setError(err.response?.data?.error || 'No se pudo cargar el pedido.'))
  }, [numero])

  useEffect(() => {
    if (!p) return
    const anterior = document.title
    document.title = `Comprobante ${p.numero} — Tecnópolis Olavarría`
    return () => { document.title = anterior }
  }, [p])

  if (error) return <div className="comp-pagina"><p>{error}</p><Link to="/admin/pedidos">← Volver a pedidos</Link></div>
  if (!p) return <div className="comp-pagina"><p>Cargando…</p></div>

  const envio = p.costoEnvioArs != null ? Number(p.costoEnvioArs) : null
  // El total a cobrar lo calcula el backend según el medio de pago (7% off en efectivo, recargo de
  // Mercado Pago), igual que en la web.
  const totalArs = p.totalCobroArs != null ? Number(p.totalCobroArs) : Number(p.totalArs) + (envio || 0) - Number(p.descuentoPuntosArs || 0)
  const email = p.usuarioEmail || p.emailContacto

  const textoWhatsapp = [
    `Hola ${p.nombreContacto || ''}! Te paso el comprobante de tu pedido ${p.numero} en Tecnópolis Olavarría:`,
    ...p.items.map(i => `• ${i.cantidad} × ${i.productoNombre} — US$ ${formatNumber(i.subtotalUsd)}`),
    `Total: US$ ${formatNumber(p.totalUsd)} (≈ $ ${formatNumber(totalArs)})`,
    `Pago: ${MEDIO[p.medioPago] || p.medioPago}${p.estadoPago === 'APROBADO' ? ' (cobrado)' : ''}`,
    p.modoEnvio ? `Entrega: ${etiquetaEnvio(p.modoEnvio)}` : null,
    'Guardá este mensaje: es tu comprobante para la garantía. Condiciones: www.futuratecno.com.ar/garantia'
  ].filter(Boolean).join('\n')
  const tel = telefonoWhatsapp(p.telefonoContacto)
  // Sin número, wa.me deja elegir el contacto: sirve también cuando el teléfono del pedido está mal.
  const linkElegirContacto = `https://wa.me/?text=${encodeURIComponent(textoWhatsapp)}`
  const linkWhatsapp = tel ? `https://wa.me/${tel}?text=${encodeURIComponent(textoWhatsapp)}` : linkElegirContacto

  return (
    <div className="comp-pagina">
      <div className="comp-acciones no-print">
        <Link to="/admin/pedidos">← Volver a pedidos</Link>
        <div>
          <button type="button" onClick={() => window.print()}>Imprimir o guardar PDF</button>
          <a href={linkWhatsapp} target="_blank" rel="noreferrer" className="comp-wa">
            {tel ? `Mandar por WhatsApp (+${tel})` : 'Mandar por WhatsApp (elegir contacto)'}
          </a>
          {tel && (
            <a href={linkElegirContacto} target="_blank" rel="noreferrer" className="comp-wa comp-wa-alt">
              Elegir otro contacto
            </a>
          )}
        </div>
      </div>

      <article className="comp-hoja">
        <header className="comp-cabecera">
          <div>
            <img src="/marca/tecnopolis-olavarria-logo-sobre-blanco.svg" alt="Tecnópolis Olavarría" className="comp-logo" />
            <div className="comp-titular">
              Tecnópolis Olavarría — nombre comercial de {TITULAR.nombre}<br />
              CUIT {TITULAR.cuit} · {TITULAR.condicionIva}<br />
              {TITULAR.domicilioComercial}<br />
              {CONTACTO_EMAIL} · WhatsApp +{WHATSAPP_NUMBER}
            </div>
          </div>
          <div className="comp-numero">
            <div className="comp-tipo">Comprobante de pedido</div>
            <div className="comp-nro">{p.numero}</div>
            <div>Fecha: {formatFecha(p.createdAt)}</div>
            <div className="comp-no-factura">Documento no válido como factura</div>
          </div>
        </header>

        <section className="comp-cliente">
          <strong>Cliente:</strong> {p.nombreContacto || '—'}
          {p.telefonoContacto && <> · Tel. {p.telefonoContacto}</>}
          {email && <> · {email}</>}
        </section>

        <table className="comp-tabla">
          <thead>
            <tr><th>Cant.</th><th>Artículo</th><th className="num">Precio unit.</th><th className="num">Subtotal</th></tr>
          </thead>
          <tbody>
            {p.items.map(i => (
              <tr key={i.id}>
                <td>{i.cantidad}</td>
                <td>
                  {i.productoNombre}
                  {(i.especificaciones || i.sku) && <div className="comp-detalle">{[i.especificaciones, i.sku && `Cód. ${i.sku}`].filter(Boolean).join(' · ')}</div>}
                  {textoDemora(i) && <div className="comp-detalle">Entrega en {textoDemora(i)}</div>}
                </td>
                <td className="num">US$ {formatNumber(i.precioUnitarioUsd)}</td>
                <td className="num">US$ {formatNumber(i.subtotalUsd)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="comp-totales">
          <div><span>Total productos</span><strong>US$ {formatNumber(p.totalUsd)}</strong></div>
          <div className="comp-detalle"><span>En pesos (dólar $ {formatNumber(p.cotizacionUsada)})</span><span>$ {formatNumber(p.totalArs)}</span></div>
          {p.modoEnvio && <div><span>{etiquetaEnvio(p.modoEnvio)}</span><span>{envio == null ? 'a cotizar' : envio === 0 ? 'sin cargo' : `$ ${formatNumber(envio)}`}</span></div>}
          {Number(p.descuentoPuntosArs) > 0 && <div><span>Puntos canjeados</span><span>− $ {formatNumber(p.descuentoPuntosArs)}</span></div>}
          {p.medioPago === 'EFECTIVO' && <div className="comp-detalle"><span>Descuento pago en efectivo</span><span>7% sobre productos</span></div>}
          <div className="comp-total"><span>Total a pagar ({MEDIO[p.medioPago] || p.medioPago})</span><strong>$ {formatNumber(totalArs)}</strong></div>
          <div className="comp-detalle"><span>Pago: {MEDIO[p.medioPago] || p.medioPago}</span><span>{p.estadoPago === 'APROBADO' ? 'Cobrado' : 'Pendiente de pago'}</span></div>
        </div>

        {p.notas && <p className="comp-notas"><strong>Notas:</strong> {p.notas}</p>}

        <footer className="comp-pie">
          Garantía por defectos de fabricación de al menos 3 meses desde la entrega, o la del fabricante si es mayor.
          Conservá este comprobante: es el que vale para la garantía. Condiciones completas en www.futuratecno.com.ar/garantia.
          Podés revocar la compra dentro de los 10 días de recibida en www.futuratecno.com.ar/arrepentimiento.
        </footer>
      </article>
    </div>
  )
}
