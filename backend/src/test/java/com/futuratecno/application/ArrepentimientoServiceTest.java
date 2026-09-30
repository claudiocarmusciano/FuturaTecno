package com.futuratecno.application;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArrepentimientoServiceTest {

    private static class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-10-01T15:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final EmailService email = mock(EmailService.class);
    private final Reloj reloj = new Reloj();
    private final ArrepentimientoService svc = new ArrepentimientoService(email, new LimitadorIntentos(reloj), "admin@example.com", reloj);

    private static ArrepentimientoService.Solicitud sol(String mail) {
        return new ArrepentimientoService.Solicitud("Ana Pérez", mail, "2284 123456", "FT-12", "No era lo que esperaba <b>");
    }

    @Test
    void entregaUnCodigoYLeAvisaAlAdminYAlCliente() {
        String codigo = svc.solicitar(sol("ana@example.com"), "1.1.1.1");
        assertTrue(codigo.matches("ARR-20261001-[A-Z2-9]{5}"), codigo);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(email).enviarHtmlAsync(eq("admin@example.com"), anyString(), html.capture());
        assertTrue(html.getValue().contains(codigo) && html.getValue().contains("24 h"));
        assertTrue(html.getValue().contains("&lt;b&gt;"), "lo que escribe el cliente se escapa");
        verify(email).enviarHtmlAsync(eq("ana@example.com"), anyString(), anyString());
    }

    @Test
    void exigeNombreYEmailValido() {
        assertThrows(IllegalArgumentException.class, () -> svc.solicitar(
                new ArrepentimientoService.Solicitud(" ", "ana@example.com", null, null, null), "1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> svc.solicitar(sol("no-es-un-mail"), "1.1.1.1"));
    }

    @Test
    void frenaElAbusoPorIpYPorEmailYSeLiberaALaHora() {
        for (int i = 0; i < ArrepentimientoService.MAXIMO_POR_HORA; i++) svc.solicitar(sol("x" + i + "@example.com"), "2.2.2.2");
        assertThrows(ArrepentimientoService.DemasiadasSolicitudesException.class,
                () -> svc.solicitar(sol("otro@example.com"), "2.2.2.2"));

        for (int i = 0; i < ArrepentimientoService.MAXIMO_POR_HORA; i++) svc.solicitar(sol("mismo@example.com"), "3.3.3." + i);
        assertThrows(ArrepentimientoService.DemasiadasSolicitudesException.class,
                () -> svc.solicitar(sol("MISMO@example.com"), "9.9.9.9"));

        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(61));
        assertTrue(svc.solicitar(sol("otro@example.com"), "2.2.2.2").startsWith("ARR-"));
    }
}
