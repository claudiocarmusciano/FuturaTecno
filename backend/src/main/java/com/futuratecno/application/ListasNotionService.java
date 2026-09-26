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

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public ListasNotionService(@Qualifier("n8nRestTemplate") RestTemplate restTemplate,
                               @Value("${app.n8n.procesar-listas-url:}") String webhookUrl) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }

    public void procesar(String pedidoPor) {
        if (webhookUrl.isEmpty()) {
            throw new NoDisponibleException("El procesamiento de listas no está configurado (falta N8N_PROCESAR_LISTAS_URL).", false);
        }
        try {
            restTemplate.postForEntity(webhookUrl, java.util.Map.of("pedidoPor", pedidoPor == null ? "" : pedidoPor), String.class);
            logger.info("Procesamiento de listas de Notion disparado por {}", pedidoPor);
        } catch (RestClientException e) {
            // Sin la URL en el log: es el secreto.
            logger.warn("n8n no aceptó el pedido de procesar listas: {}: {}", e.getClass().getSimpleName(),
                    String.valueOf(e.getMessage()).replace(webhookUrl, "<webhook>"));
            throw new NoDisponibleException("n8n no respondió. Probá de nuevo en unos minutos; si sigue, revisá que el workflow esté publicado.", true);
        }
    }
}
