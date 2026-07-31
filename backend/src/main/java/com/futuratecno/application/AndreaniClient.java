package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente de la API Pyme de Andreani. OJO: no es la API corporativa pública
 * ({@code apis.andreani.com}, que exige contrato comercial) sino el middleware que Andreani montó
 * para su plugin de WooCommerce ({@code woocommerce-api-acom.andreani.com}) — es la única vía de
 * API para cuentas Pyme, pero Andreani podría cambiarla sin preaviso pensando solo en su plugin.
 * Por eso todo consumidor tiene que tolerar que esto falle (degradado con gracia, como el mail).
 *
 * <p>Autenticación: la credencial ("hash") se genera en el portal Pyme (Integraciones →
 * WooCommerce) y viaja tal cual en {@code Authorization} contra {@code /api/v1/Login}. El Login
 * devuelve un accessToken (header {@code X-Auth-Token} de las siguientes llamadas) y la lista de
 * contratos de la cuenta. El token se cachea; ante un 401 se reloguea una vez y se reintenta.
 */
@Service
public class AndreaniClient {
    private static final Logger logger = LoggerFactory.getLogger(AndreaniClient.class);

    private final RestTemplate restTemplate;

    @Value("${andreani.base-url:https://woocommerce-api-acom.andreani.com}")
    private String baseUrl;

    @Value("${andreani.hash:}")
    private String hash;

    @Value("${andreani.cp-origen:}")
    private String cpOrigen;

    private volatile String accessToken;

    public AndreaniClient(@Qualifier("envioRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return hash != null && !hash.isBlank() && cpOrigen != null && !cpOrigen.isBlank();
    }

    public String getCpOrigen() {
        return cpOrigen;
    }

    /**
     * Cotiza un envío. Cada producto va como {@code {quantity, price, dimensions: {width, height,
     * depth, grams}}} (dimensiones en cm enteros, precio en ARS — lo usa el seguro). Devuelve el
     * array {@code rates} de la respuesta: {@code [{code: "estándar"|"sucursal"|..., total}]},
     * con las opciones de sucursal repetidas por cada punto de retiro del CP.
     *
     * @throws IllegalStateException si el cliente no está configurado
     */
    public JsonNode cotizar(String cpDestino, List<Map<String, Object>> products) {
        if (!estaConfigurado()) {
            throw new IllegalStateException("Andreani no está configurado.");
        }
        Map<String, Object> body = Map.of(
                "postal_code_origin", cpOrigen.trim(),
                "postal_code_destination", cpDestino.trim(),
                "products", products);

        JsonNode respuesta = conToken(token -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Token", token);
            return restTemplate.postForObject(baseUrl + "/api/v1/Pyme/rates",
                    new HttpEntity<>(body, headers), JsonNode.class);
        });
        return respuesta != null ? respuesta.path("response").path("rates") : null;
    }

    /**
     * Ejecuta la llamada con el token cacheado; si Andreani devuelve 401 (token vencido),
     * reloguea una sola vez y reintenta.
     */
    private JsonNode conToken(LlamadaAutenticada llamada) {
        String token = accessToken;
        if (token == null) {
            token = login();
        }
        try {
            return llamada.ejecutar(token);
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.info("Andreani: token vencido, relogueando.");
            return llamada.ejecutar(login());
        }
    }

    /** Login con la credencial del portal. Guarda y devuelve el accessToken. */
    private synchronized String login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, hash.trim());

        JsonNode respuesta = restTemplate.postForObject(baseUrl + "/api/v1/Login",
                new HttpEntity<>(headers), JsonNode.class);
        String token = respuesta != null ? respuesta.path("response").path("accessToken").asText(null) : null;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Andreani no devolvió accessToken en el Login.");
        }
        accessToken = token;
        logger.info("Andreani: login OK (cliente {}).",
                respuesta.path("response").path("cl").asText("?"));
        return token;
    }

    @FunctionalInterface
    private interface LlamadaAutenticada {
        JsonNode ejecutar(String token);
    }
}
