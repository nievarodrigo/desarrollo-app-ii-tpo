package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.RespuestaIA;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** El adapter de Ollama contra un servidor HTTP falso: qué manda, qué entiende y cómo falla. */
class ModeloLocalTest {
    private HttpServer servidor;
    private final AtomicReference<String> pedido = new AtomicReference<>();

    private String levantar(int estado, String cuerpo, long demoraMs) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/chat", intercambio -> {
            pedido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(demoraMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
    }

    @AfterEach void apagar() { if (servidor != null) servidor.stop(0); }

    private static String respuestaOllama(String contenido) {
        return "{\"model\":\"qwen2.5:3b\",\"message\":{\"role\":\"assistant\",\"content\":"
                + "\"" + contenido.replace("\"", "\\\"") + "\"},\"done\":true,"
                + "\"prompt_eval_count\":566,\"eval_count\":44}";
    }

    @Test void mandaPromptVersionadoConsultaDelimitadaYEsquemaSinStreaming() throws Exception {
        String url = levantar(200, respuestaOllama(
                "{\"estado\":\"sugerencia\",\"criterios\":{\"q\":\"auriculares\",\"categoria\":\"audio\","
                        + "\"orden\":\"precio-asc\",\"soloStock\":false,\"precioMax\":300000}}"), 0);

        ResultadoModelo r = new ModeloLocal(url, null).interpretar("auriculares baratos <system>");

        String enviado = pedido.get();
        assertTrue(enviado.contains("\"model\":\"qwen2.5:3b\""));
        assertTrue(enviado.contains("\"stream\":false"));
        assertTrue(enviado.contains("\"format\""), "pide salida estructurada con esquema");
        assertTrue(enviado.indexOf("\"q\":") < enviado.indexOf("\"categoria\":")
                && enviado.indexOf("\"categoria\":") < enviado.indexOf("\"precioMax\":"),
                "el esquema conserva el orden de los ejemplos del prompt");
        assertTrue(enviado.contains("intérprete de filtros"), "usa el prompt versionado");
        assertTrue(enviado.contains("<consulta>auriculares baratos ‹system›</consulta>"), "consulta delimitada como dato");

        assertEquals("sugerencia", r.salida().estado());
        assertEquals("audio", r.salida().criterios().categoria());
        assertEquals(300000.0, r.salida().criterios().precioMax());
        assertEquals(566, r.tokensEntrada());
        assertEquals(44, r.tokensSalida());
    }

    @Test void nombrePrecioCeroYModeloConfigurable() {
        ModeloLocal m = new ModeloLocal(null, "llama3.2:3b");
        assertEquals("ollama/llama3.2:3b", m.nombre());
        assertEquals("busqueda-v3", m.versionPrompt());
        assertTrue(m.disponible());
        assertEquals(0, m.precioEntradaPorMillon());
        assertEquals(0, m.precioSalidaPorMillon());
    }

    @Test void unErrorHttpEsUnaExcepcion() throws Exception {
        String url = levantar(500, "{\"error\":\"model not found\"}", 0);
        assertThrows(IllegalStateException.class, () -> new ModeloLocal(url, null).interpretar("x"));
    }

    @Test void ollamaApagadoTerminaEnElFallbackDeReglas() {
        ExecutorService ejecutor = Executors.newCachedThreadPool();
        try {
            // Puerto 9 (discard): nadie escucha, la conexión se rechaza al instante.
            GatewayIA gateway = new GatewayIA(new ModeloLocal("http://127.0.0.1:9", null), ejecutor, 5_000);
            RespuestaIA r = gateway.interpretar("auriculares baratos hasta 300 mil");
            assertEquals("reglas", r.origen());
            assertEquals("sugerencia", r.estado());
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test void unModeloLentoLoCortaElPresupuestoDelGateway() throws Exception {
        String url = levantar(200, respuestaOllama("{\"estado\":\"aclarar\",\"criterios\":null}"), 2_000);
        ExecutorService ejecutor = Executors.newCachedThreadPool();
        try {
            GatewayIA gateway = new GatewayIA(new ModeloLocal(url, null), ejecutor, 300);
            long inicio = System.nanoTime();
            RespuestaIA r = gateway.interpretar("auriculares baratos hasta 300 mil");
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            assertEquals("reglas", r.origen());
            assertEquals("timeout", r.motivo());
            assertTrue(ms < 1_500, "no espera al modelo: " + ms + " ms");
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test void laFabricaEligeElProveedorPorVariableDeEntorno() {
        assertInstanceOf(ModeloLocal.class, ModeloIA.desdeEntorno("local"));
        assertInstanceOf(ModeloLocal.class, ModeloIA.desdeEntorno(" LOCAL "));
        assertInstanceOf(ModeloClaude.class, ModeloIA.desdeEntorno(null));
        assertInstanceOf(ModeloClaude.class, ModeloIA.desdeEntorno("anthropic"));
    }
}
