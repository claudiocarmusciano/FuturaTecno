package com.futuratecno.application;

import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.ProveedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Flor Qtech Electronics (código FLR): su lista de precios y stock es una planilla de Google
 * publicada como CSV (la que muestra distriqtechstocklive.netlify.app). No trae códigos, así que
 * cada fila entra por la carga JSON, que identifica el artículo por marca + modelo + capacidad +
 * color y conserva foto, categoría y margen entre cargas.
 *
 * <p>Columnas: nombre, categoría, (vacía), precio x1, estado, precio x3, x5, x10. Los precios dicen
 * "$" pero son dólares; siempre se toma el de UNA unidad. La planilla es el stock completo: lo que
 * deja de aparecer sale de la tienda, y la carga lo reactiva el día que vuelve.
 */
@Service
public class FlrImportService {
    private static final Logger logger = LoggerFactory.getLogger(FlrImportService.class);
    private static final long CLAVE_BLOQUEO = 81003L;
    /** Si la planilla viene con menos filas que esto, algo salió mal: no se saca nada de la tienda. */
    private static final int MINIMO_FILAS_PARA_DAR_DE_BAJA = 20;
    /** Mismo criterio que el generador de listados: no se publican usados ni open box. */
    private static final Pattern NO_NUEVO = Pattern.compile(
            "OPEN ?BOX|\\bOOO\\b|BATERIA CAMBIADA|SIN CAJA|\\bDETALLE\\b|\\bUSAD[OA]\\b|REACONDICIONAD|REFURB|\\bCPO\\b");
    /** Lo que el proveedor antepone al nombre para decir el tipo de producto. */
    /** Prefijo de tipo → lo que queda del tipo en el nombre ("" = nada: el modelo ya lo dice). */
    private static final List<Map.Entry<String, String>> PREFIJOS_TIPO = List.of(
            Map.entry("CELULAR ", ""), Map.entry("NOTEBOOK ", ""), Map.entry("TABLET ", ""), Map.entry("IA ", ""),
            Map.entry("PERFUME KIT ", "Kit"), Map.entry("PERFUME ", ""),
            Map.entry("PARLANTE SPEAKER ", "Parlante"), Map.entry("PARLANTE ", "Parlante"), Map.entry("SPEAKER ", "Parlante"),
            Map.entry("HOME THEATER - SOUNDBAR ", "Soundbar"), Map.entry("HOME THEATER ", "Home Theater"),
            Map.entry("SOUNDBAR ", "Soundbar"), Map.entry("CONSOLA PORTATIL ", "Consola Portátil"),
            Map.entry("CONSOLA ", "Consola"), Map.entry("VOLANTE ", "Volante"), Map.entry("CAMARA ", "Cámara"),
            Map.entry("AURICULARES ", "Auriculares"), Map.entry("AURICULAR ", "Auricular"),
            Map.entry("SMARTWATCH ", "Smartwatch"), Map.entry("RELOJ ", "Reloj"), Map.entry("LAVARROPAS ", "Lavarropas"));
    private static final List<String> APPLE = List.of(
            "IPHONE", "IPAD", "AIRPODS", "AIRTAG", "MACBOOK", "IMAC", "MAC MINI", "MAC STUDIO", "APPLE WATCH");
    private static final List<String> MARCAS_DOS_PALABRAS = List.of("HARMAN KARDON", "BANG OLUFSEN", "MARSHALL MAJOR");
    private static final Map<String, String> PALABRAS = Map.ofEntries(
            Map.entry("IPHONE", "iPhone"), Map.entry("IPAD", "iPad"), Map.entry("AIRPODS", "AirPods"),
            Map.entry("AIRTAG", "AirTag"), Map.entry("MACBOOK", "MacBook"), Map.entry("IMAC", "iMac"),
            Map.entry("PLAYSTATION", "PlayStation"), Map.entry("JBL", "JBL"), Map.entry("HP", "HP"));

    private static final Set<String> SIGLAS = Set.of(
            "USB", "GPS", "SSD", "HDD", "FHD", "QHD", "UHD", "RTX", "GTX", "OLED", "LED", "LCD", "EDP", "EDT",
            "NFC", "LTE", "RGB", "DDR", "ANC", "TWS", "AMD", "SIM", "ESIM", "BT", "HD", "TV", "ML", "PC", "LG",
            "MM", "GB", "TB", "AI", "IA", "OS", "II", "III", "IV", "XL", "SE", "HP", "JBL", "MSI", "BX");

    private final CargaJsonService cargaJsonService;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${flr.csv-url:}")
    private String csvUrl;
    @Value("${flr.proveedor-id:34}")
    private Long proveedorId;
    @Value("${flr.precio-minimo-usd:50}")
    private BigDecimal precioMinimo;
    @Value("${flr.categorias-excluidas:PERFUMES}")
    private String categoriasExcluidas;

    public FlrImportService(CargaJsonService cargaJsonService, ProveedorRepository proveedorRepository,
                            ProductoRepository productoRepository) {
        this.cargaJsonService = cargaJsonService;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
    }

    public boolean estaConfigurado() {
        return csvUrl != null && !csvUrl.isBlank();
    }

    @Transactional
    public Map<String, Object> sincronizar() {
        if (!estaConfigurado()) throw new IllegalStateException("Falta FLR_CSV_URL (la planilla de Flor Qtech).");
        if (!productoRepository.intentarBloqueoImportacion(CLAVE_BLOQUEO)) {
            throw new IllegalStateException("Ya hay una importación de Flor Qtech en curso. Esperá a que termine y volvé a intentar.");
        }
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalStateException("No existe el proveedor " + proveedorId + "."));
        // Un id equivocado en otra base cargaría la lista en otro proveedor y le daría de baja todo.
        if (!"FLR".equalsIgnoreCase(proveedor.getCodigo())) {
            throw new IllegalStateException("El proveedor " + proveedorId + " no tiene el código FLR.");
        }

        List<List<String>> filas = parsearCsv(descargar());
        Set<String> excluidas = new HashSet<>();
        for (String c : categoriasExcluidas.split(",")) if (!c.isBlank()) excluidas.add(normalizar(c));

        List<ArticuloJsonDTO> articulos = new ArrayList<>();
        int sinPrecio = 0, bajoMinimo = 0, noNuevos = 0, porCategoria = 0;
        for (List<String> f : filas) {
            if (f.size() < 4 || f.get(0).isBlank()) continue;
            String nombre = f.get(0).trim().replaceAll("\\s+", " ");
            String categoria = f.get(1).trim();
            BigDecimal precio = precio(f.get(3));
            if (precio == null) { sinPrecio++; continue; }
            if (precio.compareTo(precioMinimo) < 0) { bajoMinimo++; continue; }
            if (excluidas.contains(normalizar(categoria))) { porCategoria++; continue; }
            if (NO_NUEVO.matcher(normalizar(nombre)).find()) { noNuevos++; continue; }
            articulos.add(articulo(nombre, categoria, precio));
        }

        CargaJsonResponse carga = cargaJsonService.cargar(proveedor.getId(), articulos);

        // Todo lo que la carga tocó o consideró (también lo que quedó en revisión) sigue en pie.
        Set<Long> vigentes = new HashSet<>();
        for (CargaJsonResponse.Item it : carga.getItems()) {
            if (it.getProductoId() != null) vigentes.add(it.getProductoId());
            if (it.getCandidatos() != null) vigentes.addAll(it.getCandidatos());
        }
        int dadosDeBaja = 0;
        if (filas.size() >= MINIMO_FILAS_PARA_DAR_DE_BAJA) {
            List<Long> ausentes = productoRepository.findByProveedorIdAndActivo(proveedor.getId(), true).stream()
                    .map(Producto::getId).filter(id -> !vigentes.contains(id)).toList();
            if (!ausentes.isEmpty()) dadosDeBaja = productoRepository.desactivarPorIds(ausentes);
        } else {
            logger.warn("Flor Qtech: la planilla trajo solo {} filas, no se saca nada de la tienda.", filas.size());
        }

        String mensaje = String.format("Flor Qtech: %d nuevos, %d actualizados, %d en revisión, %d sacados de la tienda "
                        + "por no estar más en la lista. Salteados: %d por debajo de US$ %s, %d usados u open box, "
                        + "%d por categoría, %d sin precio.",
                carga.getCreados(), carga.getActualizados(), carga.getRevision(), dadosDeBaja,
                bajoMinimo, precioMinimo.stripTrailingZeros().toPlainString(), noNuevos, porCategoria, sinPrecio);
        logger.info(mensaje);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("creados", carga.getCreados());
        out.put("actualizados", carga.getActualizados());
        out.put("revision", carga.getRevision());
        out.put("dadosDeBaja", dadosDeBaja);
        out.put("mensaje", mensaje);
        out.put("items", carga.getItems());
        return out;
    }

    private String descargar() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(csvUrl)).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) throw new IllegalStateException("La planilla de Flor Qtech respondió " + res.statusCode() + ".");
            return res.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la descarga de la planilla de Flor Qtech.");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo descargar la planilla de Flor Qtech: " + e.getMessage());
        }
    }

    static ArticuloJsonDTO articulo(String nombre, String categoria, BigDecimal precio) {
        String n = normalizar(nombre);
        String original = nombre;
        String tipo = "";
        for (Map.Entry<String, String> p : PREFIJOS_TIPO) {
            if (n.startsWith(p.getKey()) && n.length() > p.getKey().length()) {
                n = n.substring(p.getKey().length());
                tipo = p.getValue();
                original = original.substring(original.length() - n.length());
                break;
            }
        }
        final String sinTipo = n;
        String marca, modelo;
        if (APPLE.stream().anyMatch(sinTipo::startsWith)) {
            marca = "Apple";
            modelo = n.startsWith("APPLE ") ? original.substring(6) : original;
        } else {
            String dos = MARCAS_DOS_PALABRAS.stream().filter(m -> sinTipo.startsWith(m + " ")).findFirst().orElse(null);
            int corte = dos != null ? dos.length() : (n.indexOf(' ') > 0 ? n.indexOf(' ') : n.length());
            marca = titulo(original.substring(0, corte));
            modelo = original.substring(corte).trim();
            if (modelo.isEmpty()) modelo = original;
        }
        ArticuloJsonDTO a = new ArticuloJsonDTO();
        a.setListado(nombre);
        a.setMarca(marca);
        a.setModelo(tipo.isEmpty() ? titulo(modelo) : tipo + " " + titulo(modelo));
        a.setPrecioUsd(precio);
        a.setFuente("FLR");
        a.setCategoria(titulo(categoria));
        return a;
    }

    /** "S25 ULTRA 12/256GB BLACK" → "S25 Ultra 12/256GB Black": lo que tiene números o es una sigla corta queda igual. */
    static String titulo(String s) {
        StringBuilder out = new StringBuilder();
        for (String w : s.trim().split("\\s+")) {
            if (out.length() > 0) out.append(' ');
            String clave = normalizar(w);
            if (PALABRAS.containsKey(clave)) out.append(PALABRAS.get(clave));
            else if (!w.matches("\\p{L}+") || SIGLAS.contains(clave)
                    || !w.equals(w.toUpperCase(Locale.ROOT))) out.append(w);
            else out.append(w.charAt(0)).append(w.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    /** "805", "$805", "1.030", "Sin precio" → 805, 805, 1030, null. */
    static BigDecimal precio(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[^0-9.,]", "");
        if (t.isEmpty()) return null;
        // Sin centavos en esta lista: un punto o coma seguido de 3 dígitos es separador de miles.
        if (t.matches("\\d{1,3}([.,]\\d{3})+")) t = t.replaceAll("[.,]", "");
        else t = t.replace(",", ".");
        try {
            BigDecimal v = new BigDecimal(t);
            return v.signum() > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** CSV con comillas ("" adentro de un campo entre comillas es una comilla). */
    static List<List<String>> parsearCsv(String texto) {
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (comillas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') { campo.append('"'); i++; }
                else if (c == '"') comillas = false;
                else campo.append(c);
            } else if (c == '"') comillas = true;
            else if (c == ',') { fila.add(campo.toString()); campo.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                fila.add(campo.toString()); campo.setLength(0);
                filas.add(fila); fila = new ArrayList<>();
            } else campo.append(c);
        }
        if (campo.length() > 0 || !fila.isEmpty()) { fila.add(campo.toString()); filas.add(fila); }
        return filas;
    }
}
