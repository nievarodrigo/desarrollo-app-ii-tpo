package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.CriteriosSugeridos;
import ar.edu.desarrolloappii.ecommerce.ia.domain.RespuestaIA;
import ar.edu.desarrolloappii.ecommerce.ia.domain.SalidaModelo;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Eval set de la feature de IA (Clase 14: "si la IA no tiene tests, no está terminada").
 *
 * Corre en cada build del CI sobre la capa DETERMINISTA del gateway (política + fallback por
 * reglas), que es lo que responde sin API key, y exige los umbrales del dataset: si no se
 * cumplen, el build falla y no se mergea. Deja el detalle en target/eval-ia.md.
 *
 * Con {@code -Dia.eval.real=true} evalúa un MODELO real con el mismo dataset y los mismos umbrales
 * (no corre en el CI por defecto): Claude con ANTHROPIC_API_KEY (cuesta centavos) o, con
 * GOMARKET_IA_PROVEEDOR=local, el modelo local de Ollama (gratis).
 */
class EvalSetIATest {

    record Resultado(String id, boolean adversarial, boolean correcto, boolean formatoValido,
                     String obtenido, String origen, long ms) { }

    @Test void elGatewayCumpleLosUmbralesDelEvalSet() throws Exception {
        JsonObject dataset = cargar();
        boolean local = "local".equalsIgnoreCase(String.valueOf(System.getenv("GOMARKET_IA_PROVEEDOR")).trim());
        boolean real = Boolean.getBoolean("ia.eval.real") && (local || System.getenv("ANTHROPIC_API_KEY") != null);
        ExecutorService ejecutor = Executors.newFixedThreadPool(4);
        try {
            ModeloIA modelo = real ? ModeloIA.desdeEntorno(System.getenv("GOMARKET_IA_PROVEEDOR")) : new SinProveedor();
            if (real) modelo.calentar(); // la primera llamada carga el modelo: que no cuente como timeout
            GatewayIA gateway = new GatewayIA(modelo, ejecutor, GatewayIA.PRESUPUESTO_MS);
            List<Resultado> resultados = new ArrayList<>();
            for (JsonValue v : dataset.getJsonArray("casos")) {
                JsonObject caso = v.asJsonObject();
                RespuestaIA r = gateway.interpretar(caso.getString("consulta"));
                resultados.add(new Resultado(caso.getString("id"), caso.getBoolean("adversarial", false),
                        correcto(caso.getJsonObject("esperado"), r), formatoValido(r),
                        r.estado() + (r.criterios() == null ? "" : " " + r.criterios()), r.origen(), r.milisegundos()));
            }
            JsonObject umbrales = dataset.getJsonObject("umbrales");
            double exactitud = proporcion(resultados, Resultado::correcto, false);
            double adversariales = proporcion(resultados, Resultado::correcto, true);
            double formato = (double) resultados.stream().filter(Resultado::formatoValido).count() / resultados.size();
            escribirReporte(dataset.getString("version"), real ? modelo.nombre() : "política + reglas (sin proveedor)",
                    resultados, exactitud, adversariales, formato, umbrales);

            assertTrue(exactitud >= umbrales.getJsonNumber("exactitud").doubleValue(), "exactitud " + exactitud);
            assertTrue(adversariales >= umbrales.getJsonNumber("adversarialesResistidos").doubleValue(),
                    "adversariales resistidos " + adversariales);
            assertTrue(formato >= umbrales.getJsonNumber("formatoValido").doubleValue(), "formato válido " + formato);
        } finally {
            ejecutor.shutdownNow();
        }
    }

    /** Sin proveedor: el gateway resuelve con la política y el intérprete de reglas. */
    static class SinProveedor implements ModeloIA {
        @Override public String nombre() { return "sin-proveedor"; }
        @Override public boolean disponible() { return false; }
        @Override public ResultadoModelo interpretar(String consulta) { throw new IllegalStateException(); }
    }

    static boolean correcto(JsonObject esperado, RespuestaIA r) {
        List<String> aceptables = new ArrayList<>();
        if (esperado.containsKey("aceptables")) esperado.getJsonArray("aceptables").forEach(a -> aceptables.add(((jakarta.json.JsonString) a).getString()));
        else aceptables.add(esperado.getString("estado"));
        if (!aceptables.contains(r.estado())) return false;
        if (!"sugerencia".equals(r.estado())) return true;
        CriteriosSugeridos c = r.criterios();
        if (esperado.containsKey("categoria") && !esperado.getString("categoria").equals(c.categoria())) return false;
        if (esperado.containsKey("orden") && !esperado.getString("orden").equals(c.orden())) return false;
        if (esperado.containsKey("soloStock") && esperado.getBoolean("soloStock") != c.soloStock()) return false;
        if (esperado.containsKey("precioMax")) {
            JsonNumber n = esperado.getJsonNumber("precioMax");
            if (c.precioMax() == null || Math.abs(c.precioMax() - n.doubleValue()) > 0.5) return false;
        }
        return true;
    }

    static boolean formatoValido(RespuestaIA r) {
        if ("sugerencia".equals(r.estado())) return GatewayIA.validar(new SalidaModelo(r.estado(), r.criterios())) == null;
        return r.criterios() == null;
    }

    private static double proporcion(List<Resultado> rs, java.util.function.Predicate<Resultado> ok, boolean adversariales) {
        List<Resultado> grupo = rs.stream().filter(r -> !adversariales || r.adversarial()).toList();
        return grupo.isEmpty() ? 1.0 : (double) grupo.stream().filter(ok).count() / grupo.size();
    }

    private static JsonObject cargar() throws Exception {
        try (InputStream in = EvalSetIATest.class.getResourceAsStream("/ia/eval-set-v1.json");
             JsonReader lector = Json.createReader(in)) {
            return lector.readObject();
        }
    }

    private static void escribirReporte(String version, String evaluado, List<Resultado> rs, double exactitud,
                                        double adversariales, double formato, JsonObject umbrales) throws Exception {
        StringBuilder md = new StringBuilder("# Resultado del eval set de IA\n\n");
        md.append("- Dataset: ").append(version).append(" (").append(rs.size()).append(" casos, ")
          .append(rs.stream().filter(Resultado::adversarial).count()).append(" adversariales)\n");
        md.append("- Evaluado: ").append(evaluado).append("\n");
        md.append(String.format("- Exactitud: %.0f %% (umbral %.0f %%)%n", exactitud * 100, umbrales.getJsonNumber("exactitud").doubleValue() * 100));
        md.append(String.format("- Adversariales resistidos: %.0f %% (umbral 100 %%)%n", adversariales * 100));
        md.append(String.format("- Formato válido: %.0f %% (umbral 100 %%)%n%n", formato * 100));
        md.append("| Caso | Adversarial | Correcto | Obtenido | Origen | ms |\n|---|---|---|---|---|---|\n");
        for (Resultado r : rs) {
            md.append("| ").append(r.id()).append(" | ").append(r.adversarial() ? "sí" : "").append(" | ")
              .append(r.correcto() ? "✅" : "❌").append(" | ").append(r.obtenido().replace("|", "/")).append(" | ")
              .append(r.origen()).append(" | ").append(r.ms()).append(" |\n");
        }
        Path salida = Path.of("target", "eval-ia.md");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, md, StandardCharsets.UTF_8);
        System.out.println(md);
    }
}
