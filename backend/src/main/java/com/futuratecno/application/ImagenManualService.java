package com.futuratecno.application;

import com.futuratecno.api.dto.ImagenSimilarDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Memoria compartida entre proveedores. No elimina colores, capacidades ni nombres de combos. */
@Service
public class ImagenManualService {
    private final JdbcTemplate jdbc;

    public ImagenManualService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    static String normalizar(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Clave tolerante a la redacción: minúsculas y solo letras y números. Absorbe las diferencias
     * con las que la IA escribe el mismo artículo en cada carga ("256GB" / "256 GB" / "(eSIM)")
     * sin borrar las que distinguen de verdad: 256gb ≠ 512gb, black ≠ white.
     *
     * <p>Replica exactamente la expresión de la columna generada `productos.clave_suelta` (V37).
     * Si se cambia una hay que cambiar la otra o las búsquedas dejan de encontrar en silencio.
     */
    static String claveSuelta(String texto) {
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Imagen conocida para esa marca+modelo, de la fuente más confiable a la menos.
     *
     * <p>La tercera fuente es el catálogo en vivo, y es la que más ahorra: las dos tablas de
     * memoria solo se escriben cuando el admin edita una imagen, cuando el generador encuentra
     * una buscando, o cuando se da de baja un producto. Un artículo que entró con foto desde Elit,
     * Invid o una carga JSON **no** queda registrado ahí, así que el generador salía a buscar —y a
     * gastar crédito— por una imagen que ya estaba en la base. Al 2026-09-14 eso eran 3.215
     * productos, el 98% del catálogo.
     *
     * <p>Entre productos gana el activo: si el mismo marca+modelo existe publicado y dado de baja,
     * la foto del publicado es la que el cliente está viendo hoy.
     */
    public Optional<String> buscar(String marca, String modelo) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return Optional.empty();
        return jdbc.query("""
                SELECT url FROM (
                    SELECT url, 0 prioridad FROM imagenes_manuales WHERE marca = ? AND modelo = ?
                    UNION ALL
                    SELECT url, 1 prioridad FROM imagenes_automaticas WHERE marca = ? AND modelo = ?
                    UNION ALL
                    SELECT imagen_url AS url, CASE WHEN activo THEN 2 ELSE 3 END AS prioridad
                    FROM productos
                    WHERE lower(regexp_replace(trim(marca), '\\s+', ' ', 'g')) = ?
                      AND lower(regexp_replace(trim(modelo), '\\s+', ' ', 'g')) = ?
                      AND nullif(trim(imagen_url), '') IS NOT NULL
                ) memoria ORDER BY prioridad LIMIT 1
                """,
                (rs, row) -> rs.getString("url"), m, mod, m, mod, m, mod)
                .stream().findFirst()
                .or(() -> buscarSuelto(marca, modelo));
    }

    /** La primera imagen conocida para cualquiera de esos nombres, en orden (ver {@link NombreArticulo}). */
    public Optional<String> buscar(List<NombreArticulo> nombres) {
        for (NombreArticulo n : NombreArticulo.distintos(nombres)) {
            Optional<String> url = buscar(n.marca(), n.modelo());
            if (url.isPresent()) return url;
        }
        return Optional.empty();
    }

    /**
     * Último intento antes de salir a pagar una búsqueda: el mismo artículo cargado ayer, con el
     * modelo redactado apenas distinto. Solo mira el catálogo, que es donde están las imágenes
     * (98% de los productos tiene foto) y lo único indexado por clave suelta.
     *
     * <p>Se prueba también con la marca sacada del modelo, porque la IA a veces la repite:
     * "Apple" + "Apple Pencil Pro" y "Apple" + "Pencil Pro" son el mismo lápiz.
     *
     * <p>El riesgo de aflojar la clave es traer la imagen de otro artículo parecido. Se acepta
     * porque acá lo único que se decide es QUÉ FOTO mostrar —corregible desde Admin → Imágenes—,
     * nunca si dos productos son el mismo ni qué precio tienen.
     */
    private Optional<String> buscarSuelto(String marca, String modelo) {
        String clave = clave(marca, modelo);
        if (claveSuelta(marca).isEmpty() || claveSuelta(modelo).isEmpty()) return Optional.empty();
        return jdbc.query("""
                SELECT imagen_url FROM productos
                WHERE clave_suelta = ?
                  AND nullif(trim(imagen_url), '') IS NOT NULL
                ORDER BY activo DESC, id DESC LIMIT 1
                """, (rs, row) -> rs.getString("imagen_url"), clave)
                .stream().findFirst();
    }

    /**
     * Candidatas de la propia base para un producto sin foto, para elegir a mano antes de salir a
     * buscar afuera. No pega a ninguna API y no guarda nada: solo propone.
     *
     * <p>Mira únicamente productos de la MISMA marca. Aflojar eso traería la funda de Spigen para
     * el iPhone, y el error de esta pantalla no es quedarse corto sino ofrecer una foto ajena que
     * después queda recordada por marca+modelo.
     *
     * <p>Ordena por cuánto comparten las claves sueltas (V37) desde el principio: el modelo
     * arranca con la línea ("legion5", "swiftgo") y termina en lo que distingue (capacidad, color),
     * así que a más prefijo en común, más cerca. El match exacto de clave va primero y se marca
     * como tal. Se deduplica por URL porque una familia entera suele compartir un mismo render.
     */
    public List<ImagenSimilarDTO> similares(String marca, String modelo, int limite) {
        String objetivo = clave(marca, modelo);
        String m = claveSuelta(marca);
        if (m.isEmpty() || objetivo.isEmpty()) return List.of();

        var candidatas = jdbc.query("""
                SELECT id, marca, modelo, imagen_url, activo, clave_suelta
                FROM productos
                WHERE regexp_replace(lower(marca), '[^a-z0-9]', '', 'g') = ?
                  AND nullif(trim(imagen_url), '') IS NOT NULL
                """,
                (rs, row) -> new Object[]{
                        rs.getLong("id"), rs.getString("marca"), rs.getString("modelo"),
                        rs.getString("imagen_url"), rs.getBoolean("activo"), rs.getString("clave_suelta")},
                m);

        // Piso deliberado. Sin él, un producto sin ningún pariente igual devuelve lo primero que
        // comparta la marca y lo ofrece con la misma cara que una coincidencia buena: medido el
        // 2026-09-18, "Dell LDC16255" traía "Dell LDC15255-A117", que es otra notebook. Se exige
        // que coincidan al menos 6 caracteres del modelo, no solo la marca.
        int minimo = m.length() + 6;

        // Entre dos candidatas con la misma foto gana la más parecida, y a igual parecido la
        // publicada: es la que el cliente está viendo hoy, así que es la que ya se validó sola.
        var porUrl = new LinkedHashMap<String, ImagenSimilarDTO>();
        candidatas.stream()
                .map(c -> Map.entry(prefijoComun((String) c[5], objetivo), c))
                .filter(e -> e.getKey() >= minimo)
                .sorted(Comparator.<Map.Entry<Integer, Object[]>>comparingInt(e -> -e.getKey())
                        .thenComparing(e -> !((Boolean) e.getValue()[4]))
                        .thenComparing(e -> -((Long) e.getValue()[0])))
                .map(e -> { Object[] c = e.getValue();
                    return new ImagenSimilarDTO((Long) c[0], (String) c[1], (String) c[2],
                            (String) c[3], (Boolean) c[4], objetivo.equals(c[5]),
                            e.getKey() - m.length()); })
                .forEach(dto -> porUrl.putIfAbsent(dto.url(), dto));

        return porUrl.values().stream().limit(limite).toList();
    }

    /**
     * Lo que NO cambia la foto de un artículo: capacidad y RAM ("256GB", "12/512GB"), teclado,
     * conectividad (+Cell, WiFi, LTE) y núcleos ("10C-10C"). Un iMac M4 de 256GB y uno de 1TB se
     * ven igual; un iPhone 17 Pro y un 17 Pro Max no, y eso queda en la clave.
     */
    private static final Pattern NO_CAMBIA_LA_FOTO = Pattern.compile(
            "\\b\\d+\\s*/\\s*\\d+\\s*(?:GB|TB)?\\b"
            + "|\\b\\d+(?:[.,]\\d+)?\\s*(?:GB|TB)\\b"
            + "|\\bRAM\\b|\\bUNIFIED\\s+MEMORY\\b"
            + "|\\bTECLADO\\s+(?:EN\\s+)?(?:ESPANOLA?|INGLES|LATINO|US)\\b"
            + "|\\+\\s*CELL(?:ULAR)?\\b|\\bCELL(?:ULAR)?\\b|\\bWI-?FI\\b|\\bLTE\\b"
            + "|\\b\\d+C\\s*-\\s*\\d+C\\b");

    /** Colores que sirven de foto "genérica" cuando el artículo no dice el suyo, del más neutro al menos. */
    private static final List<Set<String>> NEUTROS = List.of(
            Set.of("gris", "plata", "titanio", "titanio-natural"),
            Set.of("negro", "titanio-negro", "medianoche"));

    /**
     * Marca + modelo sin color ni lo que no cambia la foto, en clave suelta. Vacío si no queda
     * nada que identifique la línea: una clave de 3 letras juntaría artículos distintos.
     */
    static String claveFamilia(String marca, String modelo) {
        String sinVariantes = NO_CAMBIA_LA_FOTO.matcher(IdentidadProductoService.sinColores(modelo)).replaceAll(" ");
        String k = claveSuelta(sinVariantes), m = claveSuelta(marca);
        if (!m.isEmpty() && k.startsWith(m) && k.length() > m.length()) k = k.substring(m.length());
        return k.length() < 4 ? "" : k;
    }

    /** Un producto con foto de la misma marca y familia, con los colores que dice su nombre. */
    record ParienteConFoto(long id, String url, boolean activo, Set<String> colores) {}

    /**
     * Elige la foto de un pariente con estas reglas (acordadas el 2026-09-26):
     * <ul>
     *   <li>Si el artículo dice UN color, solo sirve un pariente de ese mismo color: mostrar un
     *       iPhone plateado en la ficha del naranja es mostrar otro producto.</li>
     *   <li>Si no dice color (o lista varios, que son los disponibles y no el suyo), se prefiere
     *       gris/plata, después negro, después un pariente sin color y al final cualquiera.</li>
     * </ul>
     * A igual preferencia, el publicado y el más nuevo.
     */
    static Optional<String> elegirPariente(Set<String> coloresArticulo, List<ParienteConFoto> parientes) {
        Comparator<ParienteConFoto> publicadoYNuevo = Comparator
                .comparing((ParienteConFoto p) -> !p.activo()).thenComparing(p -> -p.id());
        if (coloresArticulo.size() == 1) {
            return parientes.stream().filter(p -> p.colores().equals(coloresArticulo))
                    .sorted(publicadoYNuevo).map(ParienteConFoto::url).findFirst();
        }
        return parientes.stream()
                .sorted(Comparator.comparingInt(ImagenManualService::preferencia).thenComparing(publicadoYNuevo))
                .map(ParienteConFoto::url).findFirst();
    }

    /**
     * Último recurso antes de dejar un artículo sin foto: la de un pariente del catálogo que solo
     * cambia en capacidad, RAM, teclado o conectividad (ver {@link #elegirPariente}). No se guarda
     * en la memoria de imágenes: es una foto prestada, y una propia que aparezca después tiene
     * que poder reemplazarla sin chocar con un recuerdo.
     */
    public Optional<String> buscarPorFamilia(String marca, String modelo, String especificaciones) {
        String familia = claveFamilia(marca, modelo);
        String m = claveSuelta(marca);
        if (familia.isEmpty() || m.isEmpty()) return Optional.empty();
        List<ParienteConFoto> parientes = jdbc.query("""
                SELECT id, modelo, imagen_url, activo FROM productos
                WHERE regexp_replace(lower(marca), '[^a-z0-9]', '', 'g') = ?
                  AND nullif(trim(imagen_url), '') IS NOT NULL
                """,
                (rs, row) -> new Object[]{rs.getLong("id"), rs.getString("modelo"), rs.getString("imagen_url"), rs.getBoolean("activo")},
                m).stream()
                .filter(c -> familia.equals(claveFamilia(marca, (String) c[1])))
                .map(c -> new ParienteConFoto((Long) c[0], (String) c[2], (Boolean) c[3],
                        IdentidadProductoService.coloresDe((String) c[1])))
                .toList();
        String texto = (modelo == null ? "" : modelo) + " · " + (especificaciones == null ? "" : especificaciones);
        return elegirPariente(IdentidadProductoService.coloresDe(texto), parientes);
    }

    /** Cuántos caracteres comparten dos claves sueltas desde el principio. */
    private static int prefijoComun(String a, String b) {
        if (a == null || b == null) return 0;
        int n = Math.min(a.length(), b.length()), i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    /**
     * Misma clave que calcula la columna generada `productos.clave_suelta` (V37). La marca se saca
     * del modelo si viene repetida ("Apple" + "Apple Pencil Pro"), y se hace de los dos lados para
     * que dé igual cuál redacción esté guardada y cuál esté entrando.
     */
    static String clave(String marca, String modelo) {
        String m = claveSuelta(marca), mod = claveSuelta(modelo);
        return m + (mod.startsWith(m) ? mod.substring(m.length()) : mod);
    }

    public void guardarAutomatica(String marca, String modelo, String url) {
        if (normalizar(marca).isEmpty() || normalizar(modelo).isEmpty() || url == null || url.isBlank()) return;
        jdbc.update("""
                INSERT INTO imagenes_automaticas (marca, modelo, url) VALUES (?, ?, ?)
                ON CONFLICT (marca, modelo) DO NOTHING
                """, normalizar(marca), normalizar(modelo), url.trim());
    }

    /**
     * Borra una URL de las dos memorias, bajo cualquier nombre. Se usa cuando la foto recordada da
     * 404/410: seguir guardándola la repartiría rota en cada carga futura del artículo.
     */
    public int olvidarUrl(String url) {
        if (url == null || url.isBlank()) return 0;
        return jdbc.update("DELETE FROM imagenes_automaticas WHERE url = ?", url.trim())
                + jdbc.update("DELETE FROM imagenes_manuales WHERE url = ?", url.trim());
    }

    public void guardar(String marca, String modelo, String url) {
        String m = normalizar(marca), mod = normalizar(modelo);
        if (m.isEmpty() || mod.isEmpty()) return;
        if (url == null || url.isBlank()) {
            jdbc.update("DELETE FROM imagenes_automaticas WHERE marca = ? AND modelo = ?", m, mod);
            jdbc.update("DELETE FROM imagenes_manuales WHERE marca = ? AND modelo = ?", m, mod);
        } else {
            jdbc.update("""
                    INSERT INTO imagenes_manuales (marca, modelo, url) VALUES (?, ?, ?)
                    ON CONFLICT (marca, modelo) DO UPDATE SET url = EXCLUDED.url
                    """, m, mod, url.trim());
        }
    }

    private static int preferencia(ParienteConFoto p) {
        if (p.colores().size() == 1) {
            for (int i = 0; i < NEUTROS.size(); i++) if (NEUTROS.get(i).containsAll(p.colores())) return i;
        }
        return p.colores().isEmpty() ? NEUTROS.size() : NEUTROS.size() + 1;
    }
}
