package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.CriteriosSugeridos;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FALLBACK del gateway: interpreta la consulta con reglas fijas, sin IA.
 *
 * Es más limitado que el modelo pero responde en microsegundos, no cuesta nada y no
 * depende de la red. Si el proveedor tarda más de 5 s, falla o no está configurado,
 * el usuario igual recibe una sugerencia razonable en vez de un error.
 */
public class InterpreteDeReglas {
    private static final Map<String, String> CATEGORIAS = Map.ofEntries(
            Map.entry("auricular", "audio"), Map.entry("parlante", "audio"), Map.entry("audio", "audio"),
            Map.entry("musica", "audio"), Map.entry("sonido", "audio"),
            Map.entry("notebook", "tecnologia"), Map.entry("celular", "tecnologia"), Map.entry("monitor", "tecnologia"),
            Map.entry("tablet", "tecnologia"), Map.entry("tecnologia", "tecnologia"), Map.entry("mouse", "tecnologia"),
            Map.entry("teclado", "tecnologia"),
            Map.entry("cafetera", "hogar"), Map.entry("cocina", "hogar"), Map.entry("hogar", "hogar"),
            Map.entry("casa", "hogar"),
            Map.entry("oficina", "oficina"), Map.entry("silla", "oficina"), Map.entry("escritorio", "oficina"),
            Map.entry("deporte", "deportes"), Map.entry("deportes", "deportes"), Map.entry("bici", "deportes"),
            Map.entry("pelota", "deportes"), Map.entry("correr", "deportes"));
    private static final Set<String> BARATO = Set.of("barato", "barata", "baratos", "baratas", "economico", "economica", "economicos");
    private static final Set<String> CARO = Set.of("caro", "cara", "premium", "gama alta");
    private static final Set<String> VALORADO = Set.of("mejor", "valorado", "valorados", "puntuado", "recomendado", "recomendados");
    private static final Set<String> STOCK = Set.of("stock", "disponible", "disponibles", "ya", "hoy");
    private static final Set<String> RELLENO = Set.of("quiero", "busco", "necesito", "un", "una", "unos", "unas", "el", "la",
            "los", "las", "de", "del", "para", "con", "que", "y", "o", "en", "mi", "me", "algo", "hasta", "menos", "max",
            "maximo", "pesos", "mil", "por", "favor", "sea", "sean", "esten", "este", "hay", "tenga", "tengan", "muy", "mas");
    private static final Pattern PRECIO = Pattern.compile(
            "(?:hasta|menos de|max(?:imo)?|por debajo de|\\$)\\s*\\$?\\s*(\\d+(?:[.,]\\d{3})*)\\s*(k|mil|lucas)?");

    public Optional<CriteriosSugeridos> interpretar(String consulta) {
        String texto = normalizar(consulta);
        Double precioMax = precio(texto);
        String sinPrecio = PRECIO.matcher(texto).replaceAll(" ");
        String categoria = "todas";
        String orden = "relevancia";
        boolean soloStock = false;
        List<String> libres = new ArrayList<>();
        for (String palabra : sinPrecio.split("[^a-z0-9]+")) {
            if (palabra.isBlank() || palabra.matches("\\d+")) continue;
            String singular = palabra.endsWith("es") && CATEGORIAS.containsKey(palabra.substring(0, palabra.length() - 2))
                    ? palabra.substring(0, palabra.length() - 2)
                    : palabra.endsWith("s") && CATEGORIAS.containsKey(palabra.substring(0, palabra.length() - 1))
                    ? palabra.substring(0, palabra.length() - 1) : palabra;
            if (CATEGORIAS.containsKey(singular)) {
                if (categoria.equals("todas")) categoria = CATEGORIAS.get(singular);
                if (!singular.equals(CATEGORIAS.get(singular))) libres.add(singular);
            } else if (BARATO.contains(palabra)) orden = "precio-asc";
            else if (CARO.contains(palabra)) orden = "precio-desc";
            else if (VALORADO.contains(palabra)) orden = "rating";
            else if (STOCK.contains(palabra)) soloStock = true;
            else if (!RELLENO.contains(palabra) && palabra.length() > 2) libres.add(palabra);
        }
        // Sólo el sustantivo del producto va al texto: el catálogo exige TODAS las palabras.
        String q = libres.isEmpty() ? "" : libres.get(0);
        // Sin ninguna señal concreta (categoría, precio, orden o stock) no hay nada que sugerir:
        // devolver sólo el texto sería hacer pasar una búsqueda común por "interpretación".
        boolean entendioAlgo = !categoria.equals("todas") || precioMax != null || !orden.equals("relevancia") || soloStock;
        return entendioAlgo ? Optional.of(new CriteriosSugeridos(q, categoria, orden, soloStock, precioMax)) : Optional.empty();
    }

    static Double precio(String texto) {
        Matcher m = PRECIO.matcher(texto);
        if (!m.find()) return null;
        double valor = Double.parseDouble(m.group(1).replace(".", "").replace(",", ""));
        if (m.group(2) != null) valor *= 1000;
        return valor > 0 && valor <= 1_000_000_000 ? valor : null;
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
