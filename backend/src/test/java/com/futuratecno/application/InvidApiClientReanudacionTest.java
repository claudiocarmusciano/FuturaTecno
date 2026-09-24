package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El catálogo de Invid pasa las 50 páginas y la cuota es de 50 consultas por hora. Un reintento
 * que arrancara de la página 1 volvería a cortar en el mismo lugar para siempre (21–23/9/2026).
 */
class InvidApiClientReanudacionTest {

    private final ObjectMapper om = new ObjectMapper();
    private RestTemplate rest;
    private InvidApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        rest = mock(RestTemplate.class);
        client = new InvidApiClient(rest);
        ReflectionTestUtils.setField(client, "baseUrl", "https://invid.test");
        ReflectionTestUtils.setField(client, "username", "u");
        ReflectionTestUtils.setField(client, "password", "p");
        when(rest.postForObject(contains("auth.php"), any(), eq(JsonNode.class)))
                .thenReturn(om.readTree("{\"access_token\":\"t\",\"expiration_time\":86400}"));
    }

    private ResponseEntity<JsonNode> pagina(int n, Integer siguiente) throws Exception {
        String next = siguiente == null ? "null" : "\"https://invid.test/api/v1/articulo.php?page=" + siguiente + "\"";
        return ResponseEntity.ok(om.readTree("{\"data\":[{\"id\":" + n + "}],\"next_page_url\":" + next + "}"));
    }

    private static HttpClientErrorException limite() {
        HttpHeaders h = new HttpHeaders();
        h.set("Retry-After", "60");
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", h,
                new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    void elReintentoSigueDesdeLaPaginaQueCortoSinRepetirLasAnteriores() throws Exception {
        List<String> pedidas = new ArrayList<>();
        int[] intentosPagina3 = {0};
        when(rest.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenAnswer(inv -> {
                    String url = inv.getArgument(0);
                    pedidas.add(url.contains("page=") ? url.replaceAll(".*page=(\\d+).*", "p$1") : "p1");
                    if (url.contains("page=3")) {
                        if (intentosPagina3[0]++ == 0) throw limite();   // la cuota se agota acá
                        return pagina(3, null);
                    }
                    return url.contains("page=2") ? pagina(2, 3) : pagina(1, 2);
                });

        assertThrows(InvidRateLimitException.class, () -> client.obtenerArticulos(false));
        List<JsonNode> todo = client.obtenerArticulos(false);

        assertEquals(List.of(1, 2, 3), todo.stream().map(n -> n.path("id").asInt()).toList());
        // La segunda corrida arranca en la página que falló: la 1 y la 2 se pidieron una sola vez.
        assertEquals(List.of("p1", "p2", "p3", "p3"), pedidas);
    }
}
