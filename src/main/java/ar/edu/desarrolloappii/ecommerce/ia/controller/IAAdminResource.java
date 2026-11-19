package ar.edu.desarrolloappii.ecommerce.ia.controller;

import ar.edu.desarrolloappii.ecommerce.ia.service.GatewayIA;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.TreeMap;

/**
 * Operación de la feature de IA, sólo ADMIN (Clase 14: monitoreo y plan de apagado).
 * {@code GET /admin/ia} muestra modelo, versión del prompt, estado del circuito y métricas;
 * {@code PUT /admin/ia/habilitada} prende o apaga la IA sin redeployar.
 */
@Path("/admin/ia")
@Produces(MediaType.APPLICATION_JSON)
public class IAAdminResource {
    @Inject GatewayIA gateway;

    public record Cambio(boolean habilitada) { }

    @GET
    @RolesAllowed("ADMIN")
    public Map<String, Object> estado() {
        Map<String, Object> r = new TreeMap<>();
        r.put("habilitada", gateway.interruptor().habilitada());
        r.put("modelo", gateway.nombreModelo());
        r.put("versionPrompt", gateway.versionPrompt());
        r.put("circuito", gateway.circuito().estado().name());
        r.put("timeoutMs", GatewayIA.PRESUPUESTO_MS);
        r.put("metricas", gateway.metricas().resumen());
        return r;
    }

    @PUT
    @Path("/habilitada")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Map<String, Object> cambiar(Cambio cambio) {
        gateway.interruptor().cambiar(cambio != null && cambio.habilitada());
        return estado();
    }
}
