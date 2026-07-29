package com.futuratecno.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vencimiento diario de los pedidos que quedaron sin cerrar.
 *
 * <p>Corre a la misma hora que {@link SincronizacionScheduler} (06:30 AR) porque ese es el momento
 * en que la sincronización con los mayoristas pisa precios y stock: a partir de ahí, lo que el
 * cliente aceptó ya no es lo que hay. Es un scheduler aparte a propósito — apagar la sync con
 * SYNC_ENABLED=false no debe dejar pedidos vivos con precios viejos.
 */
@Component
public class PedidoScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PedidoScheduler.class);

    private final PedidoService pedidoService;

    public PedidoScheduler(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Segundo Minuto Hora DíaMes Mes DíaSemana — default: 06:30 todos los días.
    @Scheduled(cron = "${pedidos.vencimiento-cron:0 30 6 * * *}", zone = "America/Argentina/Buenos_Aires")
    public void vencerPedidos() {
        try {
            int vencidos = pedidoService.vencerPendientes();
            if (vencidos > 0) {
                logger.info("Corte diario: {} pedido(s) vencidos.", vencidos);
            }
        } catch (Exception e) {
            logger.error("Falló el vencimiento de pedidos: {}", e.toString());
        }
    }
}
