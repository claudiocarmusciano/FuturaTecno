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
 * Clasifica un producto dentro del árbol fijo de categorías. Primero prueba un mapeo
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
        // Categorías de primer nivel sin subcategoría: la categoría cruda calza exacto con la hoja.
        m.put("ACCESORIOS", "Accesorios");
        m.put("DESTACADOS", "DESTACADOS");
        m.put("ELECTRODOMÉSTICOS", "Electrodomésticos");
        m.put("PROYECTORES", "Proyectores");
        m.put("SILLAS Y ESCRITORIOS", "Sillas y escritorios");
        m.put("TABLETS", "Tablets");
        // Almacenamiento
        m.put("TARJETAS DE MEMORIA", "Almacenamiento > Tarjetas de memoria");
        // Computadoras
        m.put("KIT PC", "Computadoras > Kit PC");
        m.put("MINI PC", "Computadoras > Mini PC");
        m.put("PC", "Computadoras > PC");
        // Conectividad
        m.put("ACCESS POINT Y EXTENSORES DE RANGO", "Conectividad > Access Point y Extensores de Rango");
        m.put("MODEM ADSL Y GPON", "Conectividad > Modem ADSL y GPON");
        m.put("ROUTER", "Conectividad > Router");
        m.put("ROUTER WIRELESS", "Conectividad > Router Wireless");
        m.put("SWITCHES ADMINISTRABLES", "Conectividad > Switches Administrables");
        m.put("SWITCHES NO ADMINISTRABLES", "Conectividad > Switches No Administrables");
        // Consumibles
        m.put("CONSUMIBLES HP", "Consumibles > Consumibles HP");
        // Coolers
        m.put("FANS", "Coolers > Fans");
        m.put("WATERCOOLERS", "Coolers > Watercoolers");
        // Discos Rígidos / SSD
        m.put("DISCO RÍGIDO EXTERNO", "Discos Rígidos / SSD > Disco Rígido Externo");
        m.put("DISCO RÍGIDO SATA", "Discos Rígidos / SSD > Disco Rígido SATA");
        m.put("DISCO SSD", "Discos Rígidos / SSD > Disco SSD");
        m.put("DISCO SSD M2", "Discos Rígidos / SSD > Disco SSD M2");
        // Energía
        m.put("UPS", "Energía > UPS");
        // Gabinetes y Fuentes
        m.put("FUENTES DE ALIMENTACIÓN", "Gabinetes y Fuentes > Fuentes de Alimentación");
        m.put("GABINETES CON FUENTE", "Gabinetes y Fuentes > Gabinetes con Fuente");
        m.put("GABINETES SIN FUENTE", "Gabinetes y Fuentes > Gabinetes sin Fuente");
        // Impresoras
        m.put("INK JET", "Impresoras > Ink Jet");
        m.put("LASER", "Impresoras > Laser");
        m.put("MULTIFUNCIÓN", "Impresoras > Multifunción");
        // Memorias RAM
        m.put("DDR4", "Memorias RAM > Memoria DDR4");
        m.put("DDR5", "Memorias RAM > Memoria DDR5");
        m.put("MEMORIA DDR4", "Memorias RAM > Memoria DDR4");
        m.put("MEMORIA DDR5", "Memorias RAM > Memoria DDR5");
        m.put("MEMORIA SODIMM", "Memorias RAM > Memoria Sodimm");
        // Monitores
        m.put("MONITOR CONSUMO", "Monitores > Monitor Consumo");
        m.put("MONITOR CORPORATIVO", "Monitores > Monitor Corporativo");
        m.put("MONITOR GAMER", "Monitores > Monitor Gamer");
        // Mothers
        m.put("PLATAFORMA AMD", "Mothers > Plataforma AMD");
        m.put("PLATAFORMA INTEL", "Mothers > Plataforma Intel");
        // Notebooks
        m.put("NOTEBOOK GAMER", "Notebooks > Gamer");
        m.put("NOTEBOOK OFFICE", "Notebooks > Corporativa");
        // Periféricos
        m.put("AURICULARES", "Periféricos > Auriculares");
        m.put("MICRÓFONOS", "Periféricos > Micrófonos");
        m.put("MOUSE", "Periféricos > Mouse");
        m.put("MOUSEPADS", "Periféricos > Mousepads");
        m.put("PARLANTES", "Periféricos > Parlantes");
        m.put("TECLADOS", "Periféricos > Teclados");
        m.put("WEB CAM", "Periféricos > Web Cam");
        // Placas de video
        m.put("LÍNEA AMD RADEON", "Placas de video > Línea AMD RADEON");
        m.put("LÍNEA NVIDIA GEFORCE", "Placas de video > Línea NVIDIA GEFORCE");
        m.put("LÍNEA QUADRO/RADEON PRO", "Placas de video > Línea Quadro/Radeon Pro");
        return m;
    }
}
