package ar.edu.desarrolloappii.ecommerce.pedido.controller;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.service.PedidoService;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.ws.rs.core.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/** REST síncrono para aceptar el comando; procesamiento asíncrono por JMS. */
@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
public class PedidoResource {
    @Inject PedidoService servicio;
    @Context UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(SolicitudPedido solicitud) {
        try {
            Pedido pedido = servicio.crear(solicitud);
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
    public Pedido obtener(@PathParam("id") String id) {
        Pedido pedido = servicio.obtener(id);
        if (pedido == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .type(ProblemDetail.MEDIA_TYPE)
                    .entity(ProblemDetail.notFound("No existe un pedido con id " + id,
                            uriInfo.getRequestUri().getPath()))
                    .build());
        }
        return pedido;
    }
}
