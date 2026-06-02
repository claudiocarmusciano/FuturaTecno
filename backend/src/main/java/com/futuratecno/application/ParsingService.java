package com.futuratecno.application;

import com.futuratecno.api.dto.ParsingResponse;
import com.futuratecno.api.dto.ProductoParseado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParsingService {
    private static final Logger logger = LoggerFactory.getLogger(ParsingService.class);

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model:claude-3-5-haiku-20241022}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CotizacionService cotizacionService;

    public ParsingService(RestTemplate restTemplate, ObjectMapper objectMapper, CotizacionService cotizacionService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.cotizacionService = cotizacionService;
    }

    public ParsingResponse parsearLista(String texto) {
        try {
            String prompt = construirPrompt(texto);
            String respuesta = llamarClaudeAPI(prompt);
            return procesarRespuesta(respuesta);
        } catch (Exception e) {
            logger.error("Error al parsear lista", e);
            return construirRespuestaError("Error al procesar: " + e.getMessage());
        }
    }

    private String construirPrompt(String texto) {
        return """
                Extrae los productos de esta lista de precios de WhatsApp.
                Para cada línea identifica: marca, modelo, especificaciones opcionales (RAM+almacenamiento), precio y moneda.

                REGLAS PARA LA MONEDA:
                - Si el precio dice "USD", "U$S", "U$D", "dolares" o similar, la moneda es "USD".
                - Si el precio usa solo "$" o "pesos" sin mención de dólar, la moneda es "ARS".
                - Por defecto, si no hay ninguna mención de dólar, asume "ARS".
                - El campo "precio" debe ser SOLO el número (sin símbolos ni puntos de miles), ej: 208784 o 890.

                Devuelve EXACTAMENTE un JSON válido SIN backticks, SIN explicaciones, SIN texto adicional. Responde SOLO con el JSON:
                {"productos": [{"marca": "...", "modelo": "...", "especificaciones": "...", "precio": 123.45, "moneda": "ARS"}]}

                Si no puedes extraer algún campo, úsalo como null.

                Lista de precios:
                """ + texto;
    }

    private String llamarClaudeAPI(String prompt) {
        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 4096);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response == null || response.isEmpty()) {
            throw new RuntimeException("Respuesta vacía de Claude API");
        }

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Sin contenido en respuesta de Claude");
        }

        return (String) content.get(0).get("text");
    }

    private ParsingResponse procesarRespuesta(String respuestaJson) {
        try {
            // Limpiar backticks y espacios en blanco si existen
            respuestaJson = respuestaJson.trim();
            if (respuestaJson.startsWith("```json")) {
                respuestaJson = respuestaJson.substring(7);
            }
            if (respuestaJson.startsWith("```")) {
                respuestaJson = respuestaJson.substring(3);
            }
            if (respuestaJson.endsWith("```")) {
                respuestaJson = respuestaJson.substring(0, respuestaJson.length() - 3);
            }
            respuestaJson = respuestaJson.trim();

            JsonNode rootNode = objectMapper.readTree(respuestaJson);
            JsonNode productosNode = rootNode.get("productos");

            if (productosNode == null || !productosNode.isArray()) {
                return construirRespuestaError("Formato de respuesta inválido");
            }

            BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
            List<ProductoParseado> productos = new ArrayList<>();

            for (JsonNode prodNode : productosNode) {
                ProductoParseado producto = new ProductoParseado();

                producto.setMarca(obtenerString(prodNode, "marca"));
                producto.setModelo(obtenerString(prodNode, "modelo"));
                producto.setEspecificaciones(obtenerString(prodNode, "especificaciones"));

                String moneda = obtenerString(prodNode, "moneda");
                moneda = (moneda != null && moneda.equalsIgnoreCase("USD")) ? "USD" : "ARS";
                producto.setMonedaOrigen(moneda);

                String precioStr = obtenerString(prodNode, "precio");
                if (precioStr != null && !precioStr.isEmpty()) {
                    try {
                        BigDecimal precio = new BigDecimal(precioStr);
                        if ("USD".equals(moneda)) {
                            producto.setPrecioUsd(precio);
                            producto.setPrecioArs(precio.multiply(cotizacion).setScale(2, RoundingMode.HALF_UP));
                        } else {
                            producto.setPrecioArs(precio.setScale(2, RoundingMode.HALF_UP));
                            producto.setPrecioUsd(precio.divide(cotizacion, 2, RoundingMode.HALF_UP));
                        }
                        producto.setEstado("exitoso");
                    } catch (NumberFormatException e) {
                        producto.setEstado("error");
                        producto.setMensaje("Precio inválido: " + precioStr);
                    }
                } else {
                    producto.setEstado("advertencia");
                    producto.setMensaje("Precio no encontrado");
                }

                if (producto.getMarca() != null && producto.getModelo() != null) {
                    productos.add(producto);
                }
            }

            ParsingResponse response = new ParsingResponse();
            response.setProductos(productos);
            response.setTotal(productos.size());
            response.setProcesados((int) productos.stream().filter(p -> "exitoso".equals(p.getEstado())).count());
            response.setCotizacionUsdArs(cotizacion);
            response.setMensaje("Parsing completado: " + response.getProcesados() + "/" + response.getTotal()
                    + " productos procesados (dólar oficial: $" + cotizacion + ")");

            return response;
        } catch (Exception e) {
            logger.error("Error procesando respuesta JSON", e);
            return construirRespuestaError("Error al procesar respuesta: " + e.getMessage());
        }
    }

    private String obtenerString(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText();
    }

    private ParsingResponse construirRespuestaError(String mensaje) {
        ParsingResponse response = new ParsingResponse();
        response.setProductos(new ArrayList<>());
        response.setTotal(0);
        response.setProcesados(0);
        response.setMensaje(mensaje);
        return response;
    }
}
