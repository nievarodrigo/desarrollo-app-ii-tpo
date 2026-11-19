package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.shared.exception.DemasiadasSolicitudesException;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de uso del gateway de IA: como máximo {@value #POR_MINUTO} consultas por minuto por cliente (IP).
 *
 * El bulkhead limita la CONCURRENCIA; esto limita el COSTO: sin él, cualquiera podría
 * gastar tokens pagos en un loop contra un endpoint público. Ventana fija de 60 s.
 */
@ApplicationScoped
public class LimiteDeConsultas {
    static final int POR_MINUTO = 30;
    private static final long VENTANA_MS = 60_000;

    private record Ventana(long inicio, int usadas) { }

    private final Map<String, Ventana> porCliente = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimiteDeConsultas() { this(Clock.systemUTC()); }
    public LimiteDeConsultas(Clock reloj) { this.reloj = reloj; }

    public void registrar(String cliente) {
        long ahora = reloj.millis();
        if (porCliente.size() > 10_000) porCliente.entrySet().removeIf(e -> ahora - e.getValue().inicio() >= VENTANA_MS);
        Ventana v = porCliente.merge(cliente == null ? "?" : cliente, new Ventana(ahora, 1),
                (vieja, nueva) -> ahora - vieja.inicio() >= VENTANA_MS ? nueva : new Ventana(vieja.inicio(), vieja.usadas() + 1));
        if (v.usadas() > POR_MINUTO) {
            long espera = Math.max(1, (v.inicio() + VENTANA_MS - ahora + 999) / 1000);
            throw new DemasiadasSolicitudesException(
                    "Hiciste muchas búsquedas con IA seguidas. Probá de nuevo en " + espera + " segundos.", espera);
        }
    }
}
