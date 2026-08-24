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

    public PedidoService(PedidoRepository pedidoRepository,
                         VarianteRepository varianteRepository,
                         UsuarioRepository usuarioRepository,
                         CotizacionService cotizacionService,
                         PrecioService precioService,
                         PedidoEmailService pedidoEmailService,
                         EnvioService envioService) {
        this.pedidoRepository = pedidoRepository;
        this.varianteRepository = varianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.cotizacionService = cotizacionService;
        this.precioService = precioService;
        this.pedidoEmailService = pedidoEmailService;
        this.envioService = envioService;
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

        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalArs = BigDecimal.ZERO;

        for (ItemPedidoRequest itemReq : req.getItems()) {
            if (itemReq.getVarianteId() == null) {
                throw new IllegalArgumentException("Hay un artículo sin identificar en el pedido.");
            }
            int cantidad = itemReq.getCantidad() != null ? itemReq.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de cada artículo debe ser mayor a cero.");
            }

            Variante variante = varianteRepository.findById(itemReq.getVarianteId())
                    .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Un artículo del pedido ya no está disponible. Revisá el carrito."));

            Producto producto = variante.getProducto();
            if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
                throw new IllegalArgumentException(
                        "Un artículo del pedido ya no está disponible. Revisá el carrito.");
            }

            // Precio recalculado acá, ignorando cualquier importe que venga del cliente.
            BigDecimal precioUsd = precioService.precioVentaUsd(variante, producto.getProveedor());
            BigDecimal precioArs = precioService.aArs(precioUsd, cotizacion);

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
            item.setPrecioUnitarioArs(precioArs);
            pedido.getItems().add(item);

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
        logger.info("Pedido {} creado por {} ({} ítems, US$ {})",
                guardado.getNumero(), usuario.getEmail(), guardado.getItems().size(), guardado.getTotalUsd());

        // Los mails son asíncronos: si el proveedor de mail está caído, el pedido ya quedó guardado.
        pedidoEmailService.notificarPedidoNuevo(guardado);

        return toDTO(guardado, false);
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
        List<Pedido> pedidos = (estado == null)
                ? pedidoRepository.findAllByOrderByCreatedAtDesc()
                : pedidoRepository.findByEstadoOrderByCreatedAtDesc(estado);
        List<PedidoDTO> out = new ArrayList<>();
        for (Pedido p : pedidos) {
            out.add(toDTO(p, true));
        }
        return out;
    }

    @Transactional
    public PedidoDTO cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        pedido.setEstado(nuevoEstado);
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
        if (paraAdmin && p.getUsuario() != null) {
            dto.setUsuarioEmail(p.getUsuario().getEmail());
        }

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
            items.add(idto);
        }
        dto.setItems(items);
        return dto;
    }
}
