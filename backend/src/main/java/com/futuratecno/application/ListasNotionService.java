package com.futuratecno.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Dispara el workflow de n8n que procesa las listas de proveedores cargadas en Notion como
 * "Pendiente" (Notion → OpenAI → /api/admin/carga-json). n8n corre en otro proyecto de Railway.
 *
 * <p>La URL del webhook lleva un path aleatorio largo y vive solo en {@code N8N_PROCESAR_LISTAS_URL}:
 * es el secreto. Nunca llega al navegador (el repo es público y el front se sirve a cualquiera).
 * El webhook responde apenas recibe el pedido; el resultado de cada lista queda en Notion. Si se
 * aprieta dos veces, n8n encola las ejecuciones (N8N_CONCURRENCY_PRODUCTION_LIMIT=1) y la segunda
 * no encuentra nada en Pendiente.
 */
@Service
public class ListasNotionService {

    private static final Logger logger = LoggerFactory.getLogger(ListasNotionService.class);

    /** El pedido no se pudo hacer: sin configurar o n8n no respondió. El mensaje es para el admin. */
    public static class NoDisponibleException extends RuntimeException {
        private final boolean configurado;
        public NoDisponibleException(String mensaje, boolean configurado) { super(mensaje); this.configurado = configurado; }
        public boolean isConfigurado() { return configurado; }
    }

    /** Si n8n no avisa en este tiempo, algo falló o no había listas: el admin mira Notion. */
    static final java.time.Duration SIN_RESPUESTA = java.time.Duration.ofMinutes(10);

    private final RestTemplate restTemplate;
    private final String webhookUrl;
    private final java.time.Clock reloj;

    /*
     * Estado en memoria, a propósito: es un aviso para la pantalla, no un registro. Si la app se
     * reinicia se pierde y la tarjeta vuelve a "sin datos"; el resultado de cada lista sigue en
     * Notion, que es la fuente de verdad.
     */
    private volatile java.time.Instant pedidoEn;
    private volatile java.time.Instant resultadoEn;
    private volatile java.util.Map<String, Object> resultado;

    // Hay dos constructores (el otro es para tests, con reloj): sin @Autowired Spring no sabe cuál usar
    // y la app no arranca.
    @org.springframework.beans.factory.annotation.Autowired
    public ListasNotionService(@Qualifier("n8nRestTemplate") RestTemplate restTemplate,
                               @Value("${app.n8n.procesar-listas-url:}") String webhookUrl) {
        this(restTemplate, webhookUrl, java.time.Clock.systemUTC());
    }

    ListasNotionService(RestTemplate restTemplate, String webhookUrl, java.time.Clock reloj) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.reloj = reloj;
    }

    public void procesar(String pedidoPor) {
        if (webhookUrl.isEmpty()) {
            throw new NoDisponibleException("El procesamiento de listas no está configurado (falta N8N_PROCESAR_LISTAS_URL).", false);
        }
        try {
            restTemplate.postForEntity(webhookUrl, java.util.Map.of("pedidoPor", pedidoPor == null ? "" : pedidoPor), String.class);
            pedidoEn = reloj.instant();
            logger.info("Procesamiento de listas de Notion disparado por {}", pedidoPor);
        } catch (RestClientException e) {
            // Sin la URL en el log: es el secreto.
            logger.warn("n8n no aceptó el pedido de procesar listas: {}: {}", e.getClass().getSimpleName(),
                    String.valueOf(e.getMessage()).replace(webhookUrl, "<webhook>"));
            throw new NoDisponibleException("n8n no respondió. Probá de nuevo en unos minutos; si sigue, revisá que el workflow esté publicado.", true);
        }
    }

    /**
     * Lo manda n8n al terminar (con el mismo login de admin que usa para cargar). Llega también
     * de las ejecuciones manuales desde el editor de n8n: vale igual, es la última corrida.
     */
    public void registrarResultado(java.util.Map<String, Object> resumen) {
        resultado = resumen == null ? java.util.Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(resumen));
        resultadoEn = reloj.instant();
        logger.info("n8n informó el resultado de las listas de Notion: {}", resultado.get("listas"));
    }

    /** procesando | sin_respuesta | terminado | sin_datos, con el último resumen si hay. */
    public java.util.Map<String, Object> estado() {
        java.time.Instant ahora = reloj.instant(), pedido = pedidoEn, fin = resultadoEn;
        var out = new java.util.LinkedHashMap<String, Object>();
        boolean esperando = pedido != null && (fin == null || fin.isBefore(pedido));
        if (esperando) {
            out.put("estado", java.time.Duration.between(pedido, ahora).compareTo(SIN_RESPUESTA) < 0 ? "procesando" : "sin_respuesta");
        } else {
            out.put("estado", fin != null ? "terminado" : "sin_datos");
        }
        out.put("pedidoEn", pedido);
        out.put("terminadoEn", fin);
        out.put("resultado", resultado);
        return out;
    }
}
