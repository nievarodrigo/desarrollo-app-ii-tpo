package ar.edu.desarrolloappii.ecommerce.pedido.service;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.ConsumoEventoEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import jakarta.json.bind.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
/** El registro persistente representa el efecto de cada suscriptor de la demo y su inbox. */
@Stateless
public class ConsumosDeEventosJpa extends ConsumosDeEventos {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 public ConsumosDeEventosJpa() {}
 public ConsumosDeEventosJpa(EntityManager em) { this.em=em; }
 @Override public boolean consumir(String suscriptor,PedidoConfirmado evento) {
  if(!Set.of(NOTIFICACIONES,AUDITORIA,ESTADISTICAS).contains(suscriptor)) throw new IllegalArgumentException("Suscriptor desconocido: "+suscriptor);
  String id=suscriptor+":"+evento.idEvento();
  if(em.find(ConsumoEventoEntity.class,id)!=null) return false;
  try(Jsonb json=JsonbBuilder.create()) {
   var e=new ConsumoEventoEntity();e.id=id;e.suscriptor=suscriptor;e.payload=json.toJson(evento);e.total=evento.total();e.creado=Instant.now();em.persist(e);em.flush();return true;
  } catch(RuntimeException fallo) { throw fallo; }
  catch(Exception fallo) { throw new IllegalStateException(fallo); }
 }
 @Override public Map<String,Object> resumen() {
  var auditoria=em.createQuery("select c.payload from ConsumoEventoEntity c where c.suscriptor=:s order by c.creado desc",String.class).setParameter("s",AUDITORIA).setMaxResults(500).getResultStream().map(ConsumosDeEventos::leer).toList();
  BigDecimal total=em.createQuery("select sum(c.total) from ConsumoEventoEntity c where c.suscriptor=:s",BigDecimal.class).setParameter("s",ESTADISTICAS).getSingleResult();
  return Map.of("notificacionesEnviadas",contar(NOTIFICACIONES),"estadisticas",Map.of("pedidosConfirmados",contar(ESTADISTICAS),"facturacion",total==null?BigDecimal.ZERO:total),"auditoria",auditoria);
 }
 private long contar(String suscriptor) { return em.createQuery("select count(c) from ConsumoEventoEntity c where c.suscriptor=:s",Long.class).setParameter("s",suscriptor).getSingleResult(); }
}
