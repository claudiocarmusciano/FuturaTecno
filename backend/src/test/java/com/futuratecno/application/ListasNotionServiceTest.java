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
}
