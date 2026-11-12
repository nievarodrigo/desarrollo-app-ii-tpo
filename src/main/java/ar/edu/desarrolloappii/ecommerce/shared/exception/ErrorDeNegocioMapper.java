package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Traduce cualquier {@link ErrorDeNegocio} (401, 403, 409, 429...) a Problem Details. */
@Provider
public class ErrorDeNegocioMapper implements ExceptionMapper<ErrorDeNegocio> {
    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(ErrorDeNegocio error) {
        return construir(error, uriInfo.getRequestUri().getPath()).build();
    }

    static Response.ResponseBuilder construir(ErrorDeNegocio error, String instance) {
        Response.ResponseBuilder respuesta = Response.status(error.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(new ProblemDetail("about:blank", error.titulo(), error.status(),
                        error.getMessage(), instance));
        error.cabeceras().forEach(respuesta::header);
        return respuesta;
    }
}
