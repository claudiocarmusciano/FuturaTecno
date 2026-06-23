package com.futuratecno.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Calcula la fecha estimada de entrega: N días hábiles después de la compra,
 * salteando fines de semana y feriados de Argentina, según la hora de corte.
 */
@Service
public class EtaService {

    private final FeriadosService feriadosService;

    @Value("${app.cutoff-hour:14}")
    private int horaCorte;

    @Value("${eta.dias-habiles:3}")
    private int diasHabiles;

    public EtaService(FeriadosService feriadosService) {
        this.feriadosService = feriadosService;
    }

    public int getHoraCorte() { return horaCorte; }
    public int getDiasHabiles() { return diasHabiles; }

    public boolean esAntesDeCorte() {
        return LocalDateTime.now().getHour() < horaCorte;
    }

    /** Fecha estimada de entrega contando {diasHabiles} días hábiles desde la compra. */
    public LocalDate calcularFechaEntrega() {
        LocalDateTime ahora = LocalDateTime.now();
        // Si ya pasó la hora de corte, el pedido "entra" al día siguiente.
        LocalDate base = esAntesDeCorte() ? ahora.toLocalDate() : ahora.toLocalDate().plusDays(1);

        LocalDate fecha = base;
        int habilesContados = 0;
        while (habilesContados < diasHabiles) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha)) {
                habilesContados++;
            }
        }
        return fecha;
    }

    private boolean esHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) return false;
        return !feriadosService.feriadosDe(fecha.getYear()).contains(fecha);
    }
}
