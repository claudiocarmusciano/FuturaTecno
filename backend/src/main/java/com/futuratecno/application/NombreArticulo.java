package com.futuratecno.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Un nombre (marca + modelo) con el que se conoce un artículo. Las memorias manuales (imagen,
 * descripción, atributos, márgenes) están guardadas por marca+modelo; cuando la identidad (V42)
 * dice que dos redacciones son el mismo artículo, se consultan todas sus redacciones en orden —
 * así una carga redactada distinto no pierde lo que el admin cargó a mano.
 */
public record NombreArticulo(String marca, String modelo) {

    /** Sin repetidos (a igual marca+modelo normalizados), conservando el orden de prioridad. */
    public static List<NombreArticulo> distintos(List<NombreArticulo> nombres) {
        List<NombreArticulo> out = new ArrayList<>();
        List<String> vistos = new ArrayList<>();
        for (NombreArticulo n : nombres) {
            if (n == null || n.marca() == null || n.modelo() == null) continue;
            String k = n.marca().strip().toLowerCase(Locale.ROOT) + "|" + n.modelo().strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (!vistos.contains(k)) { vistos.add(k); out.add(n); }
        }
        return out;
    }
}
