package com.futuratecno.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Freno de intentos por clave ("login-email:ana@…", "registro-ip:1.2.3.4") con ventana deslizante
 * en memoria. Alcanza sin base ni Redis porque la tienda corre en una sola instancia de Railway;
 * un reinicio lo pone en cero, que para esto es aceptable.
 *
 * <p>Ojo con la IP: con {@code forward-headers-strategy=framework} sale de X-Forwarded-For, que un
 * atacante puede falsear para rotar "IPs". Por eso lo que protege una cuenta es el límite por
 * EMAIL; el de IP solo corta el volumen bruto.
 */
@Component
public class LimitadorIntentos {

    private final Clock clock;
    private final Map<String, Deque<Instant>> marcas = new ConcurrentHashMap<>();
    private final AtomicInteger llamadas = new AtomicInteger();

    @Autowired
    public LimitadorIntentos() {
        this(Clock.systemUTC());
    }

    LimitadorIntentos(Clock clock) {
        this.clock = clock;
    }

    /** Anota un intento y dice si entra en el cupo ({@code maximo} por {@code ventana}). */
    public boolean permitir(String clave, int maximo, Duration ventana) {
        Instant ahora = clock.instant();
        Deque<Instant> d = marcas.computeIfAbsent(clave, k -> new ArrayDeque<>());
        boolean ok;
        synchronized (d) {
            podar(d, ahora, ventana);
            ok = d.size() < maximo;
            if (ok) d.addLast(ahora);
        }
        limpiarDeVezEnCuando(ventana);
        return ok;
    }

    /** ¿Ya se llegó al tope? No anota nada: para chequear antes de intentar. */
    public boolean agotado(String clave, int maximo, Duration ventana) {
        Deque<Instant> d = marcas.get(clave);
        if (d == null) return false;
        synchronized (d) {
            podar(d, clock.instant(), ventana);
            return d.size() >= maximo;
        }
    }

    /** Anota un intento fallido (sin tope): se usa junto con {@link #agotado}. */
    public void anotar(String clave) {
        Deque<Instant> d = marcas.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) { d.addLast(clock.instant()); }
    }

    /** Borra el historial de la clave (por ejemplo, después de un login correcto). */
    public void reiniciar(String clave) {
        marcas.remove(clave);
    }

    private static void podar(Deque<Instant> d, Instant ahora, Duration ventana) {
        Instant limite = ahora.minus(ventana);
        while (!d.isEmpty() && d.peekFirst().isBefore(limite)) d.pollFirst();
    }

    /** Cada tanto se tiran las claves viejas, así el mapa no crece para siempre. */
    private void limpiarDeVezEnCuando(Duration ventana) {
        if (llamadas.incrementAndGet() % 500 != 0) return;
        Instant limite = clock.instant().minus(ventana.compareTo(Duration.ofHours(1)) > 0 ? ventana : Duration.ofHours(1));
        marcas.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(limite);
            }
        });
    }
}
