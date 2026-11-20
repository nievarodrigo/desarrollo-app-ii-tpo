package ar.edu.desarrolloappii.ecommerce.ia.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Circuit breaker del proveedor de IA (Clase 12 y 14: "tras muchas fallas deja de llamar un rato").
 *
 * Mismos parámetros que el ejemplo de la cátedra con MicroProfile Fault Tolerance:
 * {@code requestVolumeThreshold = 10}, {@code failureRatio = 0.5}, {@code delay = 30 s}.
 * <ul>
 *   <li><b>CERRADO</b>: se llama al proveedor; se recuerdan los últimos 10 resultados.</li>
 *   <li><b>ABIERTO</b>: si de esos 10, la mitad o más fallaron (timeout, error, salida inválida),
 *       no se llama durante 30 s: el gateway responde el fallback al instante.</li>
 *   <li><b>SEMIABIERTO</b>: pasados los 30 s se deja pasar UNA llamada de prueba; si sale bien
 *       se cierra, si falla se vuelve a abrir.</li>
 * </ul>
 * WildFly trae MicroProfile Fault Tolerance sólo en su perfil microprofile; nosotros desplegamos
 * en standalone-full (por JMS), así que el patrón se implementa a mano, en 60 líneas testeables.
 */
public class CircuitoIA {
    public enum Estado { CERRADO, ABIERTO, SEMIABIERTO }

    static final int VENTANA = 10;
    static final double PROPORCION_FALLAS = 0.5;
    static final long ESPERA_MS = 30_000;

    private final Clock reloj;
    private final Deque<Boolean> resultados = new ArrayDeque<>();
    private Estado estado = Estado.CERRADO;
    private long abiertoDesde;
    private boolean pruebaEnCurso;

    public CircuitoIA() { this(Clock.systemUTC()); }
    public CircuitoIA(Clock reloj) { this.reloj = reloj; }

    /** ¿Se puede llamar al proveedor ahora? En SEMIABIERTO sólo autoriza una llamada de prueba. */
    public synchronized boolean permiteLlamar() {
        if (estado == Estado.ABIERTO && reloj.millis() - abiertoDesde >= ESPERA_MS) {
            estado = Estado.SEMIABIERTO;
            pruebaEnCurso = false;
        }
        if (estado == Estado.CERRADO) return true;
        if (estado == Estado.SEMIABIERTO && !pruebaEnCurso) { pruebaEnCurso = true; return true; }
        return false;
    }

    public synchronized void registrarExito() {
        if (estado == Estado.SEMIABIERTO) { estado = Estado.CERRADO; resultados.clear(); }
        agregar(true);
    }

    public synchronized void registrarFalla() {
        if (estado == Estado.SEMIABIERTO) { abrir(); return; }
        agregar(false);
        long fallas = resultados.stream().filter(ok -> !ok).count();
        if (resultados.size() >= VENTANA && fallas >= VENTANA * PROPORCION_FALLAS) abrir();
    }

    public synchronized Estado estado() {
        if (estado == Estado.ABIERTO && reloj.millis() - abiertoDesde >= ESPERA_MS) return Estado.SEMIABIERTO;
        return estado;
    }

    private void agregar(boolean ok) {
        resultados.addLast(ok);
        while (resultados.size() > VENTANA) resultados.removeFirst();
    }

    private void abrir() {
        estado = Estado.ABIERTO;
        abiertoDesde = reloj.millis();
        pruebaEnCurso = false;
        resultados.clear();
    }
}
