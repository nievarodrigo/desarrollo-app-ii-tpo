package ar.edu.desarrolloappii.ecommerce.pedido.domain;

import java.util.List;

/** Entrada REST: sólo identificadores y cantidades; el cliente no fija precios. */
public class SolicitudPedido {
    private List<Item> items;

    public SolicitudPedido() { }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private long idProducto;
        private int cantidad;

        public Item() { }
        public Item(long idProducto, int cantidad) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
        }
        public long getIdProducto() { return idProducto; }
        public void setIdProducto(long idProducto) { this.idProducto = idProducto; }
        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    }
}
