package ar.edu.desarrolloappii.ecommerce.ia.controller;

import ar.edu.desarrolloappii.ecommerce.ia.domain.RespuestaIA;
import ar.edu.desarrolloappii.ecommerce.ia.service.GatewayIA;
import ar.edu.desarrolloappii.ecommerce.ia.service.LimiteDeConsultas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * {@code POST /api/ia/interpretar} — "búsqueda en lenguaje natural".
 * Recibe {@code {"consulta": "auriculares baratos hasta 50 mil"}} y devuelve filtros sugeridos.
 * Responde 200 con un resultado útil aunque la IA falle (contesta el fallback).
 * Público, pero con límite de uso por IP (429) para que nadie gaste tokens en un loop.
 */
@Path("/ia")
@Produces(MediaType.APPLICATION_JSON)
public class IAResource {
    @Inject GatewayIA gateway;
    @Inject LimiteDeConsultas limite;
    @Context HttpServletRequest http;

    public record Consulta(String consulta) { }

    @POST
    @Path("/interpretar")
    @Consumes(MediaType.APPLICATION_JSON)
    public RespuestaIA interpretar(Consulta cuerpo) {
        limite.registrar(http == null ? null : http.getRemoteAddr());
        return gateway.interpretar(cuerpo == null ? null : cuerpo.consulta());
    }
}
