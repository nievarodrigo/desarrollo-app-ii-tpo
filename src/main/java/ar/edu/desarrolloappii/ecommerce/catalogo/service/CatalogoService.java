package ar.edu.desarrolloappii.ecommerce.catalogo.service;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import java.util.List;
import java.util.Map;

/**
 * Contrato del componente Catálogo.
 *
 * Es la única puerta de entrada al catálogo desde afuera del componente: la
 * capa de presentación depende de esta interfaz y no conoce ni el repositorio
 * ni la base de datos.
 */
public interface CatalogoService {

    /** Productos que cumplen los criterios, ya filtrados y ordenados. */
    List<Producto> buscar(CriteriosDeBusqueda criterios);

    /**
     * Producto por id.
     *
     * @throws ar.edu.desarrolloappii.ecommerce.shared.exception.RecursoNoEncontradoException
     *         si no existe o está inactivo
     */
    Producto obtener(long id);

    List<Categoria> listarCategorias();

    /** Cantidad de resultados por categoría, ignorando el filtro de categoría. */
    Map<String, Long> contarPorCategoria(CriteriosDeBusqueda criterios);

    /** Precio más bajo y más alto del catálogo activo. */
    RangoDePrecios rangoDePrecios();
}
