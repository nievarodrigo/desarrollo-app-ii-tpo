package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.*;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoRepository;
import jakarta.ejb.*;
import jakarta.persistence.*;
import java.util.List;
@Stateless
public class PedidoJpa implements PedidoRepository {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 public PedidoJpa() {}
 public PedidoJpa(EntityManager em) { this.em=em; }
 public void guardar(Pedido p) { em.persist(PedidoMapper.entidad(p)); }
 public Pedido obtener(String id) { return PedidoMapper.dominio(em.find(PedidoEntity.class,id,LockModeType.PESSIMISTIC_WRITE)); }
 public Pedido aplicar(String id,EventoPedido evento) {
  var e=em.find(PedidoEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
  if(e==null) return null;
  var nuevo=PedidoMapper.dominio(e).transicionar(evento); e.estado=nuevo.getEstado(); return nuevo;
 }
 public List<Pedido> listarPorEstado(String estado) {
  return em.createQuery("select p from PedidoEntity p where p.estado=:estado",PedidoEntity.class).setParameter("estado",estado).getResultStream().map(PedidoMapper::dominio).toList();
 }
}
