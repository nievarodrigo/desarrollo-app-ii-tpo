package ar.edu.desarrolloappii.ecommerce.pedido.controller;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.service.PedidoService;
import ar.edu.desarrolloappii.ecommerce.seguridad.controller.AutenticacionFilter;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST síncrono para aceptar el comando; procesamiento asíncrono por JMS.
 * Todas las operaciones exigen JWT. Los cambios de estado pasan por el patrón State
 * del dominio: si el pedido no admite el evento, responde 409 (TransicionInvalidaException).
 */
@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
public class PedidoResource {
    @Inject PedidoService servicio;
    @Context UriInfo uriInfo;
    @Context SecurityContext seguridad;

    /** Sólo CLIENTE compra; el dueño del pedido sale del token, no del cuerpo. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("CLIENTE")
    public Response crear(SolicitudPedido solicitud) {
        try {
            Pedido pedido = servicio.crear(solicitud, AutenticacionFilter.identidadDe(seguridad));
            return Response.accepted(pedido).build();
        } catch (IllegalArgumentException error) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(ProblemDetail.MEDIA_TYPE)
                    .entity(ProblemDetail.badRequest(error.getMessage(), uriInfo.getRequestUri().getPath()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Pedido obtener(@PathParam("id") String id) {
        return servicio.obtener(id, AutenticacionFilter.identidadDe(seguridad));
    }

    /** El dueño (o un ADMIN) cancela. Si ya está CONFIRMADO, State lo rechaza con 409. */
    @POST
    @Path("/{id}/cancelar")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Pedido cancelar(@PathParam("id") String id) {
        return servicio.aplicarEvento(id, EventoPedido.CANCELAR, AutenticacionFilter.identidadDe(seguridad));
    }

    /** Sólo ADMIN confirma. Sólo vale desde PROCESADO: antes de eso (o cancelado) → 409. */
    @POST
    @Path("/{id}/confirmar")
    @RolesAllowed("ADMIN")
    public Pedido confirmar(@PathParam("id") String id) {
        return servicio.aplicarEvento(id, EventoPedido.CONFIRMAR, AutenticacionFilter.identidadDe(seguridad));
    }
}
