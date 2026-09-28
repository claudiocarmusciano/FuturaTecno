package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de Jev (TypeSafe AI, https://docs.typesafe.ai). No genera texto: recibe un "state" y
 * preguntas cerradas, y devuelve la opción elegida con su confianza. Por eso sirve para elegir
 * una categoría de la lista del árbol y no para buscar imágenes: solo acepta texto y no navega.
 *
 * Sin TYPESAFE_API_KEY no está configurado y quien lo use sigue por su camino de siempre.
 */
@Component
public class JevClient {
    private static final Logger logger = LoggerFactory.getLogger(JevClient.class);
    private static final String API_URL = "https://api.typesafe.ai/v1/systemone";
    /** Tope de opciones por pregunta Choice que acepta la API. */
    public static final int MAX_OPCIONES = 255;

    public record Eleccion(String opcion, double confianza) {}

    @Value("${typesafe.api-key:}")
    private String apiKey;

    @Value("${typesafe.model:jev-latest}")
    private String model;

    private final RestTemplate http;

    public JevClient() {
        // Jev responde en décimas de segundo. Timeouts propios porque el RestTemplate por defecto
        // del proyecto no tiene ninguno, y esto corre dentro de importaciones.
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3_000);
        f.setReadTimeout(10_000);
        this.http = new RestTemplate(f);
    }

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Elige una opción de {@code opciones} para {@code estado}. Null si no está configurado, si
     * la llamada falla o si la respuesta no trae una de las opciones: quien llama decide qué
     * hacer ante la duda, este cliente nunca inventa una respuesta.
     */
    public Eleccion elegir(Object estado, String instrucciones, List<String> opciones) {
        if (!configurado() || opciones.isEmpty() || opciones.size() > MAX_OPCIONES) return null;
        Map<String, Object> criterios = new LinkedHashMap<>();
        opciones.forEach(o -> criterios.put(o, null));
        Map<String, Object> pregunta = Map.of("type", "choice", "instructions", instrucciones, "criteria", criterios);
        Map<String, Object> body = Map.of("state", estado, "model", model, "questions", Map.of("respuesta", pregunta));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            JsonNode resp = http.postForObject(API_URL, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode respuesta = resp == null ? null : resp.path("answers").path("respuesta");
            if (respuesta == null || !respuesta.hasNonNull("choice")) return null;
            String elegida = respuesta.get("choice").asText();
            if (!criterios.containsKey(elegida)) {
                logger.warn("Jev devolvió una opción que no estaba en la lista: '{}'", elegida);
                return null;
            }
            return new Eleccion(elegida, respuesta.path("confidence").asDouble(0));
        } catch (HttpStatusCodeException e) {
            // El cuerpo del error dice más que cualquier texto propio (clave inválida, saldo, formato).
            logger.warn("Jev respondió {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.warn("Jev falló: {}", e.toString());
            return null;
        }
    }
}
