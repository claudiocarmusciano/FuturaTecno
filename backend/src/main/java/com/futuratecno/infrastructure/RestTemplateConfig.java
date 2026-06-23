package com.futuratecno.infrastructure;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate por defecto (sin timeout corto) — usado por el parsing de Claude,
     * que puede tardar varios segundos en listas grandes.
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
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }
}
