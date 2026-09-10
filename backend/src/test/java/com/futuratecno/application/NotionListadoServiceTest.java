package com.futuratecno.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NotionListadoServiceTest {
    final String id = "3d775555-124f-800f-b213-d54fd32096e2";
    final RestTemplate http = new RestTemplate();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    final NotionListadoService service = new NotionListadoService(new RestTemplateBuilder().additionalCustomizers(client -> client.setRequestFactory(http.getRequestFactory())), "test-token", id);
    String url(String block) { return "https://api.notion.com/v1/blocks/" + block + "/children?page_size=100"; }
    String paragraph(String text) { return "{\"type\":\"paragraph\",\"paragraph\":{\"rich_text\":[{\"plain_text\":\"" + text + "\"}]}}"; }
    String page(String blocks) { return "{\"results\":[" + blocks + "],\"has_more\":false}"; }

    @Test void leeTextoAnidadoYPaginadoEnOrden() {
        server.expect(requestTo(url(id))).andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer test-token"))
            .andRespond(withSuccess("{\"results\":[{\"id\":\"nested\",\"type\":\"toggle\",\"has_children\":true,\"toggle\":{\"rich_text\":[{\"plain_text\":\"Samsung\"}]}}],\"has_more\":true,\"next_cursor\":\"next\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(url("nested"))).andRespond(withSuccess(page(paragraph("S25 USD 490")), MediaType.APPLICATION_JSON));
        server.expect(requestTo(url(id) + "&start_cursor=next")).andRespond(withSuccess(page(paragraph("S26 USD 710")), MediaType.APPLICATION_JSON));
        assertEquals("Samsung\nS25 USD 490\nS26 USD 710", service.leer());
        server.verify();
    }
    @Test void rechazaPaginaVacia() {
        server.expect(requestTo(url(id))).andRespond(withSuccess(page(""), MediaType.APPLICATION_JSON));
        assertThrows(IllegalArgumentException.class, service::leer);
        server.verify();
    }
    @Test void rechazaListadoLargoSinTruncarlo() {
        server.expect(requestTo(url(id))).andRespond(withSuccess(page(paragraph("a".repeat(20001))), MediaType.APPLICATION_JSON));
        assertTrue(assertThrows(IllegalArgumentException.class, service::leer).getMessage().contains("20.000"));
    }
    @Test void explicaPermisosSinExponerRespuestaExterna() {
        server.expect(requestTo(url(id))).andRespond(withStatus(HttpStatus.NOT_FOUND).body("sensitive-upstream"));
        String message = assertThrows(IllegalStateException.class, service::leer).getMessage();
        assertTrue(message.contains("compartila"));
        assertFalse(message.contains("sensitive-upstream"));
    }
    @Test void rechazaSubpaginasSinGenerarContenidoParcial() {
        server.expect(requestTo(url(id))).andRespond(withSuccess(page(paragraph("ASUS USD 500") + ",{\"type\":\"child_page\"}"), MediaType.APPLICATION_JSON));
        assertThrows(IllegalArgumentException.class, service::leer);
    }
    @Test void sinConfiguracionNoLlamaNotion() {
        var sinClave = new NotionListadoService(new RestTemplateBuilder(), "", id);
        assertTrue(assertThrows(IllegalStateException.class, sinClave::leer).getMessage().contains("NOTION_TOKEN"));
    }
}
