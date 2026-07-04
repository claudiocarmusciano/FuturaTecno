package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.futuratecno.domain.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clasifica un producto dentro del árbol fijo de categorías (V9). Primero prueba un mapeo
 * manual de categorías crudas conocidas del mayorista (gratis, instantáneo). Si no matchea
 * — categoría ambigua o nueva que nunca vimos — le pide a Claude que elija una hoja exacta
 * de la lista cerrada, usando también marca/modelo/especificaciones como contexto (la misma
 * categoría cruda puede corresponder a hojas distintas según el producto puntual).
 *
 * El resultado se persiste en Producto.categoriaId y solo se recalcula si ese campo está en
 * null — así una corrección manual del admin no se pisa en el próximo sync, y no se vuelve
 * a pagar la llamada a la IA para el mismo producto.
 */
@Service
public class CategoriaClasificadorService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaClasificadorService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    // Categoría cruda del mayorista (normalizada: sin HTML, mayúsculas, sin espacios de más)
    // -> path exacto de la hoja destino. Cubre los casos inequívocos; el resto pasa por IA.
    private static final Map<String, String> MAPEO_MANUAL = construirMapeoManual();

    private final RestTemplate restTemplate;
    private final CategoriaService categoriaService;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-haiku-4-5-20251001}")
    private String model;

    public CategoriaClasificadorService(RestTemplate restTemplate, CategoriaService categoriaService) {
        this.restTemplate = restTemplate;
        this.categoriaService = categoriaService;
    }

    /** Clasifica y devuelve el categoriaId elegido, o null si no se pudo determinar. */
    public Long clasificar(Producto producto, String categoriaCruda) {
        String normalizada = normalizar(categoriaCruda);
        if (normalizada != null) {
            String pathManual = MAPEO_MANUAL.get(normalizada);
            if (pathManual != null) {
                Long id = categoriaService.idPorPath(pathManual);
                if (id != null) return id;
                logger.warn("Mapeo manual para '{}' apunta a un path inexistente: {}", categoriaCruda, pathManual);
            }
        }
        return clasificarConIa(producto, categoriaCruda);
    }

    private Long clasificarConIa(Producto producto, String categoriaCruda) {
        if (apiKey == null || apiKey.isBlank()) return null;
        List<String> paths = categoriaService.pathsDeHoja();
        String contexto = String.join(" ", List.of(
                categoriaCruda != null ? categoriaCruda : "",
                producto.getMarca() != null ? producto.getMarca() : "",
                producto.getModelo() != null ? producto.getModelo() : ""));

        String prompt = "Elegí la subcategoría más adecuada para este producto de una tienda de tecnología argentina.\n\n"
                + "Producto: " + contexto.trim() + "\n\n"
                + "Lista CERRADA de subcategorías válidas (elegí EXACTAMENTE una, copiada tal cual):\n"
                + String.join("\n", paths) + "\n\n"
                + "Respondé SOLO con el path exacto elegido, sin ningún otro texto ni explicación.";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 200);
            body.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            JsonNode resp = restTemplate.postForObject(API_URL, req, JsonNode.class);
            if (resp == null) return null;

            StringBuilder texto = new StringBuilder();
            for (JsonNode block : resp.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    texto.append(block.path("text").asText());
                }
            }
            String elegido = texto.toString().trim();
            Long id = categoriaService.idPorPath(elegido);
            if (id == null) {
                logger.warn("IA devolvió un path que no matchea ninguna hoja para '{}': '{}'", categoriaCruda, elegido);
            }
            return id;
        } catch (Exception e) {
            logger.warn("Clasificación por IA falló para '{}': {}", categoriaCruda, e.toString());
            return null;
        }
    }

    private String normalizar(String s) {
        if (s == null) return null;
        String sinHtml = s.replaceAll("<[^>]+>", "");
        String limpio = sinHtml.trim().replaceAll("\\s+", " ").toUpperCase();
        return limpio.isBlank() ? null : limpio;
    }

    private static Map<String, String> construirMapeoManual() {
        Map<String, String> m = new HashMap<>();
        // Componentes > Almacenamiento
        m.put("DISCO RÍGIDO EXTERNO", "Componentes > Almacenamiento > Discos Externos");
        m.put("DISCO RÍGIDO SATA", "Componentes > Almacenamiento > Discos Internos");
        m.put("DISCO SSD", "Componentes > Almacenamiento > Discos Internos SSD");
        m.put("DISCO SSD M2", "Componentes > Almacenamiento > Discos Internos SSD");
        m.put("TARJETAS DE MEMORIA", "Componentes > Almacenamiento > Memorias Flash");
        // Componentes > Hardware
        m.put("FANS", "Componentes > Hardware > Coolers");
        m.put("WATERCOOLERS", "Componentes > Hardware > Coolers");
        m.put("FUENTES DE ALIMENTACIÓN", "Componentes > Hardware > Fuentes");
        m.put("GABINETES CON FUENTE", "Componentes > Hardware > Gabinetes");
        m.put("GABINETES SIN FUENTE", "Componentes > Hardware > Gabinetes");
        m.put("LÍNEA AMD RADEON", "Componentes > Hardware > Placas de Video");
        m.put("LÍNEA NVIDIA GEFORCE", "Componentes > Hardware > Placas de Video");
        m.put("LÍNEA QUADRO/RADEON PRO", "Componentes > Hardware > Placas de Video");
        // Componentes > Memorias
        m.put("DDR4", "Componentes > Memorias > Memorias PC");
        m.put("DDR5", "Componentes > Memorias > Memorias PC");
        m.put("MEMORIA DDR4", "Componentes > Memorias > Memorias PC");
        m.put("MEMORIA DDR5", "Componentes > Memorias > Memorias PC");
        m.put("MEMORIA SODIMM", "Componentes > Memorias > Memorias Notebook");
        // Cómputos > Computadoras
        m.put("MINI PC", "Cómputos > Computadoras > Mini PC");
        m.put("NOTEBOOK GAMER", "Cómputos > Computadoras > Notebooks Consumo");
        m.put("NOTEBOOK OFFICE", "Cómputos > Computadoras > Notebooks Corporativo");
        // Cómputos > Imagen
        m.put("MONITOR CONSUMO", "Cómputos > Imagen > Monitores");
        m.put("MONITOR CORPORATIVO", "Cómputos > Imagen > Monitores");
        m.put("MONITOR GAMER", "Cómputos > Imagen > Monitores");
        m.put("PROYECTORES", "Cómputos > Imagen > Proyectores");
        // Impresión
        m.put("INK JET", "Impresión > Impresoras > Impresoras Inkjet");
        m.put("LASER", "Impresión > Impresoras > Impresoras Láser");
        // Otros > Audio
        m.put("AURICULARES", "Otros > Audio > Auriculares");
        m.put("MICRÓFONOS", "Otros > Audio > Micrófonos");
        m.put("PARLANTES", "Otros > Audio > Parlantes");
        // Otros > Conectividad
        m.put("MODEM ADSL Y GPON", "Otros > Conectividad > Routers");
        m.put("ROUTER", "Otros > Conectividad > Routers");
        m.put("ROUTER WIRELESS", "Otros > Conectividad > Routers");
        m.put("SWITCHES ADMINISTRABLES", "Otros > Conectividad > Switches");
        m.put("SWITCHES NO ADMINISTRABLES", "Otros > Conectividad > Switches");
        // Otros > Periféricos
        m.put("MOUSE", "Otros > Periféricos > Mouses");
        m.put("MOUSEPADS", "Otros > Periféricos > Mouse Pads");
        m.put("TECLADOS", "Otros > Periféricos > Teclados");
        m.put("WEB CAM", "Otros > Periféricos > Cámaras Web");
        // Otros > Unidad de Energía
        m.put("UPS", "Otros > Unidad de Energía > UPS");
        // Otros > Video Juegos
        m.put("CONSOLA", "Otros > Video Juegos > Consolas");
        return m;
    }
}
