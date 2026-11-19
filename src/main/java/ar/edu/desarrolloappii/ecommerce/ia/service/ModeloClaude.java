package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.SalidaModelo;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredOutputConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Adapter hacia la API de Claude con el SDK oficial de Anthropic.
 *
 * Configuración por variables de entorno (nada de claves en el código):
 * <ul>
 *   <li>{@code ANTHROPIC_API_KEY}: si falta, el adapter queda "no disponible" y el gateway usa el fallback.</li>
 *   <li>{@code GOMARKET_IA_MODELO}: por defecto {@code claude-haiku-5-5}, el más barato (ver ADR de costos).</li>
 * </ul>
 * El cliente HTTP tiene timeout de 4,5 s y SIN reintentos: los reintentos del SDK harían
 * pasar la llamada por encima del presupuesto de 5 s del gateway.
 */
public class ModeloClaude implements ModeloIA {
    static final String MODELO_POR_DEFECTO = "claude-haiku-5-5";
    static final Duration TIMEOUT_HTTP = Duration.ofMillis(4500);

    private final String modelo;
    private final String prompt;
    private final AnthropicClient cliente;

    public ModeloClaude() {
        this(System.getenv("ANTHROPIC_API_KEY"), System.getenv("GOMARKET_IA_MODELO"));
    }

    ModeloClaude(String apiKey, String modelo) {
        this.modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim();
        this.prompt = cargarPrompt();
        this.cliente = apiKey == null || apiKey.isBlank() ? null
                : AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(TIMEOUT_HTTP).maxRetries(0).build();
    }

    @Override public String nombre() { return "anthropic/" + modelo; }

    @Override public boolean disponible() { return cliente != null; }

    @Override
    public SalidaModelo interpretar(String consulta) {
        MessageCreateParams base = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(1024L)
                .system(prompt)
                .addUserMessage(consulta)
                .build();
        StructuredMessage<SalidaModelo> respuesta = cliente.messages().create(
                base.toBuilder().outputConfig(StructuredOutputConfig.<SalidaModelo>builder()
                        .format(SalidaModelo.class)
                        .effort(OutputConfig.Effort.LOW) // tarea corta de clasificación: menos tokens y menos latencia
                        .build()).build());
        if (respuesta.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            return new SalidaModelo("rechazada", null);
        }
        return respuesta.content().stream()
                .flatMap(bloque -> bloque.text().stream())
                .map(texto -> texto.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El modelo no devolvió texto."));
    }

    private static String cargarPrompt() {
        try (InputStream in = ModeloClaude.class.getResourceAsStream("/ia/prompt-busqueda-v1.txt")) {
            if (in == null) throw new IllegalStateException("Falta el prompt /ia/prompt-busqueda-v1.txt");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer el prompt del gateway.", error);
        }
    }
}
