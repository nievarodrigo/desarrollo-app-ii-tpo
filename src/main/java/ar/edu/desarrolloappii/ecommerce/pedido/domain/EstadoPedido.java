package ar.edu.desarrolloappii.ecommerce.pedido.domain;

/** State: cada estado decide qué eventos acepta; no conoce REST ni JMS. */
public interface EstadoPedido {
    String nombre();
    EstadoPedido aplicar(EventoPedido evento);
}
