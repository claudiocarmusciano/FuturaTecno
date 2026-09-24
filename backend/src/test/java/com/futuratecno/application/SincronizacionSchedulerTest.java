package com.futuratecno.application;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * El rate limit de Invid (50 consultas/hora) costaba el día entero: el error se logueaba y no se
 * reintentaba hasta la mañana siguiente. Dos días así y sus ~1.200 productos cruzan el umbral de
 * Depurar catálogo como si el mayorista los hubiera discontinuado.
 */
class SincronizacionSchedulerTest {

    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
    }

    private final ElitImportService elit = stub(ElitImportService.class);
    private final InvidImportService invid = stub(InvidImportService.class);
    private final TaskScheduler taskScheduler = stub(TaskScheduler.class);

    private SincronizacionScheduler scheduler() {
        var s = new SincronizacionScheduler(elit, invid, taskScheduler);
        ReflectionTestUtils.setField(s, "habilitado", true);
        when(elit.estaConfigurado()).thenReturn(false);
        when(invid.estaConfigurado()).thenReturn(true);
        return s;
    }

    private long minutosHasta(Instant cuando) {
        return Duration.between(Instant.now(), cuando).toMinutes();
    }

    /** Reprograma respetando los minutos que pide la propia API, no un valor fijo nuestro. */
    @Test void reprogramaConLaEsperaQuePidioInvid() {
        when(invid.sincronizar()).thenThrow(
                new InvidRateLimitException("Invid limitó las consultas.", Duration.ofMinutes(29)));
        var captor = org.mockito.ArgumentCaptor.forClass(Instant.class);

        scheduler().sincronizarDiario();

        verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
        long minutos = minutosHasta(captor.getValue());
        assertTrue(minutos >= 29 && minutos <= 31, "esperaba ~30 min (29 + margen), fue " + minutos);
    }

    /** Sin Retry-After hay que asumir algo razonable, no reintentar al instante. */
    @Test void sinRetryAfterUsaLaEsperaPorDefecto() {
        when(invid.sincronizar()).thenThrow(
                new InvidRateLimitException("Invid limitó las consultas.", null));
        var captor = org.mockito.ArgumentCaptor.forClass(Instant.class);

        scheduler().sincronizarDiario();

        verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
        assertTrue(minutosHasta(captor.getValue()) >= 29, "no puede reintentar enseguida");
    }

    /** Si pidiera esperar medio día, no vale la pena quedarse colgado de eso. */
    @Test void recortaUnaEsperaDesproporcionada() {
        when(invid.sincronizar()).thenThrow(
                new InvidRateLimitException("Invid limitó las consultas.", Duration.ofHours(12)));
        var captor = org.mockito.ArgumentCaptor.forClass(Instant.class);

        scheduler().sincronizarDiario();

        verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
        assertTrue(minutosHasta(captor.getValue()) <= 121, "debería recortarse a 2 h + margen");
    }

    /** Reintentar para siempre sería peor que rendirse: mañana vuelve a correr sola. */
    @Test void dejaDeReintentarDespuesDelTope() {
        when(invid.sincronizar()).thenThrow(
                new InvidRateLimitException("Invid limitó las consultas.", Duration.ofMinutes(1)));
        // Ejecuta en el acto lo que se le programe, para simular los reintentos encadenados.
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        });

        scheduler().sincronizarDiario();

        // 6 intentos en total: el original y cinco reprogramados.
        verify(invid, times(6)).sincronizar();
        verify(taskScheduler, times(5)).schedule(any(Runnable.class), any(Instant.class));
    }

    /** Un error que no es de rate limit no se reintenta: no se sabe cuánto esperar. */
    @Test void noReintentaOtrosErrores() {
        when(invid.sincronizar()).thenThrow(new IllegalStateException("Invid no está configurada."));

        scheduler().sincronizarDiario();

        verifyNoInteractions(taskScheduler);
    }

    /** El camino feliz sigue igual: una corrida, sin reprogramar nada. */
    @Test void cuandoSincronizaBienNoProgramaNada() {
        when(invid.sincronizar()).thenReturn(Map.of("mensaje", "Sincronización de Invid: 1.200 productos actualizados."));

        scheduler().sincronizarDiario();

        verify(invid).sincronizar();
        verifyNoInteractions(taskScheduler);
    }
}
