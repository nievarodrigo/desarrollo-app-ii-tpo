package ar.edu.desarrolloappii.ecommerce.catalogo.service;

import java.math.BigDecimal;

/**
 * Criterios de búsqueda del catálogo.
 *
 * Es un objeto de valor: el controller traduce los query params a esto y el
 * service trabaja con esto. Así la firma de {@code CatalogoService} no crece
 * un parámetro cada vez que agregamos un filtro, y el negocio no sabe nada de
 * HTTP.
 *
 * @param q         texto libre: nombre, marca, SKU o descripción
 * @param categoria slug de categoría, o {@code "todas"}
 * @param orden     criterio de ordenamiento
 * @param soloStock excluir los productos sin unidades
 * @param precioMax tope de precio, o {@code null} si no hay tope
 */
public record CriteriosDeBusqueda(
        String q,
        String categoria,
        Orden orden,
        boolean soloStock,
        BigDecimal precioMax) {

    public static final String TODAS = "todas";

    /** Criterios sin ningún filtro aplicado. */
    public static CriteriosDeBusqueda sinFiltros() {
        return new CriteriosDeBusqueda("", TODAS, Orden.RELEVANCIA, false, null);
    }

    /** Los mismos criterios pero sin restringir la categoría (para las facetas). */
    public CriteriosDeBusqueda sinCategoria() {
        return new CriteriosDeBusqueda(q, TODAS, orden, soloStock, precioMax);
    }

    public boolean filtraPorCategoria() {
        return categoria != null && !categoria.isBlank() && !TODAS.equals(categoria);
    }

    public boolean filtraPorTexto() {
        return q != null && !q.isBlank();
    }

    /** Criterios de ordenamiento admitidos. */
    public enum Orden {
        RELEVANCIA("relevancia"),
        PRECIO_ASC("precio-asc"),
        PRECIO_DESC("precio-desc"),
        RATING("rating"),
        NOMBRE("nombre");

        private final String valor;

        Orden(String valor) {
            this.valor = valor;
        }

        public String valor() {
            return valor;
        }

        /** Tolerante: un valor desconocido cae en relevancia en vez de romper. */
        public static Orden desde(String texto) {
            if (texto == null) return RELEVANCIA;
            for (Orden orden : values()) {
                if (orden.valor.equalsIgnoreCase(texto)) return orden;
            }
            return RELEVANCIA;
        }
    }
}
