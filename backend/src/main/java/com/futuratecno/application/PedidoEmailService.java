package com.futuratecno.application;

import com.futuratecno.domain.Pedido;
import com.futuratecno.domain.PedidoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Emails que dispara un pedido: confirmación al cliente y aviso interno al admin.
 * Todo va por {@link EmailService#enviarHtmlAsync}, así un problema de mail nunca demora ni
 * hace fallar el alta del pedido.
 */
@Service
public class PedidoEmailService {
    private static final Logger logger = LoggerFactory.getLogger(PedidoEmailService.class);

    private static final Locale AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FMT_CORTE = DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm");

    private final EmailService emailService;

    @Value("${app.mail.admin-to:}")
    private String adminTo;

    public PedidoEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    /** Manda la confirmación al cliente y el aviso al admin. */
    public void notificarPedidoNuevo(Pedido pedido) {
        String emailCliente = pedido.getUsuario() != null ? pedido.getUsuario().getEmail() : null;
        if (emailCliente != null) {
            emailService.enviarHtmlAsync(
                    emailCliente,
                    "Recibimos tu pedido " + pedido.getNumero() + " — FuturaTecno",
                    htmlCliente(pedido));
        }

        if (adminTo != null && !adminTo.isBlank()) {
            emailService.enviarHtmlAsync(
                    adminTo,
                    "Pedido nuevo " + pedido.getNumero() + " (US$ " + pedido.getTotalUsd() + ")",
                    htmlAdmin(pedido, emailCliente));
        } else {
            logger.warn("Pedido {}: no hay ADMIN_NOTIFY_EMAIL/ADMIN_EMAIL configurado, no se avisó al admin.",
                    pedido.getNumero());
        }
    }

    private String htmlCliente(Pedido pedido) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,Helvetica,sans-serif;color:#16181d;max-width:600px\">");
        sb.append("<h2 style=\"margin-bottom:4px\">¡Gracias por tu pedido!</h2>");
        sb.append("<p style=\"color:#555;margin-top:0\">Pedido <strong>").append(pedido.getNumero())
          .append("</strong></p>");
        sb.append(tablaItems(pedido));
        sb.append("<p style=\"font-size:18px\"><strong>Total: US$ ").append(fmt(pedido.getTotalUsd()))
          .append("</strong> <span style=\"color:#555\">(aprox. $ ").append(fmt(pedido.getTotalArs()))
          .append(")</span></p>");
        sb.append(lineaEnvio(pedido));

        // La advertencia de vigencia que se acordó mostrar también en el checkout.
        sb.append("<div style=\"background:#FFF8E1;border-left:4px solid #C8E048;padding:12px 14px;margin:18px 0\">");
        sb.append("<strong>Tu pedido vale hasta el ").append(corteEnAr(pedido.getVenceEn())).append(" h.</strong><br>");
        sb.append("<span style=\"color:#555;font-size:14px\">Los precios se actualizan todas las mañanas junto con ")
          .append("el stock de nuestros proveedores. Si no lo cerramos antes de ese horario, el pedido vence y hay ")
          .append("que rehacerlo con los precios del día.</span>");
        sb.append("</div>");

        sb.append("<p style=\"color:#555;font-size:14px\">").append(instruccionPago(pedido)).append("</p>");
        sb.append("<p style=\"color:#888;font-size:12px\">FuturaTecno · Tu tecnología. Tu futuro.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String htmlAdmin(Pedido pedido, String emailCliente) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,Helvetica,sans-serif;color:#16181d;max-width:600px\">");
        sb.append("<h2>Pedido nuevo ").append(pedido.getNumero()).append("</h2>");
        sb.append("<p><strong>Cliente:</strong> ")
          .append(pedido.getNombreContacto() != null ? escapar(pedido.getNombreContacto()) : "—")
          .append("<br><strong>Email:</strong> ").append(emailCliente != null ? escapar(emailCliente) : "—")
          .append("<br><strong>Teléfono:</strong> ")
          .append(pedido.getTelefonoContacto() != null ? escapar(pedido.getTelefonoContacto()) : "—")
          .append("</p>");
        if (pedido.getNotas() != null) {
            sb.append("<p><strong>Notas:</strong> ").append(escapar(pedido.getNotas())).append("</p>");
        }
        sb.append(tablaItems(pedido));
        sb.append("<p style=\"font-size:18px\"><strong>Total: US$ ").append(fmt(pedido.getTotalUsd()))
          .append("</strong> · $ ").append(fmt(pedido.getTotalArs()))
          .append(" <span style=\"color:#888;font-size:13px\">(dólar ").append(fmt(pedido.getCotizacionUsada()))
          .append(")</span></p>");
        sb.append(lineaEnvio(pedido));
        sb.append("<p style=\"color:#b26a00\"><strong>Vence el ").append(corteEnAr(pedido.getVenceEn()))
          .append(" h.</strong> Después pasa a VENCIDO automáticamente.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String tablaItems(Pedido pedido) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"width:100%;border-collapse:collapse;margin:16px 0\">");
        sb.append("<tr style=\"background:#f2f2f2;text-align:left\">")
          .append("<th style=\"padding:8px\">Artículo</th>")
          .append("<th style=\"padding:8px;text-align:center\">Cant.</th>")
          .append("<th style=\"padding:8px;text-align:right\">Subtotal</th></tr>");
        for (PedidoItem i : pedido.getItems()) {
            sb.append("<tr style=\"border-bottom:1px solid #eee\">");
            sb.append("<td style=\"padding:8px\">").append(escapar(i.getProductoNombre()));
            if (i.getEspecificaciones() != null) {
                sb.append("<br><span style=\"color:#888;font-size:12px\">")
                  .append(escapar(i.getEspecificaciones())).append("</span>");
            }
            sb.append("</td>");
            sb.append("<td style=\"padding:8px;text-align:center\">").append(i.getCantidad()).append("</td>");
            sb.append("<td style=\"padding:8px;text-align:right\">US$ ").append(fmt(i.subtotalUsd())).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    /** Línea de envío elegido en el checkout. Vacía si no eligió (retiro / a coordinar). */
    private String lineaEnvio(Pedido pedido) {
        if (pedido.getModoEnvio() == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean entregaLocal = "entrega-local-olavarria".equals(pedido.getModoEnvio());
        sb.append("<p><strong>Envío:</strong> ")
          .append(entregaLocal ? "Entrega local en Olavarría" : "Andreani " + escapar(pedido.getModoEnvio()))
          .append(" a CP ").append(escapar(pedido.getCpDestino() != null ? pedido.getCpDestino() : "—"))
          .append(" — ");
        if (pedido.getCostoEnvioArs() != null) {
            if (entregaLocal && pedido.getCostoEnvioArs().signum() == 0) {
                sb.append("sin cargo");
            } else {
                sb.append("$ ").append(fmt(pedido.getCostoEnvioArs()))
                  .append(" <span style=\"color:#888;font-size:13px\">(estimado)</span>");
            }
        } else {
            sb.append("costo a cotizar");
        }
        sb.append("</p>");
        return sb.toString();
    }

    private String instruccionPago(Pedido pedido) {
        String compromiso = "Confirmar este pedido implica un <strong>compromiso de compra</strong>. ";
        if ("TRANSFERENCIA".equals(pedido.getMedioPago())) {
            return compromiso + "Coordinaremos los datos bancarios y el comprobante de transferencia por WhatsApp.";
        }
        if ("EFECTIVO".equals(pedido.getMedioPago())) {
            return compromiso + "Coordinaremos el pago en contado efectivo por WhatsApp. El total aplica el 7% de descuento sobre los productos.";
        }
        return compromiso + "El pago total se realiza online con Mercado Pago. Si todavía no lo completaste, podés volver al detalle del pedido para reintentarlo.";
    }

    /** venceEn se guarda en el huso de la JVM; para mostrarlo hay que volver a hora argentina. */
    private String corteEnAr(LocalDateTime venceEn) {
        if (venceEn == null) return "—";
        return venceEn.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(PedidoService.ZONA_AR)
                .format(FMT_CORTE);
    }

    private String fmt(BigDecimal n) {
        if (n == null) return "0,00";
        NumberFormat f = NumberFormat.getNumberInstance(AR);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(n);
    }

    /** Los datos vienen del usuario y terminan en un HTML: hay que escaparlos. */
    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
