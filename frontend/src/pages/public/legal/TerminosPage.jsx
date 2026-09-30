import { Link } from 'react-router-dom'
import LegalLayout from './LegalLayout'
import { TITULAR, CONTACTO_EMAIL, WHATSAPP_NUMBER, DEFENSA_CONSUMIDOR_URL } from '../../../config'

export default function TerminosPage() {
  return (
    <LegalLayout titulo="Términos y condiciones" actualizado="30 de septiembre de 2026"
      intro="Estas son las reglas de compra en www.futuratecno.com.ar. Al confirmar un pedido las aceptás.">
      <section><h2>1. Quiénes somos</h2>
        <p>Futura Tecno es el nombre comercial de {TITULAR.nombre}, CUIT {TITULAR.cuit}, {TITULAR.condicionIva} ante ARCA, con domicilio fiscal en {TITULAR.domicilioFiscal} y domicilio comercial en {TITULAR.domicilioComercial}.</p>
        <p>Contacto: {CONTACTO_EMAIL} · WhatsApp +{WHATSAPP_NUMBER}.</p>
      </section>

      <section><h2>2. Productos e imágenes</h2>
        <p>Los productos se describen con la información que publican el fabricante y nuestros proveedores. Las imágenes son ilustrativas: si una característica, color o versión es importante para vos, consultanos antes de confirmar y te lo confirmamos por escrito.</p>
        <p>Salvo que la publicación diga lo contrario, todos los productos son nuevos.</p>
      </section>

      <section><h2>3. Precios</h2>
        <p>Los precios se expresan en dólares estadounidenses (USD) y se muestran también en pesos, calculados con la cotización del dólar oficial del día. Se actualizan todos los días a las 6:30 h (hora de Argentina) junto con los costos de nuestros proveedores.</p>
        <p>El precio que pagás es el que figura al <strong>confirmar el pedido</strong>: desde ese momento queda fijo y un cambio posterior del catálogo no lo modifica. Un pedido confirmado mantiene su precio hasta las 6:30 h del día siguiente; si no se completó el pago para entonces, vence y hay que rehacerlo con los precios del día.</p>
      </section>

      <section><h2>4. Pedido y disponibilidad</h2>
        <p>Para confirmar un pedido tenés que tener una cuenta y aceptar el compromiso de compra. El monto mínimo del pedido es de US$ 250 en productos, antes de envío, puntos o descuentos.</p>
        <p>Trabajamos con stock de proveedores que rota todos los días, así que la disponibilidad se confirma al procesar el pedido. Si un producto ya no está disponible te avisamos y podés elegir otro, esperar su reposición o recibir el reintegro total de lo que hayas pagado por él.</p>
      </section>

      <section><h2>5. Medios de pago</h2>
        <ul>
          <li><strong>Mercado Pago</strong> (tarjetas, dinero en cuenta y los medios que ofrezca Mercado Pago). Las cuotas disponibles y su costo las define Mercado Pago según la tarjeta y las promociones vigentes al momento de pagar.</li>
          <li><strong>Transferencia bancaria:</strong> te enviamos los datos por WhatsApp.</li>
          <li><strong>Efectivo:</strong> con un 7% de descuento sobre el valor de los productos (no sobre el envío).</li>
        </ul>
        <p>El precio de cada medio de pago se muestra antes de confirmar.</p>
      </section>

      <section><h2>6. Envíos y entregas</h2>
        <p>Enviamos a todo el país por Andreani, a domicilio o a sucursal. El costo se cotiza en el checkout con tu código postal; si en ese momento no se puede cotizar, el envío queda "a cotizar" y te lo informamos antes de despachar. Dentro de Olavarría la entrega es sin cargo.</p>
        <p>La fecha de entrega que mostramos es estimada: por lo general, 3 días hábiles para pedidos confirmados antes de las 14 h. Algunos productos tienen una demora mayor, que se indica en la publicación, el carrito y el checkout (por ejemplo, "Entrega en 15 a 20 días").</p>
        <p>Al recibir el paquete revisá que esté cerrado y sin daños. Si no lo está, dejalo asentado ante el transportista y avisanos dentro de las 48 h.</p>
      </section>


      <section><h2>7. Arrepentimiento, garantía y cambios</h2>
        <p>Podés revocar la compra dentro de los 10 días corridos desde que recibiste el producto, sin dar explicaciones, con el <Link to="/arrepentimiento">Botón de arrepentimiento</Link>. Las condiciones de garantía y cambios están en <Link to="/garantia">Garantía y devoluciones</Link>.</p>
      </section>

      <section><h2>8. Puntos y promociones</h2>
        <p>Los puntos del Club FuturaTecno y las promociones tienen las condiciones que se informan en cada caso. Los puntos no son canjeables por dinero.</p>
      </section>

      <section><h2>9. Datos personales</h2>
        <p>Usamos tus datos para gestionar tus pedidos, como se explica en la <Link to="/privacidad">Política de privacidad</Link>.</p>
      </section>

      <section><h2>10. Reclamos</h2>
        <p>Ante cualquier problema escribinos a {CONTACTO_EMAIL} o por WhatsApp: lo resolvemos directamente. También podés hacer tu reclamo ante <a href={DEFENSA_CONSUMIDOR_URL} target="_blank" rel="noreferrer">Defensa del Consumidor</a>. Rige la ley argentina y, en particular, la Ley 24.240 de Defensa del Consumidor.</p>
      </section>
    </LegalLayout>
  )
}
