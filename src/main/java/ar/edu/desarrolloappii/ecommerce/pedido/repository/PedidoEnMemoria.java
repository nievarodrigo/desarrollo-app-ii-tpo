package ar.edu.desarrolloappii.ecommerce.pedido.repository;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Estado visible de la demo; NO sustituye una base transaccional. */
@ApplicationScoped
public class PedidoEnMemoria implements PedidoRepository {
    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();

    @Override public void guardar(Pedido pedido) { pedidos.put(pedido.getId(), pedido); }
    @Override public Pedido obtener(String id) { return pedidos.get(id); }

    /** computeIfPresent es atómico: si la transición lanza, el mapa queda intacto. */
    @Override public Pedido aplicar(String id, EventoPedido evento) {
        return pedidos.computeIfPresent(id, (clave, anterior) -> anterior.transicionar(evento));
    }

    @Override public List<Pedido> listarPorEstado(String estado) {
        return pedidos.values().stream().filter(p -> estado.equals(p.getEstado())).toList();
    }
}
