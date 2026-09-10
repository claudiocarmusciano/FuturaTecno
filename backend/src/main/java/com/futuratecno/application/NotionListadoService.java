package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
public class NotionListadoService {
    private final RestTemplate http;
    private final String token;
    private final String pageId;
    private static final Set<String> OMITIR = Set.of("image", "divider", "bookmark", "embed", "link_preview", "table_of_contents");

    public NotionListadoService(RestTemplateBuilder builder,
            @Value("${notion.token:}") String token, @Value("${notion.page-id:}") String pageId) {
        this.http = builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(8)).build();
        this.token = token.trim();
        this.pageId = pageId.trim();
    }

    public String leer() {
        if (token.isBlank() || pageId.isBlank())
            throw new IllegalStateException("Configurá NOTION_TOKEN y NOTION_PAGE_ID en el servidor y compartí la página con esa conexión.");
        String id;
        try { id = UUID.fromString(pageId.replaceFirst("^([a-fA-F0-9]{8})([a-fA-F0-9]{4})([a-fA-F0-9]{4})([a-fA-F0-9]{4})([a-fA-F0-9]{12})$", "$1-$2-$3-$4-$5")).toString(); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("NOTION_PAGE_ID debe contener el ID de la página."); }
        var lectura = new Lectura();
        try { hijos(id, lectura, 0); }
        catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 401) throw new IllegalStateException("La clave de Notion no es válida. Revisá NOTION_TOKEN.");
            if (status == 403 || status == 404) throw new IllegalStateException("No se puede leer la página. Revisá NOTION_PAGE_ID y compartila con la conexión del servidor.");
            if (status == 429) throw new IllegalStateException("Notion alcanzó su límite de solicitudes. Esperá un momento y reintentá.");
            throw new IllegalStateException("Notion no pudo responder. Reintentá más tarde.");
        }
        String texto = lectura.texto.toString().trim();
        if (texto.isBlank()) throw new IllegalArgumentException("La página de Notion está vacía o no contiene texto. Pegá el listado y reintentá.");
        return texto;
    }

    private void hijos(String id, Lectura lectura, int profundidad) {
        if (profundidad > 10) throw new IllegalArgumentException("La página tiene demasiados niveles. Pegá el listado como texto simple.");
        String cursor = null;
        do {
            if (++lectura.solicitudes > 30 || System.nanoTime() > lectura.limite)
                throw new IllegalArgumentException("La página es demasiado compleja. Dividí el listado en cargas más pequeñas.");
            var uri = UriComponentsBuilder.fromHttpUrl("https://api.notion.com/v1/blocks/" + id + "/children").queryParam("page_size", 100);
            if (cursor != null) uri.queryParam("start_cursor", cursor);
            var headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Notion-Version", "2022-06-28");
            JsonNode respuesta = http.exchange(uri.build().encode().toUri(), HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
            if (respuesta == null || !respuesta.path("results").isArray()) throw new IllegalStateException("Notion devolvió una respuesta incompleta. Reintentá.");
            for (JsonNode bloque : respuesta.path("results")) {
                String tipo = bloque.path("type").asText();
                JsonNode contenido = bloque.path(tipo);
                if (contenido.has("rich_text")) linea(contenido.path("rich_text"), lectura);
                else if (tipo.equals("table_row")) {
                    for (JsonNode celda : contenido.path("cells")) {
                        linea(celda, lectura);
                        lectura.texto.setCharAt(lectura.texto.length() - 1, '\t');
                    }
                    lectura.texto.append('\n');
                } else if (!OMITIR.contains(tipo) && !Set.of("column_list", "column", "table").contains(tipo)) {
                    throw new IllegalArgumentException("La página contiene bloques no compatibles (" + tipo + "). Pegá el listado como texto, sin subpáginas ni archivos.");
                }
                if (bloque.path("has_children").asBoolean()) hijos(bloque.path("id").asText(), lectura, profundidad + 1);
            }
            if (!respuesta.path("has_more").asBoolean()) break;
            String siguiente = respuesta.path("next_cursor").asText("");
            if (siguiente.isBlank() || siguiente.equals(cursor)) throw new IllegalStateException("Notion devolvió una paginación incompleta. Reintentá.");
            cursor = siguiente;
        } while (true);
    }

    private void linea(JsonNode fragmentos, Lectura lectura) {
        for (JsonNode parte : fragmentos) lectura.texto.append(parte.path("plain_text").asText(parte.path("text").path("content").asText("")));
        lectura.texto.append('\n');
        if (lectura.texto.length() > 20000) throw new IllegalArgumentException("El listado supera los 20.000 caracteres. Dividilo en cargas más pequeñas; no se generó un listado parcial.");
    }

    private static class Lectura {
        final StringBuilder texto = new StringBuilder();
        final long limite = System.nanoTime() + Duration.ofSeconds(40).toNanos();
        int solicitudes;
    }
}
