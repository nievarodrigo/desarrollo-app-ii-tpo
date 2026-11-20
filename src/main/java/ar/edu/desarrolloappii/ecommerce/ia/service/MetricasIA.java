package ar.edu.desarrolloappii.ecommerce.ia.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Monitoreo en uso de la feature de IA (Clase 14, métricas de operación): cantidad de consultas,
 * cómo se resolvieron, tasa de fallback, latencia p95, tokens y costo acumulado.
 */
public class MetricasIA {
    private final AtomicLong consultas = new AtomicLong();
    private final AtomicLong tokensEntrada = new AtomicLong();
    private final AtomicLong tokensSalida = new AtomicLong();
    private final DoubleAdder costoUsd = new DoubleAdder();
    private final Map<String, Long> porOrigen = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, Long> porMotivo = Collections.synchronizedMap(new TreeMap<>());
    private final List<Long> latencias = Collections.synchronizedList(new ArrayList<>());

    public void registrar(String origen, String motivo, long ms, long entrada, long salida, double costo) {
        consultas.incrementAndGet();
        tokensEntrada.addAndGet(entrada);
        tokensSalida.addAndGet(salida);
        costoUsd.add(costo);
        porOrigen.merge(origen, 1L, Long::sum);
        if (motivo != null) porMotivo.merge(motivo, 1L, Long::sum);
        synchronized (latencias) {
            latencias.add(ms);
            if (latencias.size() > 1000) latencias.remove(0); // ventana de las últimas 1000
        }
    }

    public Map<String, Object> resumen() {
        long total = consultas.get();
        long fallback = porOrigen.getOrDefault("reglas", 0L);
        Map<String, Object> r = new TreeMap<>();
        r.put("consultas", total);
        r.put("porOrigen", Map.copyOf(porOrigen));
        r.put("porMotivoDeFallback", Map.copyOf(porMotivo));
        r.put("tasaFallback", total == 0 ? 0.0 : (double) fallback / total);
        r.put("latenciaP95ms", p95());
        r.put("tokensEntrada", tokensEntrada.get());
        r.put("tokensSalida", tokensSalida.get());
        r.put("costoEstimadoUsd", Math.round(costoUsd.sum() * 1_000_000) / 1_000_000.0);
        return r;
    }

    long p95() {
        List<Long> copia;
        synchronized (latencias) { copia = new ArrayList<>(latencias); }
        if (copia.isEmpty()) return 0;
        Collections.sort(copia);
        return copia.get((int) Math.ceil(copia.size() * 0.95) - 1);
    }
}
