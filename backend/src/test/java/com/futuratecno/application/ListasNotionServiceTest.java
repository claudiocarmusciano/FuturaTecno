package com.futuratecno.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ListasNotionServiceTest {

    private static final String URL = "https://n8n.example/webhook/secreto-largo";

    @Test void sinUrlConfiguradaAvisaQueFaltaYNoLlamaANadie() {
        var e = assertThrows(ListasNotionService.NoDisponibleException.class,
                () -> new ListasNotionService(new RestTemplate(), "  ").procesar("admin"));
        assertFalse(e.isConfigurado());
        assertTrue(e.getMessage().contains("N8N_PROCESAR_LISTAS_URL"));
    }

    @Test void disparaElWebhookConUnPost() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess());
        new ListasNotionService(rt, URL).procesar("admin@x");
        server.verify();
    }

    /** Si n8n falla, el admin recibe un motivo y la URL (el secreto) no viaja en el mensaje. */
    @Test void siN8nFallaDaUnMotivoSinExponerLaUrl() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo(URL)).andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));
        var e = assertThrows(ListasNotionService.NoDisponibleException.class, () -> new ListasNotionService(rt, URL).procesar("admin"));
        assertTrue(e.isConfigurado());
        assertFalse(e.getMessage().contains("secreto-largo"));
    }

    @Test void siN8nNoRespondeTambienDaUnMotivo() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo(URL)).andRespond(withException(new java.net.SocketTimeoutException("Read timed out")));
        var e = assertThrows(ListasNotionService.NoDisponibleException.class, () -> new ListasNotionService(rt, URL).procesar("admin"));
        assertTrue(e.getMessage().contains("n8n no respondió"));
    }

    /** Reloj que se puede adelantar a mano. */
    private static final class Reloj extends java.time.Clock {
        java.time.Instant ahora = java.time.Instant.parse("2026-09-26T00:00:00Z");
        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId z) { return this; }
        @Override public java.time.Instant instant() { return ahora; }
    }

    @Test void elEstadoSigueElPedidoYElAvisoDeN8n() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo(URL)).andRespond(withSuccess());
        var reloj = new Reloj();
        var svc = new ListasNotionService(rt, URL, reloj);

        assertEquals("sin_datos", svc.estado().get("estado"));
        svc.procesar("admin");
        assertEquals("procesando", svc.estado().get("estado"));

        var resumen = new java.util.LinkedHashMap<String, Object>();
        resumen.put("listas", 3);
        resumen.put("motivo", null);   // n8n manda nulls: no tiene que romper
        reloj.ahora = reloj.ahora.plusSeconds(26);
        svc.registrarResultado(resumen);
        var estado = svc.estado();
        assertEquals("terminado", estado.get("estado"));
        assertEquals(3, ((java.util.Map<?, ?>) estado.get("resultado")).get("listas"));
    }

    @Test void siN8nNoAvisaEnDiezMinutosQuedaSinRespuesta() {
        RestTemplate rt = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo(URL)).andRespond(withSuccess());
        var reloj = new Reloj();
        var svc = new ListasNotionService(rt, URL, reloj);
        svc.procesar("admin");
        reloj.ahora = reloj.ahora.plus(ListasNotionService.SIN_RESPUESTA).plusSeconds(1);
        assertEquals("sin_respuesta", svc.estado().get("estado"));
    }
}
