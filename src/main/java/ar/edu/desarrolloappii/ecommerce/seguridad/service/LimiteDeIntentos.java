package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** SEC-03: cinco ingresos fallidos en 15 minutos bloquean la cuenta, aun con la clave correcta. */
public class LimiteDeIntentos {
    static final int MAXIMO = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);

    private final Clock reloj;
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public LimiteDeIntentos(Clock reloj) { this.reloj = reloj; }

    /** Llamar ANTES de verificar la clave. */
    public void verificarDisponible(String clave) {
        Deque<Instant> lista = fallos.get(clave);
        if (lista == null) return;
        synchronized (lista) {
            purgar(lista);
            if (lista.size() >= MAXIMO) {
                long espera = Duration.between(reloj.instant(), lista.peekFirst().plus(VENTANA)).toSeconds() + 1;
                throw new DemasiadosIntentosException(Math.max(espera, 1));
            }
        }
    }

    public void registrarFallo(String clave) {
        Deque<Instant> lista = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (lista) {
            purgar(lista);
            lista.addLast(reloj.instant());
        }
    }

    public void registrarExito(String clave) { fallos.remove(clave); }

    private void purgar(Deque<Instant> lista) {
        Instant limite = reloj.instant().minus(VENTANA);
        while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) lista.removeFirst();
    }
}
