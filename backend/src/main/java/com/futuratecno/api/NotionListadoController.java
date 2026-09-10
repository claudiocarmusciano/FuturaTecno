package com.futuratecno.api;

import com.futuratecno.application.NotionListadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/carga-json/notion")
public class NotionListadoController {
    private final NotionListadoService notion;
    public NotionListadoController(NotionListadoService notion) { this.notion = notion; }

    @GetMapping
    public ResponseEntity<?> leer() {
        try { return ResponseEntity.ok().header("Cache-Control", "no-store").body(Map.of("texto", notion.leer())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (IllegalStateException e) { return ResponseEntity.status(503).body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.status(502).body(Map.of("error", "No se pudo leer Notion. Revisá la conexión y reintentá.")); }
    }
}
