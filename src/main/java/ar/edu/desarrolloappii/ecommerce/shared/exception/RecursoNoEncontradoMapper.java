package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    @Override
    public Response toResponse(RecursoNoEncontradoException excepcion) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(404, "recurso-no-encontrado", excepcion.getMessage()))
                .build();
    }
}
