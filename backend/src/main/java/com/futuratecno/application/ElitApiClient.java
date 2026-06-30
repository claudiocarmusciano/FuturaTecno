package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente de la API de Elit (mayorista). Documentación: clientes.elit.com.ar (panel del cliente).
 * Autenticación: user_id + token en el body JSON. Endpoint principal: POST /productos.
 * Las credenciales se inyectan por variables de entorno (ELIT_USER_ID, ELIT_TOKEN) — nunca en código.
 */
@Service
public class ElitApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ElitApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${elit.base-url:https://clientes.elit.com.ar/v1/api}")
    private String baseUrl;

    @Value("${elit.user-id:}")
    private String userId;

    @Value("${elit.token:}")
    private String token;

    public ElitApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean estaConfigurado() {
        return userId != null && !userId.isBlank() && token != null && !token.isBlank();
    }

    /**
     * Consulta el listado de productos. Los filtros (categoria, marca, actualizacion, store) viajan
     * como query params; user_id y token en el body. Devuelve la respuesta completa (paginador + resultado).
     */
    public JsonNode consultarProductos(Integer limit, Integer offset, String categoria, String marca,
                                       String actualizacion, String store) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/productos");
        if (limit != null) uri.queryParam("limit", limit);
        if (offset != null) uri.queryParam("offset", offset);
        if (notBlank(categoria)) uri.queryParam("categoria", categoria.trim());
        if (notBlank(marca)) uri.queryParam("marca", marca.trim());
        if (notBlank(actualizacion)) uri.queryParam("actualizacion", actualizacion.trim());
        if (notBlank(store)) uri.queryParam("store", store.trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        try {
            body.put("user_id", Long.parseLong(userId.trim()));
        } catch (NumberFormatException e) {
            body.put("user_id", userId.trim());
        }
        body.put("token", token.trim());

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        String url = uri.build(true).toUriString();
        logger.debug("Elit: POST {}", url);
        return restTemplate.postForObject(url, req, JsonNode.class);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
