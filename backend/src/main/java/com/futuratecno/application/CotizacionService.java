package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Obtiene la cotización del dólar oficial (USD -> ARS) desde dolarapi.com.
 * Cachea el valor en memoria para no llamar a la API en cada parsing.
 */
@Service
public class CotizacionService {
    private static final Logger logger = LoggerFactory.getLogger(CotizacionService.class);

    private final RestTemplate restTemplate;

    @Value("${cotizacion.api-url:https://dolarapi.com/v1/dolares/oficial}")
    private String apiUrl;

    @Value("${cotizacion.cache-minutos:30}")
    private long cacheMinutos;

    @Value("${cotizacion.fallback:1050}")
    private BigDecimal fallback;

    private BigDecimal valorCacheado;
    private Instant ultimaActualizacion;

    public CotizacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Devuelve la cotización de venta del dólar oficial en ARS por 1 USD.
     * Usa caché de {@code cacheMinutos}; si la API falla, usa el último valor o el fallback.
     */
    public BigDecimal obtenerCotizacionUsdArs() {
        if (valorCacheado != null && ultimaActualizacion != null
                && Duration.between(ultimaActualizacion, Instant.now()).toMinutes() < cacheMinutos) {
            return valorCacheado;
        }

        try {
            JsonNode response = restTemplate.getForObject(apiUrl, JsonNode.class);
            if (response != null && response.has("venta")) {
                valorCacheado = response.get("venta").decimalValue();
                ultimaActualizacion = Instant.now();
                logger.info("Cotización dólar oficial actualizada: {} ARS/USD", valorCacheado);
                return valorCacheado;
            }
            logger.warn("Respuesta de cotización sin campo 'venta', usando fallback");
        } catch (Exception e) {
            logger.error("Error al obtener cotización, usando último valor/fallback", e);
        }

        return valorCacheado != null ? valorCacheado : fallback;
    }
}
