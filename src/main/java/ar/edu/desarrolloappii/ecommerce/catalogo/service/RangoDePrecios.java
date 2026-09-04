package ar.edu.desarrolloappii.ecommerce.catalogo.service;

import java.math.BigDecimal;

/** Precio mínimo y máximo del catálogo, para el filtro deslizante del front. */
public record RangoDePrecios(BigDecimal min, BigDecimal max) {

    public static RangoDePrecios vacio() {
        return new RangoDePrecios(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
