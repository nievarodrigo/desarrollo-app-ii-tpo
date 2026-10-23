package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduce una excepción de negocio a una respuesta HTTP.
 *
 * Es el único lugar donde ese error de dominio se convierte en un 404: mantiene
 * a los services libres de JAX-RS y a los controllers libres de try/catch.
 */
@Provider
public class RecursoNoEncontradoMapper implements ExceptionMapper<RecursoNoEncontradoException> {
    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(RecursoNoEncontradoException excepcion) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(ProblemDetail.notFound(excepcion.getMessage(), uriInfo.getRequestUri().getPath()))
                .build();
    }
}
