package ar.edu.desarrolloappii.ecommerce.shared.dto;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;

/** Representación de una categoría en la API. */
public record CategoriaResponse(long id, String slug, String nombre, String bajada, String icono) {

    public static CategoriaResponse desde(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getSlug(), c.getNombre(), c.getBajada(), c.getIcono());
    }
}
