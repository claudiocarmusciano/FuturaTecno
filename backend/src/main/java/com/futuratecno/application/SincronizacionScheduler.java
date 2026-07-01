package com.futuratecno.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sincronización automática diaria de los catálogos de los mayoristas (Elit + Invid).
 * SOLO actualiza precio/stock/imagen de los productos ya importados; no agrega nuevos.
 * Se ejecuta según {@code sync.cron} (default 06:30, hora Argentina). Se puede desactivar
 * con {@code SYNC_ENABLED=false}. Solo corre para el mayorista que esté configurado.
 */
@Component
public class SincronizacionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SincronizacionScheduler.class);

    private final ElitImportService elitImportService;
    private final InvidImportService invidImportService;

    @Value("${sync.enabled:true}")
    private boolean habilitado;

    public SincronizacionScheduler(ElitImportService elitImportService,
                                   InvidImportService invidImportService) {
        this.elitImportService = elitImportService;
        this.invidImportService = invidImportService;
    }

    // Segundo Minuto Hora DíaMes Mes DíaSemana — default: 06:30 todos los días.
    @Scheduled(cron = "${sync.cron:0 30 6 * * *}", zone = "America/Argentina/Buenos_Aires")
    public void sincronizarDiario() {
        if (!habilitado) return;

        if (elitImportService.estaConfigurado()) {
            try {
                Object r = elitImportService.sincronizar().get("mensaje");
                logger.info("Sync automática Elit: {}", r);
            } catch (Exception e) {
                logger.error("Sync automática Elit falló: {}", e.toString());
            }
        }

        if (invidImportService.estaConfigurado()) {
            try {
                Object r = invidImportService.sincronizar().get("mensaje");
                logger.info("Sync automática Invid: {}", r);
            } catch (Exception e) {
                logger.error("Sync automática Invid falló: {}", e.toString());
            }
        }
    }
}
