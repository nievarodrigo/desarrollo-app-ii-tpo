package ar.edu.desarrolloappii.ecommerce.catalogo.controller;

import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CriteriosDeBusqueda;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.RangoDePrecios;
import ar.edu.desarrolloappii.ecommerce.shared.dto.FacetasResponse;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ListadoProductosResponse;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ProductoResponse;
import ar.edu.desarrolloappii.ecommerce.shared.http.Etags;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Capa de presentación del catálogo.
 *
 * Un controller delgado, como pide la rúbrica: traduce HTTP a un caso de uso,
 * arma el DTO de salida y nada más. No filtra, no ordena, no decide qué es
 * visible — todo eso es del {@link CatalogoService}.
 *
 * Cada GET devuelve un {@code ETag}. Si el cliente manda {@code If-None-Match}
 * con ese mismo valor, la respuesta es 304 sin cuerpo: ahorra ancho de banda y
 * el navegador reutiliza lo que ya tenía.
 */
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @Inject
    CatalogoService catalogo;

    @Context
    Request request;

    /** {@code GET /api/productos?q=&categoria=&orden=&soloStock=&precioMax=} */
    @GET
    public Response listar(
            @QueryParam("q") @DefaultValue("") String q,
            @QueryParam("categoria") @DefaultValue("todas") String categoria,
            @QueryParam("orden") @DefaultValue("relevancia") String orden,
            @QueryParam("soloStock") @DefaultValue("false") boolean soloStock,
            @QueryParam("precioMax") BigDecimal precioMax) {

        List<ProductoResponse> items = catalogo.buscar(criterios(q, categoria, orden, soloStock, precioMax))
                .stream()
                .map(ProductoResponse::desde)
                .toList();

        return conEtag(ListadoProductosResponse.de(items));
    }

    /** {@code GET /api/productos/facetas} — conteos y rango de precios. */
    @GET
    @Path("/facetas")
    public Response facetas(
            @QueryParam("q") @DefaultValue("") String q,
            @QueryParam("categoria") @DefaultValue("todas") String categoria,
            @QueryParam("soloStock") @DefaultValue("false") boolean soloStock,
            @QueryParam("precioMax") BigDecimal precioMax) {

        CriteriosDeBusqueda criterios = criterios(q, categoria, "relevancia", soloStock, precioMax);
        Map<String, Long> porCategoria = catalogo.contarPorCategoria(criterios);
        long total = porCategoria.values().stream().mapToLong(Long::longValue).sum();
        RangoDePrecios rango = catalogo.rangoDePrecios();

        return conEtag(new FacetasResponse(total, porCategoria, rango.min(), rango.max()));
    }

    /**
     * {@code GET /api/productos/{id}}
     * Si no existe, el service lanza la excepción de dominio y el
     * {@code RecursoNoEncontradoMapper} la convierte en 404.
     */
    @GET
    @Path("/{id}")
    public Response detalle(@PathParam("id") long id) {
        return conEtag(ProductoResponse.desde(catalogo.obtener(id)));
    }

    /** 200 con ETag, o 304 si el cliente ya tiene esta misma versión. */
    private Response conEtag(Object cuerpo) {
        EntityTag etag = new EntityTag(Etags.de(cuerpo));
        CacheControl revalidar = new CacheControl();
        revalidar.setNoCache(true); // puede guardarlo, pero debe revalidar con If-None-Match
        Response.ResponseBuilder noModificado = request.evaluatePreconditions(etag);
        if (noModificado != null) return noModificado.cacheControl(revalidar).build();
        return Response.ok(cuerpo).tag(etag).cacheControl(revalidar).build();
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
