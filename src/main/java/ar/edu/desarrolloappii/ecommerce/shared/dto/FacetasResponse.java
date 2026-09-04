package ar.edu.desarrolloappii.ecommerce.shared.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Respuesta de {@code GET /api/productos/facetas}: cuántos resultados hay por
 * categoría y entre qué precios se mueve el catálogo.
 *
 * Está separada del listado a propósito: es una consulta secundaria, la usa el
 * panel de filtros y no debe afectar el resultado principal si falla.
 */
public record FacetasResponse(
        long total,
        Map<String, Long> porCategoria,
        BigDecimal precioMin,
        BigDecimal precioMax) {
}
