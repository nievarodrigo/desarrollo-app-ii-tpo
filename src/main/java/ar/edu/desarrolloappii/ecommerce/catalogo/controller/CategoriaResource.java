package ar.edu.desarrolloappii.ecommerce.catalogo.controller;

import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.shared.dto.CategoriaResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** {@code GET /api/categorias} */
@Path("/categorias")
@Produces(MediaType.APPLICATION_JSON)
public class CategoriaResource {

    @Inject
    CatalogoService catalogo;

    @GET
    public List<CategoriaResponse> listar() {
        return catalogo.listarCategorias().stream()
                .map(CategoriaResponse::desde)
                .toList();
    }
}
