package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.api.dto.CargaJsonResponse;
import com.futuratecno.domain.Proveedor;
import com.futuratecno.infrastructure.ProveedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Kadabra (2026-10-23): su lista es una planilla de Google con una pestaña por rubro, escrita como un
 * listado de WhatsApp ("💻 MACBOOK NEO A18 PRO 13” 8GB, 256GB 6C-5C USD 835 🔥"). A diferencia de
 * Flor Qtech no hay columnas, así que cada pestaña pasa por el generador con IA (el mismo de "Cargar por
 * JSON → Texto del proveedor") y después por la carga JSON, que identifica el artículo y conserva foto,
 * categoría y margen.
 *
 * <p>Decisiones del usuario: solo las pestañas de producto nuevo (sin Liquidación, Servicio técnico,
 * Refurbished, A pedido), US$ 50 o más, y **no saca nada de la tienda**: no se sabe si la planilla es
 * todo su stock, así que lo viejo se sigue limpiando con Depurar catálogo y se puede seguir cargando por
 * JSON. La carga marca `visto_en_sync_at`, así que lo que sigue en la planilla no aparece en Depurar.
 */
@Service
public class KadabraImportService {
    private static final Logger logger = LoggerFactory.getLogger(KadabraImportService.class);
    /** Lo que no es 100% nuevo no se publica (también lo excluye el prompt del generador). */
    static final Pattern NO_NUEVO = Pattern.compile(
            "OPEN ?BOX|\\bOOO\\b|BATERIA CAMBIADA|SIN CAJA|CAJA ABIERTA|\\bDETALLE|\\bUSAD[OA]S?\\b|REACONDICIONAD|"
            + "REFURB|\\bCPO\\b|REPARAD|EXHIBICI|CLASE [ABC]\\b|SEMI ?NUEV|\\bOUTLET\\b|DESCUENTO POR");
    /** Precio por cantidad de los monopatines: se queda solo el de una unidad. */
    private static final Pattern POR_CANTIDAD = Pattern.compile("(?i)^\\s*X\\s*(\\d+)\\s+(\\d[\\d.,]*)\\s*USD.*$");
    /** El generador acepta hasta 20.000 caracteres por llamada; se deja margen. */
    private static final int MAX_CARACTERES = 18000;

    private final GenerarListadoService generador;
    private final CargaJsonService cargaJsonService;
    private final ProveedorRepository proveedorRepository;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    /** El botón del admin no espera: corre aparte y la tarjeta consulta el estado. */
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kadabra-import");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private volatile Map<String, Object> ultimo;

    @Value("${kadabra.sheet-id:}")
    private String sheetId;
    @Value("${kadabra.pestanas:}")
    private String pestanas;
    @Value("${kadabra.proveedor:Comunidad Kadabra Daro}")
    private String nombreProveedor;
    @Value("${kadabra.precio-minimo-usd:50}")
    private BigDecimal precioMinimo;

    public KadabraImportService(GenerarListadoService generador, CargaJsonService cargaJsonService,
                                ProveedorRepository proveedorRepository, ObjectMapper mapper) {
        this.generador = generador;
        this.cargaJsonService = cargaJsonService;
        this.proveedorRepository = proveedorRepository;
        this.mapper = mapper;
    }

    public boolean estaConfigurado() {
        return sheetId != null && !sheetId.isBlank() && pestanas != null && !pestanas.isBlank();
    }

    /** Arranca una corrida en segundo plano. Devuelve false si ya hay una andando. */
    public boolean iniciar() {
        if (!estaConfigurado()) throw new IllegalStateException("Falta configurar la planilla de Kadabra (KADABRA_SHEET_ID).");
        if (corriendo.get()) return false;
        ejecutor.submit(() -> {
            try { sincronizar(); } catch (Exception e) { logger.error("Kadabra: la corrida falló — {}", e.toString()); }
        });
        return true;
    }

    public Map<String, Object> estado() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configurado", estaConfigurado());
        out.put("corriendo", corriendo.get());
        out.put("ultimo", ultimo);
        return out;
    }

    /** Corrida completa. La usa el scheduler (lunes a viernes 9:00) y el botón, vía {@link #iniciar()}. */
    public Map<String, Object> sincronizar() {
        if (!corriendo.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya hay una importación de Kadabra en curso.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("inicio", LocalDateTime.now().toString());
        try {
            Proveedor proveedor = proveedorRepository.findByNombreIgnoreCase(nombreProveedor)
                    .orElseThrow(() -> new IllegalStateException("No existe el proveedor \"" + nombreProveedor + "\"."));

            List<ArticuloJsonDTO> articulos = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            int bajoMinimo = 0, noNuevos = 0, sinPrecio = 0;
            Set<Integer> yaLeidas = new HashSet<>();
            for (String pestana : pestanas.split(",")) {
                if (pestana.isBlank()) continue;
                // El nombre va TAL CUAL (algunas pestañas terminan en espacio): si no coincide exacto,
                // Google no da error, devuelve la PRIMERA pestaña, y Apple se cargaría dos veces.
                String nombre = pestana.trim();
                try {
                    String csv = descargar(pestana);
                    if (!yaLeidas.add(csv.hashCode())) {
                        throw new IllegalStateException("no la encontré en la planilla (¿le cambiaron el nombre?)");
                    }
                    String texto = texto(nombre, csv);
                    for (String parte : partes(texto)) {
                        for (Map<String, Object> a : generador.generar(parte).articulos()) {
                            ArticuloJsonDTO dto = mapper.convertValue(a, ArticuloJsonDTO.class);
                            String nombreArt = (dto.getMarca() + " " + Objects.toString(dto.getModeloExacto(), dto.getModelo()));
                            if (dto.getPrecioUsd() == null || dto.getPrecioUsd().signum() <= 0) { sinPrecio++; continue; }
                            if (dto.getPrecioUsd().compareTo(precioMinimo) < 0) { bajoMinimo++; continue; }
                            if (NO_NUEVO.matcher(FlrImportService.normalizar(nombreArt)).find()) { noNuevos++; continue; }
                            articulos.add(dto);
                        }
                    }
                } catch (Exception e) {
                    // Una pestaña que falla no frena las demás: se informa y se sigue.
                    logger.warn("Kadabra: la pestaña \"{}\" falló — {}", nombre, e.getMessage());
                    avisos.add("Pestaña " + nombre + ": " + e.getMessage());
                }
            }

            CargaJsonResponse carga = cargaJsonService.cargar(proveedor.getId(), articulos);
            String mensaje = String.format("Kadabra: %d nuevos, %d actualizados, %d en revisión. Salteados: %d por debajo de US$ %s, "
                            + "%d usados o reacondicionados, %d sin precio.%s",
                    carga.getCreados(), carga.getActualizados(), carga.getRevision(), bajoMinimo,
                    precioMinimo.stripTrailingZeros().toPlainString(), noNuevos, sinPrecio,
                    avisos.isEmpty() ? "" : " Con problemas: " + String.join(" · ", avisos));
            logger.info(mensaje);
            out.put("creados", carga.getCreados());
            out.put("actualizados", carga.getActualizados());
            out.put("revision", carga.getRevision());
            out.put("avisos", avisos);
            out.put("mensaje", mensaje);
            out.put("items", carga.getItems().stream().filter(i -> "revision".equals(i.getEstado())).toList());
        } catch (RuntimeException e) {
            out.put("error", e.getMessage());
            throw e;
        } finally {
            out.put("fin", LocalDateTime.now().toString());
            ultimo = out;
            corriendo.set(false);
        }
        return out;
    }

    private String descargar(String pestana) throws Exception {
        String url = "https://docs.google.com/spreadsheets/d/" + sheetId + "/gviz/tq?tqx=out:csv&sheet="
                + URLEncoder.encode(pestana, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) throw new IllegalStateException("la planilla respondió " + res.statusCode());
        return res.body();
    }

    /**
     * Arma el texto de una pestaña para el generador: una línea por fila, sin las filas que no son
     * producto nuevo y con el precio de UNA unidad en los monopatines ("X1 370 USD").
     */
    static String texto(String pestana, String csv) {
        StringBuilder sb = new StringBuilder("Pestaña: ").append(pestana.trim()).append('\n');
        for (List<String> fila : FlrImportService.parsearCsv(csv)) {
            String linea = String.join(" ", fila.stream().map(String::trim).filter(c -> !c.isEmpty()).toList())
                    .replace("🔥", "").replaceAll("\\s+", " ").trim();
            if (linea.isEmpty()) continue;
            var m = POR_CANTIDAD.matcher(linea);
            if (m.matches()) {
                if (!"1".equals(m.group(1))) continue;
                linea = "Precio por unidad USD " + m.group(2);
            }
            if (NO_NUEVO.matcher(FlrImportService.normalizar(linea)).find()) continue;
            sb.append(linea).append('\n');
        }
        return sb.toString();
    }

    /** Si una pestaña pasa el tope del generador, se parte entre líneas arrastrando el encabezado. */
    static List<String> partes(String texto) {
        if (texto.length() <= MAX_CARACTERES) return List.of(texto);
        String[] lineas = texto.split("\n");
        String encabezado = lineas[0];
        List<String> out = new ArrayList<>();
        StringBuilder actual = new StringBuilder(encabezado).append('\n');
        for (int i = 1; i < lineas.length; i++) {
            if (actual.length() + lineas[i].length() + 1 > MAX_CARACTERES) {
                out.add(actual.toString());
                actual = new StringBuilder(encabezado).append('\n');
            }
            actual.append(lineas[i]).append('\n');
        }
        out.add(actual.toString());
        return out;
    }
}
