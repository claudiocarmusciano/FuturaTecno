package com.futuratecno.application;

import com.futuratecno.api.dto.CrearPedidoRequest;
import com.futuratecno.api.dto.ItemPedidoRequest;
import com.futuratecno.api.dto.PedidoDTO;
import com.futuratecno.api.dto.PedidoItemDTO;
import com.futuratecno.domain.*;
import com.futuratecno.infrastructure.PedidoRepository;
import com.futuratecno.infrastructure.UsuarioRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Alta y consulta de pedidos.
 *
 * <p>Dos reglas que no hay que aflojar:
 * <ul>
 *   <li><b>El precio lo pone el backend.</b> El carrito manda variante y cantidad, nunca importes:
 *       si se confiara en el precio del cliente, cualquiera podría pedir un iPhone a un dólar.</li>
 *   <li><b>El precio se congela.</b> Lo calculado se copia al ítem del pedido, porque el precio
 *       de venta se recalcula a diario con la cotización y los costos del mayorista.</li>
 * </ul>
 */
@Service
public class PedidoService {
    private static final Logger logger = LoggerFactory.getLogger(PedidoService.class);
    /** Monto mínimo del subtotal de artículos para poder confirmar un pedido. */
    private static final BigDecimal MONTO_MINIMO_PEDIDO_USD = new BigDecimal("250");

    /** Los cortes horarios del negocio son en hora argentina, no en la del servidor. */
    public static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

    /** Hora del corte diario: la misma a la que la sync pisa precios y stock (SincronizacionScheduler). */
    private static final int CORTE_HORA = 6;
    private static final int CORTE_MINUTO = 30;

    private final PedidoRepository pedidoRepository;
    private final VarianteRepository varianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionService cotizacionService;
    private final PrecioService precioService;
    private final PedidoEmailService pedidoEmailService;
    private final EnvioService envioService;
    private final PuntosService puntosService;

    public PedidoService(PedidoRepository pedidoRepository,
                         VarianteRepository varianteRepository,
                         UsuarioRepository usuarioRepository,
                         CotizacionService cotizacionService,
                         PrecioService precioService,
                         PedidoEmailService pedidoEmailService,
                         EnvioService envioService, PuntosService puntosService) {
        this.pedidoRepository = pedidoRepository;
        this.varianteRepository = varianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.cotizacionService = cotizacionService;
        this.precioService = precioService;
        this.pedidoEmailService = pedidoEmailService;
        this.envioService = envioService;
        this.puntosService = puntosService;
    }

    /**
     * Próximo corte de las 06:30 AR a partir de ahora. Se devuelve como LocalDateTime en el huso de
     * la JVM (el mismo que usa LocalDateTime.now() en el resto del código) para que las
     * comparaciones de vencimiento funcionen corra donde corra el servidor.
     */
    public LocalDateTime proximoCorte() {
        ZonedDateTime ahoraAr = ZonedDateTime.now(ZONA_AR);
        ZonedDateTime corte = ahoraAr.toLocalDate().atTime(CORTE_HORA, CORTE_MINUTO).atZone(ZONA_AR);
        if (!corte.isAfter(ahoraAr)) {
            corte = corte.plusDays(1);
        }
        return corte.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Transactional
    public PedidoDTO crear(CrearPedidoRequest req, String emailUsuario) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido no tiene artículos.");
        }
        if (!Boolean.TRUE.equals(req.getAceptaCompromiso())) {
            throw new IllegalArgumentException(
                    "Tenés que aceptar que confirmar el pedido implica un compromiso de compra.");
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailUsuario)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setCotizacionUsada(cotizacion);
        pedido.setVenceEn(proximoCorte());
        pedido.setNombreContacto(limpiar(req.getNombreContacto(), 150));
        pedido.setTelefonoContacto(limpiar(req.getTelefonoContacto(), 50));
        pedido.setNotas(limpiar(req.getNotas(), 1000));
        String medioPago = limpiar(req.getMedioPago(), 30);
        if (medioPago == null) medioPago = "MERCADO_PAGO";
        if (!medioPago.equals("MERCADO_PAGO") && !medioPago.equals("TRANSFERENCIA") && !medioPago.equals("EFECTIVO")) {
            throw new IllegalArgumentException("La forma de pago elegida no es válida.");
        }
        pedido.setMedioPago(medioPago);

        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalArs = BigDecimal.ZERO;

        for (ItemPedidoRequest itemReq : req.getItems()) {
            PedidoItem item = armarItem(pedido, itemReq.getVarianteId(), itemReq.getCantidad(), cotizacion, null);

            totalUsd = totalUsd.add(item.subtotalUsd());
            totalArs = totalArs.add(item.subtotalArs());
        }

        pedido.setTotalUsd(totalUsd);
        pedido.setTotalArs(totalArs);
        if (totalUsd.compareTo(MONTO_MINIMO_PEDIDO_USD) < 0) {
            throw new IllegalArgumentException("El pedido mínimo es de US$ 250. Agregá productos por US$ "
                    + MONTO_MINIMO_PEDIDO_USD.stripTrailingZeros().toPlainString() + " o más para continuar.");
        }
        pedido.setNumero(generarNumero());

        // Envío: si el cliente eligió una modalidad, el costo se recotiza ACÁ y se congela —
        // nunca el importe que haya visto el frontend. Si Andreani no responde en este momento,
        // el pedido igual sale: queda la modalidad con costo null ("a cotizar", se coordina).
        String modoEnvio = limpiar(req.getModoEnvio(), 30);
        String cpDestino = limpiar(req.getCpDestino(), 10);
        if (modoEnvio != null && cpDestino != null) {
            pedido.setCpDestino(cpDestino);
            pedido.setModoEnvio(modoEnvio);
            pedido.setCostoEnvioArs(envioService.costoDeModalidad(cpDestino, modoEnvio, req.getItems()));
        }

        Pedido guardado = pedidoRepository.save(pedido);
        int puntosSolicitados = req.getPuntosUsar() == null ? 0 : req.getPuntosUsar();
        if (puntosSolicitados < 0) throw new IllegalArgumentException("La cantidad de puntos no es válida.");
        if (puntosSolicitados > 0) {
            BigDecimal baseProductos = "EFECTIVO".equals(medioPago)
                    ? precioService.precioContadoEfectivo(totalArs) : totalArs;
            int maximoCanjeable = baseProductos.divide(cotizacion, 0, java.math.RoundingMode.DOWN).intValue();
            if (puntosSolicitados > maximoCanjeable) {
                throw new IllegalArgumentException("No podés usar más puntos que el valor de los productos.");
            }
            puntosService.reservar(guardado, puntosSolicitados);
            guardado.setPuntosCanjeados(puntosSolicitados);
            guardado.setDescuentoPuntosArs(cotizacion.multiply(BigDecimal.valueOf(puntosSolicitados)));
            pedidoRepository.save(guardado);
        }
        logger.info("Pedido {} creado por {} ({} ítems, US$ {})",
                guardado.getNumero(), usuario.getEmail(), guardado.getItems().size(), guardado.getTotalUsd());

        // Los mails son asíncronos: si el proveedor de mail está caído, el pedido ya quedó guardado.
        pedidoEmailService.notificarPedidoNuevo(guardado);

        return toDTO(guardado, false);
    }

    /**
     * Arma y agrega un renglón con los datos del artículo CONGELADOS. El precio es el del catálogo
     * (PrecioService); solo una orden manual puede pasar {@code precioAcordadoUsd}, y en ese caso
     * se guarda también el del catálogo para dejar registro del cambio.
     */
    private PedidoItem armarItem(Pedido pedido, Long varianteId, Integer cantidadPedida, BigDecimal cotizacion,
                                 BigDecimal precioAcordadoUsd) {
        if (varianteId == null) {
            throw new IllegalArgumentException("Hay un artículo sin identificar en el pedido.");
        }
        int cantidad = cantidadPedida != null ? cantidadPedida : 0;
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de cada artículo debe ser mayor a cero.");
        }
        Variante variante = varianteRepository.findById(varianteId)
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Un artículo del pedido ya no está disponible. Revisá el carrito."));
        Producto producto = variante.getProducto();
        if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
            throw new IllegalArgumentException(
                    "Un artículo del pedido ya no está disponible. Revisá el carrito.");
        }

        BigDecimal precioCatalogoUsd = precioService.precioVentaUsd(variante, producto, producto.getProveedor());
        BigDecimal precioUsd = precioCatalogoUsd;
        if (precioAcordadoUsd != null) {
            if (precioAcordadoUsd.signum() <= 0) throw new IllegalArgumentException("El precio de cada artículo tiene que ser mayor a cero.");
            precioUsd = precioAcordadoUsd.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setVariante(variante);
        item.setProductoNombre(nombreDe(producto));
        item.setEspecificaciones(limpiar(variante.getEspecificaciones(), 500));
        item.setSku(producto.skuCamuflado());
        item.setImagenUrl(producto.getImagenUrl());
        item.setCantidad(cantidad);
        item.setPrecioUnitarioUsd(precioUsd);
        item.setPrecioUnitarioArs(precioService.aArs(precioUsd, cotizacion));
        if (precioCatalogoUsd != null && precioUsd.compareTo(precioCatalogoUsd) != 0) item.setPrecioCatalogoUsd(precioCatalogoUsd);
        if (producto.getProveedor() != null) {
            item.setDemoraEntregaMinDias(producto.getProveedor().getDemoraEntregaMinDias());
            item.setDemoraEntregaMaxDias(producto.getProveedor().getDemoraEntregaMaxDias());
        }
        pedido.getItems().add(item);
        return item;
    }

    /**
     * Orden de venta cargada por el admin. Diferencias con la web, todas a propósito: puede no
     * tener cuenta (quedan nombre, teléfono y email), el precio de cada artículo se puede cambiar,
     * no exige la compra mínima (es para ventas en el local o acordadas por WhatsApp) y no vence a
     * las 06:30 (el precio lo acordó una persona). No se le manda mail a nadie: el comprobante lo
     * comparte el admin.
     */
    @Transactional
    public PedidoDTO crearManual(com.futuratecno.api.dto.CrearPedidoManualRequest req, String emailAdmin) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("La orden no tiene artículos.");
        }
        String nombre = limpiar(req.getNombre(), 150);
        if (nombre == null) throw new IllegalArgumentException("Completá el nombre del cliente.");
        String email = limpiar(req.getEmail(), 190);
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) {
            throw new IllegalArgumentException("El email del cliente no es válido.");
        }
        String medioPago = limpiar(req.getMedioPago(), 30);
        if (medioPago == null) medioPago = "TRANSFERENCIA";
        if (!medioPago.equals("MERCADO_PAGO") && !medioPago.equals("TRANSFERENCIA") && !medioPago.equals("EFECTIVO")) {
            throw new IllegalArgumentException("La forma de pago elegida no es válida.");
        }

        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
        Pedido pedido = new Pedido();
        pedido.setOrigen("MANUAL");
        if (email != null) {
            usuarioRepository.findByEmailIgnoreCase(email).filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .ifPresent(pedido::setUsuario);
        }
        pedido.setEmailContacto(email);
        pedido.setNombreContacto(nombre);
        pedido.setTelefonoContacto(limpiar(req.getTelefono(), 50));
        pedido.setNotas(limpiar(req.getNotas(), 1000));
        pedido.setMedioPago(medioPago);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setCotizacionUsada(cotizacion);
        pedido.setVenceEn(null);

        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalArs = BigDecimal.ZERO;
        List<ItemPedidoRequest> paraEnvio = new ArrayList<>();
        for (var it : req.getItems()) {
            PedidoItem item = armarItem(pedido, it.getVarianteId(), it.getCantidad(), cotizacion, it.getPrecioUnitarioUsd());
            totalUsd = totalUsd.add(item.subtotalUsd());
            totalArs = totalArs.add(item.subtotalArs());
            ItemPedidoRequest ir = new ItemPedidoRequest();
            ir.setVarianteId(it.getVarianteId());
            ir.setCantidad(it.getCantidad());
            paraEnvio.add(ir);
        }
        pedido.setTotalUsd(totalUsd);
        pedido.setTotalArs(totalArs);

        String modoEnvio = limpiar(req.getModoEnvio(), 30);
        String cpDestino = limpiar(req.getCpDestino(), 10);
        if (modoEnvio != null) {
            pedido.setModoEnvio(modoEnvio);
            pedido.setCpDestino(cpDestino);
            if (req.getCostoEnvioArs() != null) {
                if (req.getCostoEnvioArs().signum() < 0) throw new IllegalArgumentException("El costo de envío no puede ser negativo.");
                pedido.setCostoEnvioArs(req.getCostoEnvioArs().setScale(2, java.math.RoundingMode.HALF_UP));
            } else if (cpDestino != null) {
                pedido.setCostoEnvioArs(envioService.costoDeModalidad(cpDestino, modoEnvio, paraEnvio));
            }
        }
        pedido.setNumero(generarNumero());
        Pedido guardado = pedidoRepository.save(pedido);
        logger.info("Orden manual {} cargada por {} ({} ítems, US$ {}, cliente {})", guardado.getNumero(), emailAdmin,
                guardado.getItems().size(), guardado.getTotalUsd(), guardado.getUsuario() != null ? "con cuenta" : "sin cuenta");
        return toDTO(guardado, true);
    }

    /** Un pedido por número, para el admin (comprobante). */
    @Transactional(readOnly = true)
    public PedidoDTO obtenerParaAdmin(String numero) {
        return toDTO(pedidoRepository.findByNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado.")), true);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> misPedidos(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        List<PedidoDTO> out = new ArrayList<>();
        for (Pedido p : pedidoRepository.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId())) {
            out.add(toDTO(p, false));
        }
        return out;
    }

    /** Detalle de un pedido propio. Falla si el pedido es de otro usuario. */
    @Transactional(readOnly = true)
    public PedidoDTO obtenerPropio(String numero, String emailUsuario) {
        Pedido pedido = pedidoRepository.findByNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        if (pedido.getUsuario() == null
                || !pedido.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
            // Mismo mensaje que "no existe": no confirma la existencia de pedidos ajenos.
            throw new IllegalArgumentException("Pedido no encontrado.");
        }
        return toDTO(pedido, false);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarParaAdmin(EstadoPedido estado) {
        return listarParaAdmin(estado, null, null, null, null, null);
    }

    /**
     * Bandeja del admin con búsqueda. Filtra en memoria: son pocos pedidos y así el texto libre
     * busca también en los renglones (nombre del artículo, SKU) sin armar una consulta aparte.
     *
     * @param texto número, nombre, teléfono, email o artículo; sin distinguir mayúsculas ni tildes
     * @param desde / hasta fechas de creación, inclusive (hora argentina)
     */
    @Transactional(readOnly = true)
    public List<PedidoDTO> listarParaAdmin(EstadoPedido estado, String texto, java.time.LocalDate desde,
                                           java.time.LocalDate hasta, String medioPago, String origen) {
        List<Pedido> pedidos = (estado == null)
                ? pedidoRepository.findAllByOrderByCreatedAtDesc()
                : pedidoRepository.findByEstadoOrderByCreatedAtDesc(estado);
        String q = texto == null || texto.isBlank() ? null : sinTildes(texto);
        List<PedidoDTO> out = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (medioPago != null && !medioPago.isBlank() && !medioPago.equalsIgnoreCase(p.getMedioPago())) continue;
            if (origen != null && !origen.isBlank() && !origen.equalsIgnoreCase(p.getOrigen())) continue;
            if (desde != null || hasta != null) {
                if (p.getCreatedAt() == null) continue;
                java.time.LocalDate dia = p.getCreatedAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZONA_AR).toLocalDate();
                if (desde != null && dia.isBefore(desde)) continue;
                if (hasta != null && dia.isAfter(hasta)) continue;
            }
            if (q != null && !sinTildes(textoBuscable(p)).contains(q)) continue;
            out.add(toDTO(p, true));
        }
        return out;
    }

    private static String textoBuscable(Pedido p) {
        StringBuilder sb = new StringBuilder();
        for (String s : new String[]{p.getNumero(), p.getNombreContacto(), p.getTelefonoContacto(), p.getEmailContacto(),
                p.getUsuario() != null ? p.getUsuario().getEmail() : null, p.getNotas()}) {
            if (s != null) sb.append(s).append(' ');
        }
        for (PedidoItem i : p.getItems()) sb.append(i.getProductoNombre()).append(' ').append(i.getSku() != null ? i.getSku() : "").append(' ');
        // El teléfono se busca también sin espacios ni guiones: "2284 381111" = "2284381111".
        return sb + " " + sb.toString().replaceAll("[\\s-]", "");
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).trim();
    }

    @Transactional
    public PedidoDTO cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        pedido.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoPedido.CANCELADO || nuevoEstado == EstadoPedido.VENCIDO) puntosService.revertirReserva(pedido);
        return toDTO(pedidoRepository.save(pedido), true);
    }

    @Transactional
    public PedidoDTO cambiarEstadoPagoManual(Long id, EstadoPago nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        // En la web el pago de Mercado Pago lo confirma el webhook. En una orden manual el cliente
        // paga por un link o QR que armó el admin, así que el cobro se marca a mano.
        if ("MERCADO_PAGO".equals(pedido.getMedioPago()) && !pedido.esManual()) {
            throw new IllegalArgumentException("Los pagos de Mercado Pago se actualizan automáticamente.");
        }
        pedido.setEstadoPago(nuevoEstado);
        if (nuevoEstado == EstadoPago.APROBADO) {
            pedido.setPagadoEn(LocalDateTime.now());
            if (pedido.getEstado() != EstadoPedido.ENTREGADO) pedido.setEstado(EstadoPedido.CONFIRMADO);
            puntosService.procesarPagoAprobado(pedido);
        }
        if (nuevoEstado == EstadoPago.CANCELADO || nuevoEstado == EstadoPago.RECHAZADO) puntosService.revertirReserva(pedido);
        return toDTO(pedidoRepository.save(pedido), true);
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return pedidoRepository.countByEstado(EstadoPedido.PENDIENTE);
    }

    /**
     * Marca VENCIDO todo pedido pendiente cuyo corte ya pasó. Lo llama el scheduler junto con la
     * sincronización de las 06:30, que es la que deja obsoletos los precios y el stock.
     */
    @Transactional
    public int vencerPendientes() {
        List<Pedido> vencidos = pedidoRepository.findByEstadoAndVenceEnBefore(
                EstadoPedido.PENDIENTE, LocalDateTime.now());
        for (Pedido p : vencidos) {
            p.setEstado(EstadoPedido.VENCIDO);
            puntosService.revertirReserva(p);
        }
        if (!vencidos.isEmpty()) {
            pedidoRepository.saveAll(vencidos);
            logger.info("Vencimiento de pedidos: {} pedido(s) pasaron a VENCIDO.", vencidos.size());
        }
        return vencidos.size();
    }

    private String generarNumero() {
        Long n = pedidoRepository.siguienteNumero();
        return String.format("FT-%06d", n);
    }

    private String nombreDe(Producto p) {
        String nombre = ((p.getMarca() != null ? p.getMarca() : "") + " "
                + (p.getModelo() != null ? p.getModelo() : "")).trim();
        return nombre.isEmpty() ? "Artículo" : recortar(nombre, 300);
    }

    private String limpiar(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : recortar(t, max);
    }

    private String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private PedidoDTO toDTO(Pedido p, boolean paraAdmin) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(p.getId());
        dto.setNumero(p.getNumero());
        dto.setEstado(p.getEstado() != null ? p.getEstado().name() : null);
        dto.setTotalUsd(p.getTotalUsd());
        dto.setTotalArs(p.getTotalArs());
        dto.setCotizacionUsada(p.getCotizacionUsada());
        dto.setNombreContacto(p.getNombreContacto());
        dto.setTelefonoContacto(p.getTelefonoContacto());
        dto.setNotas(p.getNotas());
        dto.setVenceEn(p.getVenceEn());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setCpDestino(p.getCpDestino());
        dto.setModoEnvio(p.getModoEnvio());
        dto.setCostoEnvioArs(p.getCostoEnvioArs());
        dto.setMedioPago(p.getMedioPago());
        BigDecimal envio = p.getCostoEnvioArs() != null ? p.getCostoEnvioArs() : BigDecimal.ZERO;
        BigDecimal descuentoPuntos = p.getDescuentoPuntosArs() != null ? p.getDescuentoPuntosArs() : BigDecimal.ZERO;
        BigDecimal productos = "EFECTIVO".equals(p.getMedioPago())
                ? precioService.precioContadoEfectivo(p.getTotalArs()) : p.getTotalArs();
        BigDecimal baseConEnvio = productos.subtract(descuentoPuntos).max(BigDecimal.ZERO).add(envio);
        dto.setTotalCobroArs("MERCADO_PAGO".equals(p.getMedioPago())
                ? (p.getMontoPagoArs() != null ? p.getMontoPagoArs()
                : precioService.precioMercadoPagoInmediato(baseConEnvio))
                : baseConEnvio);
        dto.setEstadoPago(p.getEstadoPago() != null ? p.getEstadoPago().name() : null);
        dto.setMercadoPagoPaymentId(p.getMercadoPagoPaymentId());
        dto.setMercadoPagoStatusDetail(p.getMercadoPagoStatusDetail());
        dto.setPagadoEn(p.getPagadoEn());
        dto.setPuntosCanjeados(p.getPuntosCanjeados());
        dto.setDescuentoPuntosArs(p.getDescuentoPuntosArs());
        if (paraAdmin && p.getUsuario() != null) {
            dto.setUsuarioEmail(p.getUsuario().getEmail());
        }
        dto.setOrigen(p.getOrigen());
        if (paraAdmin) dto.setEmailContacto(p.getEmailContacto());

        List<PedidoItemDTO> items = new ArrayList<>();
        for (PedidoItem i : p.getItems()) {
            PedidoItemDTO idto = new PedidoItemDTO();
            idto.setId(i.getId());
            idto.setProductoId(i.getProducto() != null ? i.getProducto().getId() : null);
            idto.setVarianteId(i.getVariante() != null ? i.getVariante().getId() : null);
            idto.setProductoNombre(i.getProductoNombre());
            idto.setEspecificaciones(i.getEspecificaciones());
            idto.setSku(i.getSku());
            idto.setImagenUrl(i.getImagenUrl());
            idto.setCantidad(i.getCantidad());
            idto.setPrecioUnitarioUsd(i.getPrecioUnitarioUsd());
            idto.setPrecioUnitarioArs(i.getPrecioUnitarioArs());
            idto.setSubtotalUsd(i.subtotalUsd());
            idto.setSubtotalArs(i.subtotalArs());
            idto.setDemoraEntregaMinDias(i.getDemoraEntregaMinDias());
            idto.setDemoraEntregaMaxDias(i.getDemoraEntregaMaxDias());
            if (paraAdmin) idto.setPrecioCatalogoUsd(i.getPrecioCatalogoUsd());
            items.add(idto);
        }
        dto.setItems(items);
        return dto;
    }
}
