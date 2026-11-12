package ar.edu.desarrolloappii.ecommerce.pedido.repository;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Estado visible de la demo; NO sustituye una base transaccional. */
@ApplicationScoped
public class PedidoEnMemoria {
    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();

    public void guardar(Pedido pedido) { pedidos.put(pedido.getId(), pedido); }
    public Pedido obtener(String id) { return pedidos.get(id); }
    public void aplicar(String id, EventoPedido evento) {
        pedidos.computeIfPresent(id, (clave, anterior) ->
                anterior.transicionar(evento));
    }
}
