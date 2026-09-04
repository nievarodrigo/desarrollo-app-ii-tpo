package ar.edu.desarrolloappii.ecommerce.catalogo.repository;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a productos.
 *
 * El negocio depende de ESTA interfaz, nunca de una implementación concreta.
 * Hoy la cumple {@link CatalogoEnMemoria}; mañana la cumplirá una versión con
 * JPA y no habrá que tocar {@code CatalogoService}. Ese es el punto del patrón
 * Repository y de programar contra contratos.
 */
public interface ProductoRepository {

    /** Todos los productos, sin filtrar. El filtrado es decisión del negocio. */
    List<Producto> buscarTodos();

    Optional<Producto> buscarPorId(long id);
}
