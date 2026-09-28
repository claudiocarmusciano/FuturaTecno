package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.domain.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compara Jev contra Claude en el paso 5 del clasificador (lo que el clasificador por nombre no
 * resuelve), sobre productos que ya tienen categoría: esa es la respuesta correcta. Gasta API real,
 * así que no corre en la suite normal. Las claves salen del entorno o de backend/.env.
 * <pre>
 * mvn test -Dtest=ClasificacionIaCatalogoRealTest -Dcatalogo=/ruta/catalogo.json \
 *   -Dcategorias=/ruta/categorias.json [-Dmuestra=60]
 * </pre>
 * catalogo: [{marca, modelo, actual: "Padre > Hoja", tieneCat}]; categorias: el GET /api/categorias.
 */
@EnabledIfSystemProperty(named = "categorias", matches = ".+")
class ClasificacionIaCatalogoRealTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void compararJevConClaude() throws Exception {
        List<String> paths = new ArrayList<>();
        for (JsonNode n : mapper.readTree(new File(System.getProperty("categorias")))) hojas(n, "", paths);
        List<String> opciones = CategoriaClasificadorService.opcionesJev(paths);

        ClasificadorPorNombre porNombre = new ClasificadorPorNombre();
        List<Producto> muestra = new ArrayList<>();
        List<String> esperados = new ArrayList<>();
        int max = Integer.getInteger("muestra", 60);
        // -Dvariada: hasta 3 por categoría e incluye los que el clasificador por nombre ya resuelve,
        // para no medir solo lo fácil (en la base local, lo que no resuelve son casi todos procesadores).
        boolean variada = Boolean.getBoolean("variada");
        Map<String, Integer> porCategoria = new java.util.HashMap<>();
        for (JsonNode n : mapper.readTree(new File(System.getProperty("catalogo")))) {
            if (!n.path("tieneCat").asBoolean() || muestra.size() >= max) continue;
            Producto p = new Producto();
            p.setMarca(n.path("marca").asText(null));
            p.setModelo(n.path("modelo").asText(null));
            if (variada) {
                if (porCategoria.merge(n.path("actual").asText(), 1, Integer::sum) > 3) continue;
            } else if (porNombre.clasificar(p) != null) continue;   // no llegaría a la IA
            muestra.add(p);
            esperados.add(norm(n.path("actual").asText()));
        }

        JevClient jev = new JevClient();
        ReflectionTestUtils.setField(jev, "apiKey", clave("TYPESAFE_API_KEY"));
        ReflectionTestUtils.setField(jev, "model", "jev-latest");
        String claveClaude = clave("ANTHROPIC_API_KEY");

        List<double[]> jevRes = new ArrayList<>();   // {acierto 0/1, confianza}
        int claudeOk = 0, claudeResp = 0;
        long msJev = 0, msClaude = 0;
        for (int i = 0; i < muestra.size(); i++) {
            Producto p = muestra.get(i);
            long t = System.currentTimeMillis();
            JevClient.Eleccion e = jev.elegir(CategoriaClasificadorService.estadoJev(p, null),
                    CategoriaClasificadorService.INSTRUCCION_JEV, opciones);
            msJev += System.currentTimeMillis() - t;
            String j = e == null ? null : e.opcion();
            if (e != null) jevRes.add(new double[]{esperados.get(i).equals(j) ? 1 : 0, e.confianza()});

            String c = null;
            if (claveClaude != null) {
                t = System.currentTimeMillis();
                c = claude(claveClaude, p, paths);
                msClaude += System.currentTimeMillis() - t;
                if (c != null) { claudeResp++; if (esperados.get(i).equals(c)) claudeOk++; }
            }
            System.out.printf("%-45.45s | real %s | jev %s %.2f | claude %s%n", p.getMarca() + " " + p.getModelo(),
                    esperados.get(i), j, e == null ? 0 : e.confianza(), c);
        }

        System.out.printf("%nMuestra: %d productos (%s)%n", muestra.size(), variada ? "hasta 3 por categoría" : "los que el clasificador por nombre no resuelve");
        System.out.printf("Claude: %d/%d aciertos (%d respondió), %d ms promedio%n", claudeOk, muestra.size(), claudeResp,
                muestra.isEmpty() ? 0 : msClaude / muestra.size());
        System.out.printf("Jev:    %d respondió, %d ms promedio%n", jevRes.size(), muestra.isEmpty() ? 0 : msJev / muestra.size());
        for (double umbral : new double[]{0, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9}) {
            long usados = jevRes.stream().filter(r -> r[1] >= umbral).count();
            long ok = jevRes.stream().filter(r -> r[1] >= umbral && r[0] == 1).count();
            System.out.printf("  umbral %.1f: asigna %d/%d, acierta %d (precisión %d%%), deja en null %d%n", umbral, usados,
                    muestra.size(), ok, usados == 0 ? 0 : ok * 100 / usados, muestra.size() - usados);
        }
    }

    private static void hojas(JsonNode n, String padre, List<String> out) {
        String path = padre.isEmpty() ? n.path("nombre").asText() : padre + " > " + n.path("nombre").asText();
        if (n.path("hijos").isEmpty()) out.add(norm(path));
        else for (JsonNode h : n.path("hijos")) hojas(h, path, out);
    }

    /** Igual que CategoriaService#normalizarPath: así se le pasan las opciones en producción. */
    private static String norm(String s) {
        return s.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private static String clave(String nombre) throws Exception {
        String env = System.getenv(nombre);
        if (env != null && !env.isBlank()) return env;
        Path f = Path.of(".env");
        if (!Files.exists(f)) f = Path.of("backend/.env");
        if (!Files.exists(f)) return null;
        return Files.readAllLines(f).stream().filter(l -> l.startsWith(nombre + "="))
                .map(l -> l.substring(nombre.length() + 1).trim().replaceAll("^\"|\"$", ""))
                .filter(v -> !v.isBlank()).findFirst().orElse(null);
    }

    private String claude(String key, Producto p, List<String> paths) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("x-api-key", key);
            h.set("anthropic-version", "2023-06-01");
            String prompt = CategoriaClasificadorService.promptClaude(p.getMarca() + " " + p.getModelo(), paths);
            Map<String, Object> body = Map.of("model", "claude-haiku-4-5-20251001", "max_tokens", 200,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));
            JsonNode r = new RestTemplate().postForObject("https://api.anthropic.com/v1/messages", new HttpEntity<>(body, h), JsonNode.class);
            return r == null ? null : norm(r.path("content").path(0).path("text").asText());
        } catch (Exception e) {
            return null;
        }
    }
}
