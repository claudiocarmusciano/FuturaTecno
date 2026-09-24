package com.futuratecno.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Sincronización automática diaria de los catálogos de los mayoristas (Elit + Invid).
 * SOLO actualiza precio/stock/imagen de los productos ya importados; no agrega nuevos.
 * Se ejecuta según {@code sync.cron} (default 06:30, hora Argentina). Se puede desactivar
 * con {@code SYNC_ENABLED=false}. Solo corre para el mayorista que esté configurado.
 */
@Component
public class SincronizacionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SincronizacionScheduler.class);

    /** Invid limita a 50 consultas/hora y su catálogo entero necesita más de una tanda. */
    // Cada intento avanza hasta 50 páginas (la cuota horaria) porque InvidApiClient retoma donde
    // cortó el anterior: 6 intentos cubren las 300 páginas de su TOPE_PAGINAS.
    private static final int MAX_INTENTOS_INVID = 6;
    /** Si Invid no manda Retry-After, media hora es lo que tarda en liberarse su ventana. */
    private static final Duration ESPERA_POR_DEFECTO = Duration.ofMinutes(30);
    /** Tope de cordura: si pidiera esperar medio día, no vale la pena seguir colgados de eso. */
    private static final Duration ESPERA_MAXIMA = Duration.ofHours(2);
    /** Un minuto de más para no volver a pegar justo en el borde de la ventana. */
    private static final Duration MARGEN = Duration.ofMinutes(1);

    private final ElitImportService elitImportService;
    private final InvidImportService invidImportService;
    private final TaskScheduler taskScheduler;

    @Value("${sync.enabled:true}")
    private boolean habilitado;

    public SincronizacionScheduler(ElitImportService elitImportService,
                                   InvidImportService invidImportService,
                                   TaskScheduler taskScheduler) {
        this.elitImportService = elitImportService;
        this.invidImportService = invidImportService;
        this.taskScheduler = taskScheduler;
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
            sincronizarInvid(1);
        }
    }

    /**
     * Invid corta por rate limit con bastante frecuencia y hasta ahora eso costaba el día entero:
     * el error se logueaba y no se reintentaba hasta la mañana siguiente. Dos días así y sus ~1.200
     * productos cruzan el umbral de Depurar catálogo como si el mayorista los hubiera discontinuado.
     *
     * <p>Se reprograma respetando los minutos que pide la propia API, en vez de dormir el hilo: una
     * espera de media hora bloqueando un hilo del pool es justo lo que este proyecto ya aprendió a
     * no hacer con los mails y con la generación de listados.
     */
    private void sincronizarInvid(int intento) {
        try {
            Object r = invidImportService.sincronizar().get("mensaje");
            logger.info("Sync automática Invid: {}", r);
        } catch (InvidRateLimitException e) {
            if (intento >= MAX_INTENTOS_INVID) {
                logger.error("Sync automática Invid: rate limit en el intento {} de {}. Se abandona "
                        + "hasta la corrida de mañana. {}", intento, MAX_INTENTOS_INVID, e.getMessage());
                return;
            }
            Duration espera = esperaDe(e);
            Instant cuando = Instant.now().plus(espera);
            logger.warn("Sync automática Invid: {} Reintento {} de {} programado en {} minuto(s).",
                    e.getMessage(), intento + 1, MAX_INTENTOS_INVID, espera.toMinutes());
            taskScheduler.schedule(() -> sincronizarInvid(intento + 1), cuando);
        } catch (Exception e) {
            logger.error("Sync automática Invid falló: {}", e.toString());
        }
    }

    private static Duration esperaDe(InvidRateLimitException e) {
        Duration pedida = e.getEspera() != null && !e.getEspera().isNegative() && !e.getEspera().isZero()
                ? e.getEspera() : ESPERA_POR_DEFECTO;
        return (pedida.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : pedida).plus(MARGEN);
    }
}
