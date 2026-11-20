package ar.edu.desarrolloappii.ecommerce.pedido.controller;

import ar.edu.desarrolloappii.ecommerce.pedido.service.ConsumosDeEventos;
import ar.edu.desarrolloappii.ecommerce.pedido.service.RelayOutbox;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.TreeMap;

/** {@code GET /admin/eventos} (ADMIN): qué hizo cada suscriptor del tópico y qué eventos esperan reenvío. */
@Path("/admin/eventos")
@Produces(MediaType.APPLICATION_JSON)
public class EventosResource {
    @Inject ConsumosDeEventos consumos;
    @Inject RelayOutbox pendientes;

    @GET
    @RolesAllowed("ADMIN")
    public Map<String, Object> resumen() {
        Map<String, Object> r = new TreeMap<>(consumos.resumen());
        r.put("pendientesDePublicar", pendientes.contarPendientes());
        return r;
    }
}
