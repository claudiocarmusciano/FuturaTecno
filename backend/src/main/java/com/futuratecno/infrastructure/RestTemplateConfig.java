package com.futuratecno.infrastructure;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.net.HttpURLConnection;
import java.io.IOException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate por defecto (sin timeout corto) — lo usan las llamadas que pueden tardar
     * varios segundos, como la clasificación de categorías con Claude.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * RestTemplate dedicado a la búsqueda de imágenes (Icecat, Google, DuckDuckGo),
     * con timeouts cortos para que ninguna llamada lenta cuelgue todo el proceso.
     */
    @Bean(name = "imageRestTemplate")
    public RestTemplate imageRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    /**
     * La búsqueda web de Anthropic puede tardar más que una descarga de imagen, pero tampoco
     * debe retener la pantalla de administración sin límite. Se usa exclusivamente para el
     * fallback de imágenes cuando DuckDuckGo no encuentra una URL directa válida.
     */
    @Bean(name = "anthropicImageRestTemplate")
    public RestTemplate anthropicImageRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Generación de listados. Los 90 s que tenía no alcanzaban: DeepSeek tarda más de minuto y
     * medio en redactar el JSON de un listado de ~40 artículos, y como el navegador se rendía a
     * los 100 s el admin recibía una pantalla sin explicación (no llegaba respuesta, así que ni
     * siquiera se veía el mensaje del servidor).
     *
     * <p>El backend tiene que rendirse SIEMPRE antes que el navegador — que espera 240 s — para
     * que del otro lado llegue un motivo y no un silencio. Ojo: este es un timeout de lectura del
     * socket, no un presupuesto total, así que el corte del navegador sigue siendo la única
     * garantía dura si el proveedor manda datos de a poco.
     */
    @Bean(name = "listadoRestTemplate")
    public RestTemplate listadoRestTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(180)).build();
    }

    /**
     * RestTemplate para la API de Resend. Con timeouts explícitos: el envío de mails nunca debe
     * poder colgar un hilo indefinidamente (el SMTP anterior no tenía timeout y dejaba requests
     * de varios minutos esperando una conexión que Railway bloqueaba).
     */
    @Bean(name = "mailRestTemplate")
    public RestTemplate mailRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * RestTemplate para la API de Andreani (cotización de envíos). La cotización corre dentro del
     * request del checkout, así que una API lenta no puede colgar el hilo: mejor responder
     * "sin cotización" que dejar al cliente esperando.
     */
    @Bean(name = "envioRestTemplate")
    public RestTemplate envioRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Para disparar el webhook de n8n desde el botón del admin. El webhook responde al recibir
     * (no espera a que terminen las listas), así que 10 s sobran y el botón nunca queda colgado.
     */
    @Bean(name = "n8nRestTemplate")
    public RestTemplate n8nRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
