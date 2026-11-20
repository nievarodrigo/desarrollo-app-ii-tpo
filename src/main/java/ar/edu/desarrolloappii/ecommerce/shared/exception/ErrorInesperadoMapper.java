package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Red de seguridad: cualquier excepción sin mapper propio (404/405/415 del framework,
 * EJBException, errores inesperados) sale igual en formato Problem Details.
 * Los mappers más específicos (negocio, 404 de dominio, cuerpo inválido) siguen ganando.
 */
@Provider
public class ErrorInesperadoMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ErrorInesperadoMapper.class.getName());

    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(Exception error) {
        Problemas.Resultado resultado = Problemas.desde(error, uriInfo == null ? null : uriInfo.getRequestUri().getPath());
        ProblemDetail problema = resultado.problema();
        if (problema.status() >= 500) LOG.log(Level.SEVERE, "Error no controlado en la API", error);
        Response.ResponseBuilder respuesta = Response.status(problema.status())
                .type(ProblemDetail.MEDIA_TYPE).entity(problema);
        resultado.cabeceras().forEach(respuesta::header);
        return respuesta.build();
    }
}
