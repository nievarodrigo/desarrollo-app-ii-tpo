package ar.edu.desarrolloappii.ecommerce.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * {@code GET /api/salud} — le dice al front si hay backend detrás.
 *
 * La misma UI se publica de dos formas: como WAR en WildFly (con esta API) y
 * como sitio estático (sin ella). En vez de adivinar por hostname o de provocar
 * un 404 a propósito, el front pregunta acá una sola vez al arrancar. En el
 * despliegue estático esta ruta la resuelve un JSON fijo que declara
 * {@code disponible: false}.
 */
@Path("/salud")
@Produces(MediaType.APPLICATION_JSON)
public class SaludResource {

    /** Respuesta de {@code /api/salud}. */
    public record Estado(boolean disponible, String origen, String version) {
    }

    @GET
    public Estado consultar() {
        return new Estado(true, "jakarta-ee", "1.0.0");
    }
}
