import { Link } from 'react-router-dom'
import LegalLayout from './LegalLayout'
import { CONTACTO_EMAIL, WHATSAPP_NUMBER } from '../../../config'

export default function GarantiaPage() {
  return (
    <LegalLayout titulo="Garantía y devoluciones" actualizado="30 de septiembre de 2026"
      intro="Qué cubre la garantía, cómo hacer un reclamo y cómo devolver un producto.">
      <section><h2>1. Garantía</h2>
        <p>Todos los productos tienen garantía por defectos de fabricación de <strong>al menos 3 meses</strong> desde la entrega. Si el fabricante ofrece una garantía oficial más larga (por ejemplo, 12 meses), rige la del fabricante. Vale para todos los productos, también los importados.</p>
        <p>Para la garantía vale el comprobante de compra que te entregamos con el producto (factura o remito): guardalo.</p>
        <p>La garantía cubre fallas de fabricación. No cubre daños por golpes, caídas, líquidos, sobretensión, mal uso, instalación incorrecta, apertura o reparación por terceros, ni el desgaste normal por el uso (por ejemplo, baterías fuera de los parámetros del fabricante).</p>
      </section>

      <section><h2>2. Cómo hacer un reclamo de garantía</h2>
        <ol>
          <li>Escribinos por WhatsApp (+{WHATSAPP_NUMBER}) o a {CONTACTO_EMAIL} con tu número de pedido o comprobante de compra, una descripción de la falla y, si podés, fotos o un video.</li>
          <li>Te indicamos si la gestión se hace con nosotros o con el servicio técnico oficial de la marca, que en muchos casos resuelve más rápido.</li>
          <li>Si corresponde, el producto se repara, se cambia por otro igual o, si no fuera posible, se reintegra lo pagado. Los costos de envío de un producto en garantía corren por nuestra cuenta.</li>
        </ol>
      </section>

      <section><h2>3. Arrepentimiento: devolver sin motivo</h2>
        <p>Tenés <strong>10 días corridos</strong> desde que recibiste el producto para revocar la compra, sin dar explicaciones y sin costo. Se hace con el <Link to="/arrepentimiento">Botón de arrepentimiento</Link>: te damos un código de trámite y te contactamos dentro de las 24 horas.</p>
        <p>El producto tiene que devolverse sin uso, completo, con sus accesorios y en su embalaje original. Los costos de la devolución corren por nuestra cuenta y te reintegramos el total pagado por el mismo medio de pago.</p>
      </section>

      <section><h2>4. Cambios</h2>
        <p>Si querés cambiar un producto por otro (por ejemplo, por otro color o modelo), consultanos dentro de los 10 días de recibido y con el producto sin uso y en su embalaje original. El cambio depende de la disponibilidad del producto nuevo; si tiene otro precio, se ajusta la diferencia.</p>
      </section>

      <section><h2>5. Productos que llegan dañados o equivocados</h2>
        <p>Si el paquete llega abierto o dañado, o el producto no es el que pediste, avisanos dentro de las 48 horas de recibido con fotos del paquete y del producto. Lo resolvemos sin costo para vos.</p>
      </section>
    </LegalLayout>
  )
}
