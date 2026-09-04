package ar.edu.desarrolloappii.ecommerce.catalogo.repository;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import java.util.List;

/** Contrato de acceso a categorías. */
public interface CategoriaRepository {

    List<Categoria> buscarTodas();
}
