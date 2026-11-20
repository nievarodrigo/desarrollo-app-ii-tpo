package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.SalidaModelo;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter hacia un modelo de lenguaje LOCAL servido por Ollama ({@code POST /api/chat}).
 *
 * Es el mismo puerto {@link ModeloIA} que {@link ModeloClaude}: el gateway no se entera de
 * cuál está detrás. Lo usamos para desarrollo y para la demo (costo por consulta cero, sin
 * API key y sin internet); en producción la decisión sigue siendo Claude Haiku (ADR-012).
 *
 * Configuración por variables de entorno:
 * <ul>
 *   <li>{@code GOMARKET_IA_LOCAL_URL}: por defecto {@code http://127.0.0.1:11434}.</li>
 *   <li>{@code GOMARKET_IA_LOCAL_MODELO}: por defecto {@code qwen2.5:3b}.</li>
 * </ul>
 * Mismo prompt versionado, misma consulta delimitada como dato y salida estructurada con
 * el esquema JSON de {@link SalidaModelo} (el gateway la vuelve a validar igual).
 * Timeout HTTP de 4,5 s y sin reintentos, para quedar dentro del presupuesto de 5 s.
 */
public class ModeloLocal implements ModeloIA {
    static final String URL_POR_DEFECTO = "http://127.0.0.1:11434";
    static final String MODELO_POR_DEFECTO = "qwen2.5:3b";
    static final Duration TIMEOUT_HTTP = Duration.ofMillis(4500);
    /** Mantiene el modelo cargado en la GPU entre consultas (si no, la primera de cada rato tarda segundos). */
    static final String MANTENER_CARGADO = "30m";

    /**
     * Esquema de {@link SalidaModelo} para el modo de salida estructurada de Ollama. Va ORDENADO
     * (como en los ejemplos del prompt): Ollama genera los campos en el orden del esquema y, con
     * otro orden, un modelo chico tiende a dejar {@code q} vacío.
     */
    static final Map<String, Object> ESQUEMA = ordenado(
            "type", "object",
            "required", List.of("estado", "criterios"),
            "properties", ordenado(
                    "estado", ordenado("type", "string", "enum", List.of("sugerencia", "aclarar", "rechazada")),
                    "criterios", ordenado(
                            "type", List.of("object", "null"),
                            "required", List.of("q", "categoria", "orden", "soloStock", "precioMax"),
                            "properties", ordenado(
                                    "q", ordenado("type", "string"),
                                    "categoria", ordenado("type", "string", "enum",
                                            List.of("todas", "tecnologia", "audio", "hogar", "oficina", "deportes")),
                                    "orden", ordenado("type", "string", "enum",
                                            List.of("relevancia", "precio-asc", "precio-desc", "rating", "nombre")),
                                    "soloStock", ordenado("type", "boolean"),
                                    "precioMax", ordenado("type", List.of("number", "null"))))));

    private static Map<String, Object> ordenado(Object... claveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) mapa.put((String) claveValor[i], claveValor[i + 1]);
        return mapa;
    }

    private final URI url;
    private final String modelo;
    private final String prompt;
    private final HttpClient http;

    public ModeloLocal() {
        this(System.getenv("GOMARKET_IA_LOCAL_URL"), System.getenv("GOMARKET_IA_LOCAL_MODELO"));
    }

    ModeloLocal(String url, String modelo) {
        String base = url == null || url.isBlank() ? URL_POR_DEFECTO : url.trim().replaceAll("/+$", "");
        this.url = URI.create(base + "/api/chat");
        this.modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim();
        this.prompt = ModeloClaude.cargarPrompt();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Override public String nombre() { return "ollama/" + modelo; }
    @Override public String versionPrompt() { return ModeloClaude.VERSION_PROMPT; }
    /** Siempre "disponible": si Ollama está apagado, la llamada falla y el gateway degrada a reglas. */
    @Override public boolean disponible() { return true; }
    // Precio por token: 0 (corre en nuestra máquina). El costo es el hardware, no la consulta.

    @Override
    public ResultadoModelo interpretar(String consulta) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", modelo);
        cuerpo.put("stream", false);
        cuerpo.put("keep_alive", MANTENER_CARGADO);
        cuerpo.put("format", ESQUEMA);
        cuerpo.put("options", Map.of("temperature", 0));
        cuerpo.put("messages", List.of(
                Map.of("role", "system", "content", prompt),
                Map.of("role", "user", "content", ModeloClaude.delimitar(consulta))));

        try (Jsonb jsonb = JsonbBuilder.create()) {
            HttpResponse<String> respuesta = http.send(HttpRequest.newBuilder(url)
                            .timeout(TIMEOUT_HTTP)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonb.toJson(cuerpo)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new IllegalStateException("Ollama respondió " + respuesta.statusCode());
            }
            Map<?, ?> datos = jsonb.fromJson(respuesta.body(), Map.class);
            Object mensaje = datos.get("message");
            Object contenido = mensaje instanceof Map<?, ?> m ? m.get("content") : null;
            if (!(contenido instanceof String texto) || texto.isBlank()) {
                throw new IllegalStateException("El modelo local no devolvió texto.");
            }
            SalidaModelo salida = jsonb.fromJson(texto, SalidaModelo.class);
            return new ResultadoModelo(salida, entero(datos.get("prompt_eval_count")), entero(datos.get("eval_count")));
        }
    }

    /** Carga el modelo en memoria sin esperar respuesta útil: el gateway lo llama en segundo plano al arrancar. */
    @Override
    public void calentar() {
        try {
            interpretar("auriculares");
        } catch (Exception ignorada) {
            // Ollama apagado o lento: no pasa nada, la primera consulta real irá al fallback si hace falta.
        }
    }

    private static long entero(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0;
    }
}
