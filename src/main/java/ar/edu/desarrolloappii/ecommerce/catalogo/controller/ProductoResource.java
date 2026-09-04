package ar.edu.desarrolloappii.ecommerce.catalogo.controller;

import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CriteriosDeBusqueda;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.RangoDePrecios;
import ar.edu.desarrolloappii.ecommerce.shared.dto.FacetasResponse;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ListadoProductosResponse;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ProductoResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Capa de presentación del catálogo.
 *
 * Un controller delgado, como pide la rúbrica: traduce HTTP a un caso de uso,
 * arma el DTO de salida y nada más. No filtra, no ordena, no decide qué es
 * visible — todo eso es del {@link CatalogoService}.
 */
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @Inject
    CatalogoService catalogo;

    /** {@code GET /api/productos?q=&categoria=&orden=&soloStock=&precioMax=} */
    @GET
    public ListadoProductosResponse listar(
            @QueryParam("q") @DefaultValue("") String q,
            @QueryParam("categoria") @DefaultValue("todas") String categoria,
            @QueryParam("orden") @DefaultValue("relevancia") String orden,
            @QueryParam("soloStock") @DefaultValue("false") boolean soloStock,
            @QueryParam("precioMax") BigDecimal precioMax) {

        List<ProductoResponse> items = catalogo.buscar(criterios(q, categoria, orden, soloStock, precioMax))
                .stream()
                .map(ProductoResponse::desde)
                .toList();

        return ListadoProductosResponse.de(items);
    }

    /** {@code GET /api/productos/facetas} — conteos y rango de precios. */
    @GET
    @Path("/facetas")
    public FacetasResponse facetas(
            @QueryParam("q") @DefaultValue("") String q,
            @QueryParam("categoria") @DefaultValue("todas") String categoria,
            @QueryParam("soloStock") @DefaultValue("false") boolean soloStock,
            @QueryParam("precioMax") BigDecimal precioMax) {

        CriteriosDeBusqueda criterios = criterios(q, categoria, "relevancia", soloStock, precioMax);
        Map<String, Long> porCategoria = catalogo.contarPorCategoria(criterios);
        long total = porCategoria.values().stream().mapToLong(Long::longValue).sum();
        RangoDePrecios rango = catalogo.rangoDePrecios();

        return new FacetasResponse(total, porCategoria, rango.min(), rango.max());
    }

    /**
     * {@code GET /api/productos/{id}}
     * Si no existe, el service lanza la excepción de dominio y el
     * {@code RecursoNoEncontradoMapper} la convierte en 404.
     */
    @GET
    @Path("/{id}")
    public ProductoResponse detalle(@PathParam("id") long id) {
        return ProductoResponse.desde(catalogo.obtener(id));
    }

    /** Única responsabilidad "propia" del controller: validar el formato de entrada. */
    private CriteriosDeBusqueda criterios(String q, String categoria, String orden,
                                          boolean soloStock, BigDecimal precioMax) {
        return new CriteriosDeBusqueda(
                q == null ? "" : q.trim(),
                categoria == null || categoria.isBlank() ? CriteriosDeBusqueda.TODAS : categoria.trim(),
                CriteriosDeBusqueda.Orden.desde(orden),
                soloStock,
                precioMax != null && precioMax.signum() > 0 ? precioMax : null);
    }
}
