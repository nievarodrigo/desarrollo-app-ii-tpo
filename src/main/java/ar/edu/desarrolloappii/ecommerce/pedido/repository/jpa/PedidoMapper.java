package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.*;
public final class PedidoMapper {
 private PedidoMapper() {}
 public static PedidoEntity entidad(Pedido p) {
  PedidoEntity e=new PedidoEntity(); e.id=p.getId(); e.estado=p.getEstado(); e.cliente=p.getCliente(); e.total=p.getTotal();
  for(var i:p.getItems()) { var item=new PedidoEntity.Item(); item.producto=i.getIdProducto(); item.cantidad=i.getCantidad(); e.items.add(item); }
  return e;
 }
 public static Pedido dominio(PedidoEntity e) {
  return e==null?null:new Pedido(e.id,e.estado,e.items.stream().map(i->new SolicitudPedido.Item(i.producto,i.cantidad)).toList(),e.total,e.cliente);
 }
}
