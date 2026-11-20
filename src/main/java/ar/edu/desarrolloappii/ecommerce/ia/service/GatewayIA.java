package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.CriteriosSugeridos;
import ar.edu.desarrolloappii.ecommerce.ia.domain.RespuestaIA;
import ar.edu.desarrolloappii.ecommerce.ia.domain.SalidaModelo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Gateway de IA (patrón Facade): la ÚNICA puerta entre GOMARKET y un modelo de lenguaje.
 *
 * Encapsula, en este orden:
 * <ol>
 *   <li><b>Interruptor</b> (plan de apagado): con la IA deshabilitada responde el fallback.</li>
 *   <li><b>Política de entrada</b>: tamaño, datos personales, intentos de inyección.</li>
 *   <li><b>Circuit breaker</b>: si el proveedor viene fallando, ni lo intenta durante 30 s.</li>
 *   <li><b>Bulkhead</b>: como máximo 4 llamadas simultáneas al proveedor; si está saturado,
 *       fallback inmediato en vez de encolar usuarios.</li>
 *   <li><b>Timeout de 5 s</b>: si el modelo no responde a tiempo, se corta la espera.</li>
 *   <li><b>Validación de salida</b>: aunque el modelo use structured outputs, se revalida todo.</li>
 *   <li><b>Fallback</b>: ante timeout, error, salida inválida o falta de proveedor, responde
 *       {@link InterpreteDeReglas}. El usuario nunca ve un error por culpa de la IA.</li>
 * </ol>
 * Cada consulta deja una traza en el log (versión del prompt, modelo, origen, motivo, latencia,
 * tokens y costo estimado) y suma a {@link MetricasIA}.
 */
@ApplicationScoped
public class GatewayIA {
    private static final Logger LOG = Logger.getLogger(GatewayIA.class.getName());
    public static final long PRESUPUESTO_MS = 5_000;
    static final int MAX_CONCURRENTES = 4;

    private static final Set<String> CATEGORIAS = Set.of("todas", "tecnologia", "audio", "hogar", "oficina", "deportes");
    private static final Set<String> ORDENES = Set.of("relevancia", "precio-asc", "precio-desc", "rating", "nombre");
    private static final Pattern DATOS_PERSONALES = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|\\b\\d{13,19}\\b");
    private static final Pattern INYECCION = Pattern.compile(
            "api[ _-]?key|ignora.*(reglas|instrucciones)|system prompt|emails? de (los )?clientes|pedidos de otros|<[^>]+>|cambia(le)? (el )?precio");
    private static final Pattern MONEDA_EXTRANJERA = Pattern.compile("\\bdolar(es)?\\b|\\busd\\b|€|\\beur(os)?\\b|u\\$s");

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    ManagedExecutorService ejecutorDelContenedor;

    private ModeloIA modelo;
    private ExecutorService ejecutor;
    private long presupuestoMs = PRESUPUESTO_MS;
    private final InterpreteDeReglas reglas = new InterpreteDeReglas();
    private CircuitoIA circuito = new CircuitoIA();
    private InterruptorIA interruptor = new InterruptorIA();
    private final MetricasIA metricas = new MetricasIA();
    private final Semaphore capacidad = new Semaphore(MAX_CONCURRENTES);

    public GatewayIA() { }

    /** Para tests: modelo falso, ejecutor propio y presupuesto configurable. */
    public GatewayIA(ModeloIA modelo, ExecutorService ejecutor, long presupuestoMs) {
        this(modelo, ejecutor, presupuestoMs, new CircuitoIA(), new InterruptorIA(true));
    }

    public GatewayIA(ModeloIA modelo, ExecutorService ejecutor, long presupuestoMs,
                     CircuitoIA circuito, InterruptorIA interruptor) {
        this.modelo = modelo;
        this.ejecutor = ejecutor;
        this.presupuestoMs = presupuestoMs;
        this.circuito = circuito;
        this.interruptor = interruptor;
    }

    public CircuitoIA circuito() { return circuito; }
    public InterruptorIA interruptor() { return interruptor; }
    public MetricasIA metricas() { return metricas; }
    public String nombreModelo() { return modelo == null ? "-" : modelo.nombre(); }
    public String versionPrompt() { return modelo == null ? "-" : modelo.versionPrompt(); }

    @PostConstruct
    void iniciar() {
        if (modelo == null) modelo = ModeloIA.desdeEntorno(System.getenv("GOMARKET_IA_PROVEEDOR"));
        if (ejecutor == null) ejecutor = ejecutorDelContenedor;
        if (modelo.disponible() && ejecutor != null) {
            ModeloIA aCalentar = modelo;
            ejecutor.submit(aCalentar::calentar); // no bloquea el arranque
        }
        LOG.info("Gateway de IA con " + modelo.nombre() + (modelo.disponible() ? "" : " (sin credenciales: sólo fallback)"));
    }

    public RespuestaIA interpretar(String consulta) {
        long[] tokens = new long[2];
        RespuestaIA respuesta = resolver(consulta, tokens);
        double costo = modelo == null ? 0 : (tokens[0] * modelo.precioEntradaPorMillon()
                + tokens[1] * modelo.precioSalidaPorMillon()) / 1_000_000;
        metricas.registrar(respuesta.origen(), respuesta.motivo(), respuesta.milisegundos(), tokens[0], tokens[1], costo);
        LOG.info(String.format(java.util.Locale.ROOT,
                "ia prompt=%s modelo=%s estado=%s origen=%s motivo=%s ms=%d tokensEntrada=%d tokensSalida=%d costoUsd=%.6f circuito=%s",
                versionPrompt(), nombreModelo(), respuesta.estado(), respuesta.origen(), respuesta.motivo(),
                respuesta.milisegundos(), tokens[0], tokens[1], costo, circuito.estado()));
        return respuesta;
    }

    private RespuestaIA resolver(String consulta, long[] tokens) {
        long inicio = System.nanoTime();
        if (consulta == null || consulta.isBlank() || consulta.length() > 500) {
            return new RespuestaIA("invalida", null, "politica", null, ms(inicio));
        }
        String normalizada = InterpreteDeReglas.normalizar(consulta);
        if (DATOS_PERSONALES.matcher(normalizada).find() || MONEDA_EXTRANJERA.matcher(normalizada).find()) {
            return new RespuestaIA("aclarar", null, "politica", null, ms(inicio));
        }
        if (INYECCION.matcher(normalizada).find()) {
            return new RespuestaIA("rechazada", null, "politica", null, ms(inicio));
        }
        if (!interruptor.habilitada()) return fallback(consulta, "deshabilitada", inicio);
        if (modelo == null || !modelo.disponible()) return fallback(consulta, "sin-proveedor", inicio);
        if (!capacidad.tryAcquire()) return fallback(consulta, "saturado", inicio);
        // El circuito se consulta con el cupo ya tomado: así la llamada de prueba del estado
        // SEMIABIERTO siempre llega al proveedor y el circuito no queda trabado.
        if (!circuito.permiteLlamar()) {
            capacidad.release();
            return fallback(consulta, "circuito-abierto", inicio);
        }

        // El cupo se libera UNA sola vez: desde la tarea cuando termina, o desde acá si la
        // tarea se cancela antes de arrancar (si no, quedaría tomado para siempre).
        AtomicBoolean liberado = new AtomicBoolean(false);
        AtomicBoolean arranco = new AtomicBoolean(false);
        Runnable liberar = () -> { if (liberado.compareAndSet(false, true)) capacidad.release(); };
        Future<ResultadoModelo> llamada;
        try {
            llamada = ejecutor.submit(() -> {
                arranco.set(true);
                try { return modelo.interpretar(consulta); }
                finally { liberar.run(); }
            });
        } catch (RuntimeException rechazo) {
            liberar.run();
            circuito.registrarFalla();
            return fallback(consulta, "error", inicio);
        }
        try {
            long restante = presupuestoMs - ms(inicio);
            ResultadoModelo resultado = llamada.get(Math.max(0, restante), TimeUnit.MILLISECONDS);
            if (resultado != null) { tokens[0] = resultado.tokensEntrada(); tokens[1] = resultado.tokensSalida(); }
            SalidaModelo salida = resultado == null ? null : resultado.salida();
            String problema = validar(salida);
            if (problema != null) {
                circuito.registrarFalla();
                LOG.warning("Salida del modelo descartada: " + problema);
                return fallback(consulta, "salida-invalida", inicio);
            }
            circuito.registrarExito();
            return new RespuestaIA(salida.estado(), salida.criterios(), "modelo", null, ms(inicio));
        } catch (TimeoutException timeout) {
            // Si la tarea seguía en la cola del ejecutor, cancel la descarta y su finally nunca corre.
            // Si arrancó, conserva el cupo hasta que el proveedor realmente termine (eso es el bulkhead).
            llamada.cancel(true);
            if (!arranco.get()) liberar.run();
            circuito.registrarFalla();
            LOG.warning("El modelo superó los " + presupuestoMs + " ms: fallback por reglas.");
            return fallback(consulta, "timeout", inicio);
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
            return fallback(consulta, "error", inicio);
        } catch (Exception error) {
            circuito.registrarFalla();
            LOG.warning("Falló el proveedor de IA: " + error.getMessage());
            return fallback(consulta, "error", inicio);
        }
    }

    private RespuestaIA fallback(String consulta, String motivo, long inicio) {
        return reglas.interpretar(consulta)
                .map(c -> new RespuestaIA("sugerencia", c, "reglas", motivo, ms(inicio)))
                .orElseGet(() -> new RespuestaIA("manual", null, "reglas", motivo, ms(inicio)));
    }

    /** @return null si la salida es válida; si no, el motivo. */
    static String validar(SalidaModelo salida) {
        if (salida == null || salida.estado() == null) return "salida vacía";
        switch (salida.estado()) {
            case "aclarar", "rechazada" -> { return salida.criterios() == null ? null : "criterios inesperados"; }
            case "sugerencia" -> { }
            default -> { return "estado desconocido: " + salida.estado(); }
        }
        CriteriosSugeridos c = salida.criterios();
        if (c == null) return "faltan criterios";
        if (c.q() == null || c.q().length() > 100 || c.q().matches("(?s).*[<>\\x00-\\x1f].*")) return "texto inválido";
        if (!CATEGORIAS.contains(c.categoria())) return "categoría inválida";
        if (!ORDENES.contains(c.orden())) return "orden inválido";
        if (c.precioMax() != null && (c.precioMax() <= 0 || c.precioMax() > 1_000_000_000)) return "precio inválido";
        return null;
    }

    private static long ms(long inicioNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
    }
}
