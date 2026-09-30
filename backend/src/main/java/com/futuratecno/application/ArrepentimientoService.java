package com.futuratecno.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Botón de arrepentimiento (Res. SCI 424/2020): el cliente pide revocar una compra online y recibe
 * un código de trámite. El pedido de revocación se le avisa al admin por mail para que lo gestione
 * dentro de las 24 h que exige la norma; no se toca ningún pedido solo, porque la devolución
 * (retiro del producto, reintegro) la coordina una persona.
 *
 * <p>Es un endpoint público que manda mails, así que tiene freno propio: pocas solicitudes por IP
 * y por email en una ventana de una hora. Sin eso serviría para mandar correo en masa desde el
 * dominio de la tienda (y quemar la reputación del remitente en Resend).
 */
@Service
public class ArrepentimientoService {
    private static final Logger logger = LoggerFactory.getLogger(ArrepentimientoService.class);

    static final int MAXIMO_POR_HORA = 3;
    private static final Duration VENTANA = Duration.ofHours(1);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]{1,64}@[^\\s@]{1,190}\\.[^\\s@]{2,}$");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("America/Argentina/Buenos_Aires"));
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Argentina/Buenos_Aires"));
    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // sin 0/O ni 1/I: se dictan por teléfono

    public record Solicitud(String nombre, String email, String telefono, String numeroPedido, String detalle) {}

    public static class DemasiadasSolicitudesException extends RuntimeException {
        public DemasiadasSolicitudesException() {
            super("Ya recibimos varias solicitudes. Si necesitás ayuda, escribinos por WhatsApp.");
        }
    }

    private final EmailService emailService;
    private final String adminTo;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Deque<Instant>> recientes = new ConcurrentHashMap<>();

    @Autowired   // hay un segundo constructor (con reloj) para los tests: Spring tiene que saber cuál usar
    public ArrepentimientoService(EmailService emailService, @Value("${app.mail.admin-to:}") String adminTo) {
        this(emailService, adminTo, Clock.systemUTC());
    }

    ArrepentimientoService(EmailService emailService, String adminTo, Clock clock) {
        this.emailService = emailService;
        this.adminTo = adminTo;
        this.clock = clock;
    }

    /** Registra la solicitud y devuelve el código de trámite. */
    public String solicitar(Solicitud s, String ip) {
        String nombre = limpio(s.nombre(), 120);
        String email = limpio(s.email(), 190);
        if (nombre == null) throw new IllegalArgumentException("Completá tu nombre y apellido.");
        if (email == null || !EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Ingresá un email válido.");
        String telefono = limpio(s.telefono(), 40);
        String pedido = limpio(s.numeroPedido(), 40);
        String detalle = limpio(s.detalle(), 1000);

        Instant ahora = clock.instant();
        if (!admitir("ip:" + ip, ahora) | !admitir("email:" + email.toLowerCase(), ahora)) {
            throw new DemasiadasSolicitudesException();
        }

        String codigo = "ARR-" + FECHA.format(ahora) + "-" + sufijo();
        logger.info("Arrepentimiento {} recibido (pedido {})", codigo, pedido != null ? pedido : "sin número");

        if (adminTo != null && !adminTo.isBlank()) {
            emailService.enviarHtmlAsync(adminTo, "Botón de arrepentimiento " + codigo + " — responder dentro de 24 h",
                    htmlAdmin(codigo, nombre, email, telefono, pedido, detalle, ahora));
        } else {
            logger.warn("Arrepentimiento {}: no hay ADMIN_NOTIFY_EMAIL/ADMIN_EMAIL configurado.", codigo);
        }
        emailService.enviarHtmlAsync(email, "Recibimos tu solicitud de arrepentimiento " + codigo + " — FuturaTecno",
                htmlCliente(codigo, nombre, pedido, ahora));
        return codigo;
    }

    /** Ventana deslizante en memoria. Una sola instancia en Railway: alcanza sin guardar nada. */
    private boolean admitir(String clave, Instant ahora) {
        Deque<Instant> marcas = recientes.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && marcas.peekFirst().isBefore(ahora.minus(VENTANA))) marcas.pollFirst();
            if (marcas.size() >= MAXIMO_POR_HORA) return false;
            marcas.addLast(ahora);
            return true;
        }
    }

    private String sufijo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(LETRAS.charAt(random.nextInt(LETRAS.length())));
        return sb.toString();
    }

    private static String limpio(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String htmlAdmin(String codigo, String nombre, String email, String telefono, String pedido,
                                    String detalle, Instant cuando) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;color:#16181d;max-width:600px\">"
                + "<h2 style=\"margin-bottom:4px\">Solicitud de arrepentimiento</h2>"
                + "<p style=\"color:#b42318;margin-top:0\"><strong>Hay que responderle al cliente dentro de las 24 h.</strong></p>"
                + "<p><strong>Código:</strong> " + codigo + "<br><strong>Recibida:</strong> " + FECHA_HORA.format(cuando) + " h</p>"
                + "<p><strong>Nombre:</strong> " + esc(nombre) + "<br><strong>Email:</strong> " + esc(email)
                + "<br><strong>Teléfono:</strong> " + (telefono != null ? esc(telefono) : "—")
                + "<br><strong>Pedido:</strong> " + (pedido != null ? esc(pedido) : "—") + "</p>"
                + (detalle != null ? "<p><strong>Detalle:</strong><br>" + esc(detalle).replace("\n", "<br>") + "</p>" : "")
                + "</div>";
    }

    private static String htmlCliente(String codigo, String nombre, String pedido, Instant cuando) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;color:#16181d;max-width:600px\">"
                + "<h2 style=\"margin-bottom:4px\">Recibimos tu solicitud</h2>"
                + "<p>Hola " + esc(nombre) + ", registramos tu pedido de revocación de compra"
                + (pedido != null ? " del pedido <strong>" + esc(pedido) + "</strong>" : "") + ".</p>"
                + "<p style=\"font-size:18px\">Tu código de trámite es <strong>" + codigo + "</strong></p>"
                + "<p>Lo recibimos el " + FECHA_HORA.format(cuando) + " h. Te vamos a contactar dentro de las 24 horas para "
                + "coordinar la devolución del producto y el reintegro. Guardá este código para cualquier consulta.</p>"
                + "<p style=\"color:#555;font-size:14px\">El producto tiene que estar sin uso y con su embalaje original. "
                + "Los costos de la devolución corren por nuestra cuenta.</p>"
                + "<p style=\"color:#888;font-size:12px\">FuturaTecno · Tu tecnología. Tu futuro.</p></div>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
