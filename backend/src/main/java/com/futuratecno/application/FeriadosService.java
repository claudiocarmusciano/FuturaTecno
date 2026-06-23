package com.futuratecno.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provee el conjunto de feriados de Argentina por año, desde la API pública nager.date.
 * Cachea por año y, si la API falla, devuelve un conjunto vacío (solo se saltean fines de semana).
 */
@Service
public class FeriadosService {
    private static final Logger logger = LoggerFactory.getLogger(FeriadosService.class);

    private final RestTemplate restTemplate;
    private final Map<Integer, Set<LocalDate>> cache = new ConcurrentHashMap<>();

    @Value("${eta.feriados-api:https://date.nager.at/api/v3/PublicHolidays}")
    private String apiUrl;

    public FeriadosService(@Qualifier("imageRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Set<LocalDate> feriadosDe(int anio) {
        return cache.computeIfAbsent(anio, this::cargarDesdeApi);
    }

    private Set<LocalDate> cargarDesdeApi(int anio) {
        Set<LocalDate> feriados = new HashSet<>();
        try {
            JsonNode resp = restTemplate.getForObject(apiUrl + "/" + anio + "/AR", JsonNode.class);
            if (resp != null && resp.isArray()) {
                for (JsonNode f : resp) {
                    String fecha = f.path("date").asText(null);
                    if (fecha != null) feriados.add(LocalDate.parse(fecha));
                }
                logger.info("Feriados {} cargados: {}", anio, feriados.size());
            }
        } catch (Exception e) {
            logger.warn("No se pudieron cargar feriados de {} ({}). Solo se saltean fines de semana.", anio, e.getMessage());
        }
        return feriados;
    }
}
