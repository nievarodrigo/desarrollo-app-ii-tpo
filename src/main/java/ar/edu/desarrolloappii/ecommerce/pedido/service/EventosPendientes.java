package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Eventos que no se pudieron publicar (broker caído). {@link ReenvioDePedidos} los reintenta
 * cada 15 s, igual que a los pedidos en ERROR_ENVIO: confirmar un pedido nunca falla por el broker.
 */
@ApplicationScoped
public class EventosPendientes {
    private final ConcurrentLinkedQueue<PedidoConfirmado> pendientes = new ConcurrentLinkedQueue<>();

    public void agregar(PedidoConfirmado evento) { pendientes.add(evento); }
    public PedidoConfirmado siguiente() { return pendientes.peek(); }
    public void quitar(PedidoConfirmado evento) { pendientes.remove(evento); }
    public List<PedidoConfirmado> listar() { return List.copyOf(pendientes); }
}
