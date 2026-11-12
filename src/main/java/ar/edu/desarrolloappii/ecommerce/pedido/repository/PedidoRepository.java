package ar.edu.desarrolloappii.ecommerce.pedido.repository;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;

/**
 * Puerto de persistencia de pedidos (D de SOLID: el service y el MDB dependen
 * de esta interfaz, no de dónde se guardan los datos).
 */
public interface PedidoRepository {
    void guardar(Pedido pedido);

    /** @return el pedido o {@code null} si no existe. */
    Pedido obtener(String id);

    /**
     * Aplica el evento de forma atómica: valida la transición y reemplaza el pedido.
     * Si la transición es inválida lanza TransicionInvalidaException y el pedido no cambia.
     *
     * @return el pedido ya transicionado, o {@code null} si no existe.
     */
    Pedido aplicar(String id, EventoPedido evento);
}
