package com.futuratecno.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ServerSocket;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El RestTemplate de generación TIENE que rendirse solo. Sin eso, un proveedor que acepta la
 * conexión y después no contesta deja el hilo de Tomcat esperando para siempre: el navegador se
 * cansa, el admin ve una pantalla sin explicación y en los logs no queda ni un error, porque del
 * lado del servidor nunca pasó nada. Pasó en producción con DeepSeek el 2026-09-14.
 *
 * <p>Se usa el mismo encadenado del bean pero con 2 s en vez de 180, para que el test sea rápido:
 * lo que se está verificando es que {@code RestTemplateBuilder} aplique de verdad el timeout de
 * lectura, no el número concreto.
 */
class ListadoRestTemplateTimeoutTest {

    @Test void seRindeCuandoElProveedorAceptaYNoContesta() throws Exception {
        // Acepta la conexión y no escribe nada: el connect timeout no aplica acá, solo el de lectura.
        try (ServerSocket mudo = new ServerSocket(0)) {
            RestTemplate http = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofSeconds(2))
                    .setReadTimeout(Duration.ofSeconds(2))
                    .build();

            long inicio = System.currentTimeMillis();
            assertThrows(ResourceAccessException.class,
                    () -> http.getForObject("http://localhost:" + mudo.getLocalPort() + "/", String.class));
            long tardo = System.currentTimeMillis() - inicio;

            assertTrue(tardo < 10_000, "no respetó el timeout de lectura: tardó " + tardo + " ms");
        }
    }
}
