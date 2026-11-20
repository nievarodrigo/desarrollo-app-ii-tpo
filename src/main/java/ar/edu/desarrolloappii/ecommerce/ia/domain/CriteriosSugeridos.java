package ar.edu.desarrolloappii.ecommerce.ia.domain;

/**
 * Filtros del catálogo que la IA (o el intérprete de reglas) sugiere. Son una SUGERENCIA:
 * el usuario decide si los aplica, y nunca ejecutan nada por sí mismos.
 * Los mismos nombres que los query params de {@code GET /api/productos}.
 */
public record CriteriosSugeridos(String q, String categoria, String orden, boolean soloStock, Double precioMax) {
    public static CriteriosSugeridos vacios() {
        return new CriteriosSugeridos("", "todas", "relevancia", false, null);
    }
}
