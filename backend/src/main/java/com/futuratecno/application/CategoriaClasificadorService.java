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
 * Clasifica un producto dentro del árbol fijo de categorías, en orden de preferencia:
 *   1) Path estructural: si el mayorista ya manda la categoría padre (Invid expone
 *      CATEGORIES[].PARENT.NAME), se arma "padre > categoría" y se busca esa hoja exacta.
 *      Gratis e inequívoco — el propio proveedor ya resolvió la jerarquía.
 *   2) La categoría cruda sola, por si es una hoja de primer nivel (ej. "Tablets", "DESTACADOS").
 *   3) Mapeo manual de categorías crudas conocidas (gratis, instantáneo) para mayoristas que
 *      no mandan jerarquía (Elit) o cuando el nombre no calza exacto con el árbol.
 *   4) Por el NOMBRE del producto ({@link ClasificadorPorNombre}), también gratis. Cubre a Elit,
 *      cuyo vocabulario de categorías no calza con el árbol y dejaba el 60% del catálogo sin
 *      clasificar (y por lo tanto sin peso resoluble, o sea sin cotización de envío).
 *   5) Si nada de lo anterior resolvió — categoría ambigua o nueva que nunca vimos — se le pide
 *      a Claude que elija una hoja exacta de la lista cerrada, usando marca/modelo como contexto.
 *      Sin ANTHROPIC_API_KEY este paso devuelve null y el producto queda para asignar a mano.
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
    private final ClasificadorPorNombre clasificadorPorNombre;
    private final JevClient jev;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-haiku-4-5-20251001}")
    private String model;

    /** Por debajo de esta confianza, la categoría de Jev se descarta y el producto queda en null. */
    @Value("${typesafe.confianza-minima:0.7}")
    private double confianzaMinimaJev;

    public CategoriaClasificadorService(RestTemplate restTemplate, CategoriaService categoriaService,
                                        ClasificadorPorNombre clasificadorPorNombre, JevClient jev) {
        this.restTemplate = restTemplate;
        this.categoriaService = categoriaService;
        this.clasificadorPorNombre = clasificadorPorNombre;
        this.jev = jev;
    }

    public static final String PATH_TARJETAS = "Almacenamiento > Tarjetas de memoria";
    private static final java.util.regex.Pattern TARJETA = java.util.regex.Pattern.compile(
            "^\\s*(?:tarjeta\\s+(?:de\\s+)?memoria|memoria\\s+micro\\s*sd|micro\\s*sd(?:hc|xc)?\\b|sd(?:hc|xc)\\b)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /** ¿El nombre arranca diciendo que es una tarjeta de memoria ("Tarjeta de Memoria …", "MicroSD …")? */
    public static boolean esTarjetaDeMemoria(String modelo) {
        return modelo != null && TARJETA.matcher(modelo).find();
    }

    /** Clasifica sin categoría padre sugerida (mayoristas que no mandan jerarquía, ej. Elit). */
    public Long clasificar(Producto producto, String categoriaCruda) {
        return clasificar(producto, categoriaCruda, null);
    }

    /**
     * Clasifica y devuelve el categoriaId elegido, o null si no se pudo determinar.
     * {@code categoriaPadreSugerida}: la categoría padre que el propio mayorista ya informa
     * (ej. Invid vía CATEGORIES[].PARENT.NAME) — si matchea una hoja real del árbol, se usa
     * directo, sin gastar una llamada a la IA.
     */
    public Long clasificar(Producto producto, String categoriaCruda, String categoriaPadreSugerida) {
        // Apple va SIEMPRE a su propio árbol (V38), aunque la carga sugiera otra categoría: n8n
        // manda "Celulares" para los iPhone y los iPad +Cell, y esa pista ganaba (36 productos
        // de Apple fuera de su árbol al 2026-09-26). La regla de Apple nunca devuelve null.
        // Lo mismo con las Nintendo Switch: "switch" también es un equipo de red, y una pista
        // o una categoría vieja las dejaba en "Switches No Administrables".
        // Tarjetas de memoria: Elit las informa como "Memoria DDR4" y esa pista las mandaba a
        // Memorias RAM (34 al 2026-09-26). Se decide por cómo EMPIEZA el nombre, así no atrapa un
        // parlante "con MicroSD" ni un adaptador "USB a SD".
        if (esTarjetaDeMemoria(producto.getModelo())) {
            Long id = categoriaService.idPorPath(PATH_TARJETAS);
            if (id != null) return id;
        }
        String texto = ((producto.getMarca() == null ? "" : producto.getMarca()) + " "
                + (producto.getModelo() == null ? "" : producto.getModelo())).toLowerCase(java.util.Locale.ROOT);
        boolean nintendoSwitch = texto.contains("nintendo") && texto.contains("switch");
        if (nintendoSwitch || (producto.getMarca() != null && producto.getMarca().strip().equalsIgnoreCase("Apple"))) {
            String pathApple = clasificadorPorNombre.clasificar(producto);
            Long id = pathApple == null ? null : categoriaService.idPorPath(pathApple);
            if (id != null) return id;
        }
        // Las TABLETS caen en el mismo pozo que Apple: Kadabra manda "Celulares" para las
        // Galaxy Tab y las Redmi Pad, esa pista matchea una hoja real y gana antes de que
        // corra la clasificación por nombre — que SÍ las reconoce ("tablet|\\btab\\b|\\bpad\\b").
        // Resultado al 2026-09-26: 11 tablets listadas como celulares. Si el NOMBRE del producto
        // dice que es una tablet, eso pesa más que la categoría que informa el mayorista.
        // Ojo: el clasificador por nombre ya descarta los accesorios (fundas, teclados, mousepad,
        // gamepad) antes de llegar a su regla de tablets, así que acá no hay que repetir ese filtro.
        String pathPorNombreTemprano = clasificadorPorNombre.clasificar(producto);
        if (pathPorNombreTemprano != null && pathPorNombreTemprano.startsWith("Tablets")) {
            Long idTablet = categoriaService.idPorPath(pathPorNombreTemprano);
            if (idTablet != null) return idTablet;
        }
        if (categoriaPadreSugerida != null && categoriaCruda != null) {
            Long id = categoriaService.idPorPath(categoriaPadreSugerida + " > " + categoriaCruda);
            if (id != null) return id;
        }
        Long idHojaTopLevel = categoriaService.idPorPath(categoriaCruda);
        if (idHojaTopLevel != null) return idHojaTopLevel;

        // El mayorista puede tener una jerarquía más profunda que la nuestra (ej. Invid manda
        // "DDR3" bajo "Memoria Sodimm", pero en nuestro árbol "Memoria Sodimm" ya es la hoja):
        // si el padre que informa coincide con el nombre de alguna hoja única, redondeamos ahí.
        if (categoriaPadreSugerida != null) {
            Long idPorPadre = categoriaService.idPorNombreDeHojaUnico(categoriaPadreSugerida);
            if (idPorPadre != null) return idPorPadre;
        }

        String normalizada = normalizar(categoriaCruda);
        if (normalizada != null) {
            String pathManual = MAPEO_MANUAL.get(normalizada);
            if (pathManual != null) {
                Long id = categoriaService.idPorPath(pathManual);
                if (id != null) return id;
                logger.warn("Mapeo manual para '{}' apunta a un path inexistente: {}", categoriaCruda, pathManual);
            }
        }

        // Por nombre del producto, sin IA. Va antes de Claude a propósito: es gratis, instantáneo
        // y determinístico, y resuelve el 99% de lo que manda Elit (cuyo vocabulario de categorías
        // no calza con nuestro árbol, así que nunca llegaba a clasificarse).
        String pathPorNombre = clasificadorPorNombre.clasificar(producto);
        if (pathPorNombre != null) {
            Long id = categoriaService.idPorPath(pathPorNombre);
            if (id != null) return id;
            logger.warn("Clasificación por nombre apunta a un path inexistente: {}", pathPorNombre);
        }

        return clasificarConIa(producto, categoriaCruda);
    }

    private Long clasificarConIa(Producto producto, String categoriaCruda) {
        // Con Jev configurado se usa SOLO Jev, sin caer a Claude cuando duda: su confianza baja es
        // justamente el "ante la duda, null" de este clasificador, y un fallback pagaría dos veces.
        if (jev.configurado()) return clasificarConJev(producto, categoriaCruda);
        if (apiKey == null || apiKey.isBlank()) return null;
        List<String> paths = categoriaService.pathsDeHoja();
        String contexto = String.join(" ", List.of(
                categoriaCruda != null ? categoriaCruda : "",
                producto.getMarca() != null ? producto.getMarca() : "",
                producto.getModelo() != null ? producto.getModelo() : ""));

        String prompt = promptClaude(contexto, paths);

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

    static String promptClaude(String contexto, List<String> paths) {
        return "Elegí la subcategoría más adecuada para este producto de una tienda de tecnología argentina.\n\n"
                + "Producto: " + contexto.trim() + "\n\n"
                + "Lista CERRADA de subcategorías válidas (elegí EXACTAMENTE una, copiada tal cual):\n"
                + String.join("\n", paths) + "\n\n"
                + "Respondé SOLO con el path exacto elegido, sin ningún otro texto ni explicación.";
    }

    static final String INSTRUCCION_JEV = "This is a product sold by an Argentine electronics store (names may be in Spanish). "
            + "Which store category does the product itself belong to? An accessory for a device "
            + "goes to the accessory's category, not the device's.";

    // "Destacados" es una vidriera, no un tipo de producto: nunca es la respuesta correcta.
    static List<String> opcionesJev(List<String> paths) {
        return paths.stream().filter(p -> !p.equalsIgnoreCase("DESTACADOS")).toList();
    }

    static Map<String, String> estadoJev(Producto producto, String categoriaCruda) {
        Map<String, String> estado = new java.util.LinkedHashMap<>();
        estado.put("marca", producto.getMarca() == null ? "" : producto.getMarca());
        estado.put("modelo", producto.getModelo() == null ? "" : producto.getModelo());
        if (categoriaCruda != null && !categoriaCruda.isBlank()) estado.put("categoria_del_proveedor", categoriaCruda);
        return estado;
    }

    /** Paso 5 con Jev: elige la hoja de la lista cerrada y devuelve la categoría solo si está seguro. */
    Long clasificarConJev(Producto producto, String categoriaCruda) {
        ResultadoJev r = consultarJev(producto, categoriaCruda);
        if (r == null) return null;
        if (r.eleccion().confianza() < confianzaMinimaJev) {
            logger.info("Jev dudó con '{} {}': {} (confianza {})", producto.getMarca(), producto.getModelo(),
                    r.eleccion().opcion(), String.format(java.util.Locale.ROOT, "%.2f", r.eleccion().confianza()));
            return null;
        }
        return r.id();
    }

    public record ResultadoJev(JevClient.Eleccion eleccion, Long id) {}

    /** La elección de Jev sin aplicar el umbral, para medirlo. Null si no respondió. */
    public ResultadoJev consultarJev(Producto producto, String categoriaCruda) {
        JevClient.Eleccion e = jev.elegir(estadoJev(producto, categoriaCruda), INSTRUCCION_JEV,
                opcionesJev(categoriaService.pathsDeHoja()));
        return e == null ? null : new ResultadoJev(e, categoriaService.idPorPath(e.opcion()));
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
        m.put("SUPER OFERTAS", "DESTACADOS"); // etiqueta de marketing sin categoría real propia
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
