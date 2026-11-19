package ar.edu.desarrolloappii.ecommerce.pedido.service;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.persistence.*;
@Singleton @DependsOn("Migraciones")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RelayOutbox {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 @Inject EntregaOutbox entrega;
 @Schedule(second="*/5",minute="*",hour="*",persistent=false)
 public void programado() { publicarPendientes(); }
 public long contarPendientes() { return em.createQuery("select count(e) from EventoOutboxEntity e where e.publicado is null and e.destino='TOPICO'",Long.class).getSingleResult(); }
 public int publicarPendientes() {
  var ids=em.createQuery("select e.id from EventoOutboxEntity e where e.publicado is null order by e.creado",String.class).setMaxResults(100).getResultList();
  int publicados=0;
  for(String id:ids) if(entrega.entregar(id)) publicados++;
  return publicados;
 }
}
