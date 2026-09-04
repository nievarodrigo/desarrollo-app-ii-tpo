package ar.edu.desarrolloappii.ecommerce.shared.dto;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import java.math.BigDecimal;
import java.util.List;

/**
 * Representación de un producto en la API.
 *
 * Existe para NO exponer el objeto de dominio: mañana {@code Producto} va a ser
 * una entidad JPA con relaciones y campos internos, y el contrato de la API no
 * tiene por qué cambiar cuando eso pase. Es el patrón DTO de la rúbrica.
 */
public record ProductoResponse(
        long id,
        String sku,
        String nombre,
        String marca,
        String categoria,
        BigDecimal precio,
        BigDecimal precioAnterior,
        int stock,
        double rating,
        int resenias,
        boolean activo,
        String arte,
        String descripcion,
        List<String> destacados,
        List<String> etiquetas) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(
                p.getId(),
                p.getSku(),
                p.getNombre(),
                p.getMarca(),
                p.getCategoria(),
                p.getPrecio(),
                p.getPrecioAnterior(),
                p.getStock(),
                p.getRating(),
                p.getResenias(),
                p.isActivo(),
                p.getArte(),
                p.getDescripcion(),
                List.copyOf(p.getDestacados()),
                List.copyOf(p.getEtiquetas()));
    }
}
