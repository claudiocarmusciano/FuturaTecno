package com.futuratecno.api;

import com.futuratecno.api.dto.ProductoCatalogoDTO;
import com.futuratecno.api.dto.VarianteCatalogoDTO;
import com.futuratecno.application.CatalogoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que necesitan Google y las vistas previas de links (WhatsApp, Instagram), que no ejecutan el
 * JavaScript de la tienda: el sitemap y las páginas de producto con su título, descripción e
 * imagen ya escritos en el HTML. El resto de la página la sigue armando React como siempre.
 */
@RestController
public class SeoController {

    private static final Logger log = LoggerFactory.getLogger(SeoController.class);

    /** Origen público del sitio (APP_PUBLIC_URL), sin barra final. */
    private final String sitio;
    /** Bloque de index.html que se reemplaza en cada producto (ver el comentario en index.html). */
    private static final Pattern BLOQUE_SEO = Pattern.compile("<!-- seo:.*?<!-- /seo -->", Pattern.DOTALL);
    private static final List<String> PAGINAS = List.of(
            "/", "/catalogo", "/arma-tu-pc", "/garantia", "/terminos", "/privacidad", "/arrepentimiento");

    private final CatalogoService catalogoService;

    public SeoController(CatalogoService catalogoService,
                         @org.springframework.beans.factory.annotation.Value("${app.public-url:https://www.tecnopolisolavarria.com}") String publicUrl) {
        this.catalogoService = catalogoService;
        this.sitio = publicUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String pagina : PAGINAS) {
            xml.append("  <url><loc>").append(sitio).append(pagina).append("</loc></url>\n");
        }
        DateTimeFormatter dia = DateTimeFormatter.ISO_LOCAL_DATE;
        for (ProductoCatalogoDTO p : catalogoService.listarCatalogo()) {
            xml.append("  <url><loc>").append(sitio).append("/producto/").append(p.getId()).append("</loc>");
            if (p.getUltimaActualizacion() != null) {
                xml.append("<lastmod>").append(p.getUltimaActualizacion().format(dia)).append("</lastmod>");
            }
            xml.append("</url>\n");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(xml.toString());
    }

    /**
     * La página de un producto: el mismo index.html de la tienda con el bloque SEO del producto. Si
     * el producto no existe o está dado de baja responde 404 (React muestra igual su aviso): con un
     * 200, Google indexaría una página vacía por cada producto viejo.
     */
    @GetMapping(value = "/producto/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> producto(@PathVariable String id) {
        String html = indexHtml();
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        ProductoCatalogoDTO p = null;
        try {
            p = catalogoService.obtenerProducto(Long.valueOf(id));
        } catch (IllegalArgumentException e) {
            // id inválido, dado de baja o sin variantes: va el 404 de abajo.
        } catch (RuntimeException e) {
            // Un error del catálogo no tiene que dejar al cliente sin la página: va sin SEO.
            log.warn("SEO del producto {}: {}", id, e.getMessage());
            return ResponseEntity.ok().body(html);
        }
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML).body(html);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(BLOQUE_SEO.matcher(html).replaceFirst(Matcher.quoteReplacement(bloqueProducto(p, sitio))));
    }

    static String bloqueProducto(ProductoCatalogoDTO p, String sitio) {
        String nombre = nombre(p);
        String url = sitio + "/producto/" + p.getId();
        BigDecimal precio = p.getVariantes() == null ? null : p.getVariantes().stream()
                .map(VarianteCatalogoDTO::getPrecioArs).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        String precioTexto = precio == null ? "" : " $ " + String.format(java.util.Locale.of("es", "AR"), "%,d",
                precio.setScale(0, RoundingMode.HALF_UP).longValue()) + ".";
        String descripcion = nombre + " en Tecnópolis Olavarría." + precioTexto
                + " Envío a todo el país o retiro en San Martín 2821.";
        String imagen = p.getImagenUrl() != null && p.getImagenUrl().startsWith("http")
                ? p.getImagenUrl() : sitio + "/og-image.png";

        StringBuilder b = new StringBuilder();
        b.append("<title>").append(esc(nombre)).append(" | Tecnópolis Olavarría</title>\n");
        b.append("    <meta name=\"description\" content=\"").append(esc(descripcion)).append("\" />\n");
        b.append("    <link rel=\"canonical\" href=\"").append(url).append("\" />\n");
        b.append("    <meta property=\"og:type\" content=\"product\" />\n");
        b.append("    <meta property=\"og:site_name\" content=\"Tecnópolis Olavarría\" />\n");
        b.append("    <meta property=\"og:title\" content=\"").append(esc(nombre)).append("\" />\n");
        b.append("    <meta property=\"og:description\" content=\"").append(esc(descripcion)).append("\" />\n");
        b.append("    <meta property=\"og:url\" content=\"").append(url).append("\" />\n");
        b.append("    <meta property=\"og:image\" content=\"").append(esc(imagen)).append("\" />\n");
        b.append("    <meta property=\"og:locale\" content=\"es_AR\" />\n");
        b.append("    <meta name=\"twitter:card\" content=\"summary_large_image\" />\n");
        // Datos estructurados: con esto Google puede mostrar precio y disponibilidad en el resultado.
        b.append("    <script type=\"application/ld+json\">{\"@context\":\"https://schema.org\",\"@type\":\"Product\"")
                .append(",\"name\":\"").append(json(nombre)).append("\"")
                .append(",\"image\":\"").append(json(imagen)).append("\"");
        if (p.getMarca() != null) {
            b.append(",\"brand\":{\"@type\":\"Brand\",\"name\":\"").append(json(p.getMarca())).append("\"}");
        }
        if (precio != null) {
            b.append(",\"offers\":{\"@type\":\"Offer\",\"priceCurrency\":\"ARS\",\"price\":\"")
                    .append(precio.setScale(2, RoundingMode.HALF_UP).toPlainString())
                    .append("\",\"availability\":\"https://schema.org/InStock\",\"url\":\"").append(url).append("\"}");
        }
        b.append("}</script>");
        return b.toString();
    }

    /** "Apple iPhone 17 Pro 256GB", sin repetir la marca si el modelo ya empieza con ella. */
    static String nombre(ProductoCatalogoDTO p) {
        String modelo = p.getModelo() == null ? "" : p.getModelo().trim();
        String marca = p.getMarca() == null ? "" : p.getMarca().trim();
        if (marca.isEmpty() || modelo.toLowerCase().startsWith(marca.toLowerCase())) {
            return modelo;
        }
        return marca + " " + modelo;
    }

    private String indexHtml() {
        try {
            return new String(new ClassPathResource("/static/index.html").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;   // dev local sin el frontend buildeado
        }
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String json(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c").replace("\n", " ");
    }
}
