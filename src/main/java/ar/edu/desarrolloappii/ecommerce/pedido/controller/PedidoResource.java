package ar.edu.desarrolloappii.ecommerce.pedido.controller;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.service.IdempotenciaPedidos;
import ar.edu.desarrolloappii.ecommerce.pedido.service.PedidoService;
import ar.edu.desarrolloappii.ecommerce.seguridad.controller.AutenticacionFilter;
import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import ar.edu.desarrolloappii.ecommerce.shared.exception.PrecondicionFallidaException;
import ar.edu.desarrolloappii.ecommerce.shared.http.Etags;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.Comparator;

/**
 * REST síncrono para aceptar el comando; procesamiento asíncrono por JMS.
 * Todas las operaciones exigen JWT. Los cambios de estado pasan por el patrón State
 * del dominio: si el pedido no admite el evento, responde 409 (TransicionInvalidaException).
 *
 * Resiliencia del contrato HTTP:
 * <ul>
 *   <li>{@code POST /pedidos} exige {@code Idempotency-Key}: un reintento no duplica la compra.</li>
 *   <li>Las respuestas llevan {@code ETag}; un GET con {@code If-None-Match} vigente responde 304.</li>
 *   <li>Cancelar/confirmar aceptan {@code If-Match}: si el pedido cambió mientras tanto, 412.</li>
 * </ul>
 */
@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
public class PedidoResource {
    @Inject PedidoService servicio;
    @Inject IdempotenciaPedidos idempotencia;
    @Context UriInfo uriInfo;
    @Context SecurityContext seguridad;
    @Context Request request;

    /** Sólo CLIENTE compra; el dueño del pedido sale del token, no del cuerpo. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("CLIENTE")
    public Response crear(@HeaderParam("Idempotency-Key") String clave, SolicitudPedido solicitud) {
        try {
            var identidad = AutenticacionFilter.identidadDe(seguridad);
            IdempotenciaPedidos.Resultado resultado = idempotencia.ejecutar(
                    clave, identidad.email(), huellaDe(solicitud), () -> servicio.crear(solicitud, identidad));
            Pedido pedido = resultado.pedido();
            return Response.accepted(pedido)
                    .location(uriInfo.getAbsolutePathBuilder().path(pedido.getId()).build())
                    .tag(etag(pedido))
                    .header("Idempotency-Key", clave)
                    .header("Idempotent-Replayed", Boolean.toString(resultado.repetido()))
                    .build();
        } catch (IllegalArgumentException error) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(ProblemDetail.MEDIA_TYPE)
                    .entity(ProblemDetail.badRequest(error.getMessage(), uriInfo.getRequestUri().getPath()))
                    .build();
        }
    }

    /** Con {@code If-None-Match} igual al ETag actual responde 304 sin cuerpo. */
    @GET
    @Path("/{id}")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Response obtener(@PathParam("id") String id) {
        Pedido pedido = servicio.obtener(id, AutenticacionFilter.identidadDe(seguridad));
        EntityTag etag = etag(pedido);
        CacheControl privado = new CacheControl();
        privado.setPrivate(true);
        privado.setNoCache(true);
        Response.ResponseBuilder noModificado = request.evaluatePreconditions(etag);
        if (noModificado != null) return noModificado.cacheControl(privado).build();
        return Response.ok(pedido).tag(etag).cacheControl(privado).build();
    }

    /** El dueño (o un ADMIN) cancela. Si ya está CONFIRMADO, State lo rechaza con 409. */
    @POST
    @Path("/{id}/cancelar")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Response cancelar(@PathParam("id") String id, @HeaderParam("If-Match") String ifMatch) {
        return aplicar(id, EventoPedido.CANCELAR, ifMatch);
    }

    /** Sólo ADMIN confirma. Sólo vale desde PROCESADO: antes de eso (o cancelado) → 409. */
    @POST
    @Path("/{id}/confirmar")
    @RolesAllowed("ADMIN")
    public Response confirmar(@PathParam("id") String id, @HeaderParam("If-Match") String ifMatch) {
        return aplicar(id, EventoPedido.CONFIRMAR, ifMatch);
    }

    /** Control de concurrencia optimista: el cambio sólo se aplica sobre la versión que el cliente vio. */
    private Response aplicar(String id, EventoPedido evento, String ifMatch) {
        var identidad = AutenticacionFilter.identidadDe(seguridad);
        if (ifMatch != null && !ifMatch.isBlank()) {
            Pedido actual = servicio.obtener(id, identidad);
            if (!Etags.coincide(ifMatch, etag(actual).getValue())) {
                throw new PrecondicionFallidaException(
                        "El pedido cambió desde la última vez que lo consultaste. Volvé a pedirlo y reintentá.");
            }
        }
        Pedido actualizado = servicio.aplicarEvento(id, evento, identidad);
        return Response.ok(actualizado).tag(etag(actualizado)).build();
    }

    static EntityTag etag(Pedido pedido) {
        return new EntityTag(Etags.huella(pedido.getId() + "|" + pedido.getEstado() + "|" + pedido.getTotal()));
    }

    /** Huella del cuerpo, independiente del orden de los ítems: detecta reusar la clave con otra compra. */
    static String huellaDe(SolicitudPedido solicitud) {
        if (solicitud == null || solicitud.getItems() == null) return "vacio";
        StringBuilder texto = new StringBuilder();
        solicitud.getItems().stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingLong(SolicitudPedido.Item::getIdProducto))
                .forEach(i -> texto.append(i.getIdProducto()).append('x').append(i.getCantidad()).append(';'));
        return Etags.huella(texto.toString());
    }
}
