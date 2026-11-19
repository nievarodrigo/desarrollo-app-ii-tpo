package ar.edu.desarrolloappii.ecommerce.shared.http;

import ar.edu.desarrolloappii.ecommerce.shared.exception.PrecondicionFallidaException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/** Evalúa If-None-Match / If-Match de un GET: 304 sin cuerpo, o 412 en Problem Details. */
public final class Precondiciones {
    private Precondiciones() { }

    public static Response evaluar(Request request, EntityTag etag, Response.ResponseBuilder ok, CacheControl cache) {
        Response.ResponseBuilder corte = request.evaluatePreconditions(etag);
        if (corte == null) return ok.tag(etag).cacheControl(cache).build();
        Response respuesta = corte.build();
        if (respuesta.getStatus() == 412) {
            throw new PrecondicionFallidaException("El recurso cambió: el If-Match no coincide con la versión actual.");
        }
        return corte.tag(etag).cacheControl(cache).build(); // 304
    }
}
