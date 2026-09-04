package ar.edu.desarrolloappii.ecommerce.shared.dto;

import java.util.List;

/**
 * Respuesta de {@code GET /api/productos}.
 *
 * Devuelve un objeto y no un array pelado para poder sumar metadatos
 * (hoy {@code total}; mañana paginación) sin romper a los consumidores.
 */
public record ListadoProductosResponse(List<ProductoResponse> items, int total) {

    public static ListadoProductosResponse de(List<ProductoResponse> items) {
        return new ListadoProductosResponse(items, items.size());
    }
}
