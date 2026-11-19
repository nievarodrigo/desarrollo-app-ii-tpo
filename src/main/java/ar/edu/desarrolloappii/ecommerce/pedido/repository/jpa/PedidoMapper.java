package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;

/** Traduce pedidos y entidades con Data Mapper para mantener el dominio independiente de JPA. */
public final class PedidoMapper {
    private PedidoMapper() {
    }

    /** Copia el pedido a una entidad para que el Repository pueda persistirlo sin acoplar el dominio. */
    public static PedidoEntity entidad(Pedido pedido) {
        PedidoEntity entidad = new PedidoEntity();
        entidad.id = pedido.getId();
        entidad.estado = pedido.getEstado();
        entidad.cliente = pedido.getCliente();
        entidad.total = pedido.getTotal();
        for (var itemPedido : pedido.getItems()) {
            var item = new PedidoEntity.Item();
            item.producto = itemPedido.getIdProducto();
            item.cantidad = itemPedido.getCantidad();
            entidad.items.add(item);
        }
        return entidad;
    }

    /** Reconstruye el pedido, o devuelve null si falta, para que el Repository no exponga entidades. */
    public static Pedido dominio(PedidoEntity entidad) {
        return entidad == null ? null : new Pedido(entidad.id, entidad.estado,
                entidad.items.stream()
                        .map(item -> new SolicitudPedido.Item(item.producto, item.cantidad))
                        .toList(),
                entidad.total, entidad.cliente);
    }
}
