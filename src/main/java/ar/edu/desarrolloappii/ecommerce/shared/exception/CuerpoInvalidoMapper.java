package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Un cuerpo JSON malformado o con tipos incorrectos es culpa del cliente.
 *
 * Sin este mapper, RESTEasy lo responde como 500 con un texto plano rotulado
 * {@code application/json}. Acá se devuelve 400 en formato Problem Details.
 */
@Provider
public class CuerpoInvalidoMapper implements ExceptionMapper<ProcessingException> {
    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(ProcessingException excepcion) {
        ProblemDetail problema = problema(excepcion, uriInfo.getRequestUri().getPath());
        return Response.status(problema.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problema)
                .build();
    }

    static ProblemDetail problema(ProcessingException excepcion, String instance) {
        for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
            if (causa instanceof JsonbException) {
                return ProblemDetail.badRequest("El cuerpo no es JSON válido o tiene tipos incorrectos.", instance);
            }
        }
        return new ProblemDetail("about:blank", "Error interno", 500,
                "No se pudo procesar la solicitud.", instance);
    }
}
