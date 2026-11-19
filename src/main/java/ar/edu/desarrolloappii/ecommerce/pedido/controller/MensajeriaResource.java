package ar.edu.desarrolloappii.ecommerce.pedido.controller;

import ar.edu.desarrolloappii.ecommerce.pedido.service.BuzonMensajesMuertos;
import ar.edu.desarrolloappii.ecommerce.pedido.service.ReenvioDePedidos;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/** Operación de la mensajería, sólo ADMIN: ver la DLQ y forzar el reenvío de pedidos trabados. */
@Path("/admin/mensajeria")
@Produces(MediaType.APPLICATION_JSON)
public class MensajeriaResource {
    @Inject BuzonMensajesMuertos buzon;
    @Inject ReenvioDePedidos reenvio;

    @GET
    @Path("/dlq")
    @RolesAllowed("ADMIN")
    public List<BuzonMensajesMuertos.MensajeMuerto> dlq() {
        return buzon.listar();
    }

    /** No espera al timer de 15 s: útil en la demo de "se apagó el broker y volvió". */
    @POST
    @Path("/reenviar")
    @RolesAllowed("ADMIN")
    public Map<String, Integer> reenviar() {
        return Map.of("republicados", reenvio.reintentarPendientes());
    }
}
