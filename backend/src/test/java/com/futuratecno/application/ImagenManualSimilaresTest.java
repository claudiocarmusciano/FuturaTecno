package com.futuratecno.application;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Candidatas de imagen sacadas del propio catálogo (Admin → Imágenes → "De la base").
 *
 * <p>Lo que se protege acá es el criterio, no el SQL: qué se ofrece, en qué orden y qué se
 * descarta. Ofrecer una foto ajena no es un error inocuo — la elección queda recordada por
 * marca+modelo y se reusa en cada carga futura.
 */
class ImagenManualSimilaresTest {

    /** Una fila de `productos` tal como la lee el servicio. */
    private record Fila(long id, String marca, String modelo, String url, boolean activo) {}

    private static ImagenManualService servicioCon(Fila... filas) {
        var jdbc = org.mockito.Mockito.mock(JdbcTemplate.class,
                withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(inv -> {
                    RowMapper<?> mapper = inv.getArgument(1);
                    var salida = new ArrayList<>();
                    for (Fila f : filas) salida.add(mapper.mapRow(resultSet(f), 0));
                    return salida;
                });
        return new ImagenManualService(jdbc);
    }

    private static ResultSet resultSet(Fila f) throws SQLException {
        var rs = org.mockito.Mockito.mock(ResultSet.class,
                withSettings().mockMaker(org.mockito.MockMakers.SUBCLASS));
        when(rs.getLong("id")).thenReturn(f.id());
        when(rs.getString("marca")).thenReturn(f.marca());
        when(rs.getString("modelo")).thenReturn(f.modelo());
        when(rs.getString("imagen_url")).thenReturn(f.url());
        when(rs.getBoolean("activo")).thenReturn(f.activo());
        when(rs.getString("clave_suelta")).thenReturn(ImagenManualService.clave(f.marca(), f.modelo()));
        return rs;
    }

    @Test void ponePrimeroElModeloMasParecido() {
        var r = servicioCon(
                new Fila(1, "Dell", "Inspiron 15 3520 i5 8GB", "https://img/lejos.jpg", true),
                new Fila(2, "Dell", "Latitude 5450 Core Ultra 5 16GB", "https://img/cerca.jpg", true))
                .similares("Dell", "Latitude 5450 Core Ultra 7 32GB", 8);

        assertEquals("https://img/cerca.jpg", r.get(0).url(), "gana el que comparte más modelo");
    }

    /**
     * El caso que motivó el piso: medido el 2026-09-18, "Dell LDC16255" traía "Dell LDC15255-A117"
     * —otra notebook— porque compartían marca y cuatro caracteres. Compartir la marca no alcanza.
     */
    @Test void descartaAlQueSoloComparteLaMarca() {
        var r = servicioCon(new Fila(1, "Dell", "LDC15255-A117 Ryzen 7", "https://img/otra.jpg", true))
                .similares("Dell", "LDC16255 Ryzen 7 250", 8);

        assertTrue(r.isEmpty(), "cuatro caracteres en común no son una candidata");
    }

    @Test void marcaElMismoModeloRedactadoDistintoComoExacto() {
        var r = servicioCon(new Fila(1, "Apple", "MacBook Air M5 13\" 16GB 512GB", "https://img/air.jpg", true))
                .similares("Apple", "MacBook Air M5 13 16GB 512GB", 8);

        assertEquals(1, r.size());
        assertTrue(r.get(0).exacto(), "misma clave suelta: es el mismo artículo escrito distinto");
    }

    /** Una familia entera suele compartir un único render: mostrarlo ocho veces no ayuda a elegir. */
    @Test void deduplicaPorUrlYPrefiereElPublicado() {
        var r = servicioCon(
                new Fila(1, "Lenovo", "Legion 5 Pro 16 RTX 4060", "https://img/legion.jpg", false),
                new Fila(2, "Lenovo", "Legion 5 Pro 16 RTX 4070", "https://img/legion.jpg", true))
                .similares("Lenovo", "Legion 5 Pro 16 RTX 4050", 8);

        assertEquals(1, r.size(), "la misma URL se ofrece una sola vez");
        assertTrue(r.get(0).activo(), "entre iguales gana el publicado, que es el que el cliente ve");
    }

    @Test void sinMarcaOSinModeloNoAdivina() {
        assertTrue(servicioCon().similares("", "Legion 5", 8).isEmpty());
        assertTrue(servicioCon().similares("Lenovo", "", 8).isEmpty());
    }
}
