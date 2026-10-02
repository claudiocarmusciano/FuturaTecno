import LegalLayout from './LegalLayout'
import { TITULAR, CONTACTO_EMAIL } from '../../../config'

export default function PrivacidadPage() {
  return (
    <LegalLayout titulo="Política de privacidad" actualizado="30 de septiembre de 2026"
      intro="Qué datos tuyos guardamos, para qué los usamos y cómo podés pedir que los corrijamos o borremos.">
      <section><h2>1. Responsable</h2>
        <p>El responsable de tus datos es {TITULAR.nombre} (Futura Tecno), CUIT {TITULAR.cuit}, con domicilio en {TITULAR.domicilioFiscal}. Para cualquier consulta sobre tus datos escribinos a {CONTACTO_EMAIL}.</p>
      </section>

      <section><h2>2. Qué datos guardamos</h2>
        <ul>
          <li><strong>Al crear tu cuenta:</strong> nombre, apellido, email, celular y fecha de nacimiento. Si te registraste antes de octubre de 2026, también el DNI y el usuario de Instagram que nos diste en ese momento. Si entrás con Google, recibimos tu nombre y tu email de Google.</li>
          <li><strong>Al comprar:</strong> los productos, importes, medio de pago elegido, teléfono de contacto, código postal y datos de envío. Los datos de tu tarjeta los procesa Mercado Pago: nosotros no los vemos ni los guardamos.</li>
          <li><strong>Al escribirnos</strong> por WhatsApp o por el chat del sitio: el contenido de la conversación y tu número o nombre de contacto.</li>
          <li><strong>Al navegar:</strong> datos técnicos y de uso (páginas visitadas, productos vistos, dispositivo) mediante cookies de medición.</li>
        </ul>
      </section>

      <section><h2>3. Para qué los usamos</h2>
        <ul>
          <li>Gestionar tu cuenta, tus pedidos, pagos, envíos, facturas y reclamos.</li>
          <li>Responder tus consultas, también mediante un asistente automático (Tecnito) que puede derivar la conversación a una persona.</li>
          <li>Gestionar las promociones en las que participes.</li>
          <li>Medir cómo se usa el sitio y la efectividad de nuestros anuncios, para mejorar la tienda.</li>
          <li>Enviarte novedades u ofertas, si nos diste tu consentimiento. Podés darte de baja en cualquier momento.</li>
        </ul>
        <p>No vendemos ni alquilamos tus datos.</p>
      </section>

      <section><h2>4. Con quién los compartimos</h2>
        <p>Solo con los servicios que necesitamos para operar, y en la medida necesaria: Mercado Pago (pagos), Andreani (envíos), Resend (envío de mails), Google (inicio de sesión y medición con Google Analytics), Meta (WhatsApp y medición de anuncios con el Pixel de Meta), el proveedor del asistente de chat y los servicios de alojamiento del sitio. Algunos de estos servicios pueden almacenar datos fuera de Argentina, con resguardos equivalentes.</p>
      </section>

      <section><h2>5. Cookies y medición</h2>
        <p>Usamos cookies propias para mantener tu sesión y tu carrito, y cookies de Google Analytics y del Pixel de Meta para saber cómo se usa el sitio y medir nuestros anuncios. Podés bloquearlas o borrarlas desde la configuración de tu navegador; la tienda sigue funcionando.</p>
      </section>

      <section><h2>6. Cuánto tiempo los guardamos</h2>
        <p>Mientras tengas una cuenta activa y, después, el tiempo que exijan las obligaciones legales, contables e impositivas (por ejemplo, las facturas).</p>
      </section>

      <section><h2>7. Tus derechos</h2>
        <p>Podés pedir acceso, corrección, actualización o eliminación de tus datos escribiendo a {CONTACTO_EMAIL}. Te respondemos dentro de los plazos de la Ley 25.326.</p>
        <p>El titular de los datos personales tiene la facultad de ejercer el derecho de acceso a los mismos en forma gratuita a intervalos no inferiores a seis meses, salvo que se acredite un interés legítimo al efecto, conforme lo establecido en el artículo 14, inciso 3 de la Ley N° 25.326.</p>
        <p>La AGENCIA DE ACCESO A LA INFORMACIÓN PÚBLICA, en su carácter de Órgano de Control de la Ley N° 25.326, tiene la atribución de atender las denuncias y reclamos que interpongan quienes resulten afectados en sus derechos por incumplimiento de las normas vigentes en materia de protección de datos personales.</p>
      </section>
    </LegalLayout>
  )
}
