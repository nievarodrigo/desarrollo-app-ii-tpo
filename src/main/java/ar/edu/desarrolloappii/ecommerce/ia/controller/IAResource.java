package ar.edu.desarrolloappii.ecommerce.ia.controller;

import ar.edu.desarrolloappii.ecommerce.ia.domain.RespuestaIA;
import ar.edu.desarrolloappii.ecommerce.ia.service.GatewayIA;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * {@code POST /api/ia/interpretar} — "búsqueda en lenguaje natural".
 * Recibe {@code {"consulta": "auriculares baratos hasta 50 mil"}} y devuelve filtros sugeridos.
 * Siempre responde 200 con un resultado útil: si la IA falla, contesta el fallback.
 */
@Path("/ia")
@Produces(MediaType.APPLICATION_JSON)
public class IAResource {
    @Inject GatewayIA gateway;

    public record Consulta(String consulta) { }

    @POST
    @Path("/interpretar")
    @Consumes(MediaType.APPLICATION_JSON)
    public RespuestaIA interpretar(Consulta cuerpo) {
        return gateway.interpretar(cuerpo == null ? null : cuerpo.consulta());
    }
}
