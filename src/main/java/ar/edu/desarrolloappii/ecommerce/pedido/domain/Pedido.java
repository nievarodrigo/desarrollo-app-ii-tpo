package ar.edu.desarrolloappii.ecommerce.pedido.domain;

import java.math.BigDecimal;
import java.util.List;

/** Contrato del mensaje JMS y respuesta REST del flujo de demostración. */
public class Pedido {
    private String id;
    private EstadoPedido estado;
    private List<SolicitudPedido.Item> items;
    private BigDecimal total;
    /** Email del cliente dueño del pedido (sale del JWT, nunca del cuerpo). */
    private String cliente;

    public Pedido() { }
    public Pedido(String id, String estado, List<SolicitudPedido.Item> items, BigDecimal total) {
        this(id, estado, items, total, null);
    }
    public Pedido(String id, String estado, List<SolicitudPedido.Item> items, BigDecimal total, String cliente) {
        this.cliente = cliente;
        this.id = id;
        this.estado = EstadosPedido.desde(estado);
        this.items = items;
        this.total = total;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEstado() { return estado == null ? null : estado.nombre(); }
    /** Hidratación JSON-B; los eventos de negocio usan transicionar(). */
    public void setEstado(String estado) { this.estado = EstadosPedido.desde(estado); }
    public Pedido transicionar(EventoPedido evento) {
        if (estado == null) throw new IllegalStateException("Pedido sin estado inicial.");
        return new Pedido(id, estado.aplicar(evento).nombre(), items, total, cliente);
    }
    public List<SolicitudPedido.Item> getItems() { return items; }
    public void setItems(List<SolicitudPedido.Item> items) { this.items = items; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
