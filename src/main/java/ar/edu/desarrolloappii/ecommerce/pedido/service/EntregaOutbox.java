package ar.edu.desarrolloappii.ecommerce.pedido.service;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.*;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.EventoOutboxEntity;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.json.bind.*;
import java.time.Instant;
import java.util.logging.Logger;
/** Una transacción por fila. JMS envía fuera de JTA: una caída entre envío y marca produce reentrega con el mismo id. */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class EntregaOutbox {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 @Inject PublicadorPedidos publicador;
 @Inject PublicadorDeEventos eventos;
 public EntregaOutbox() {}
 public EntregaOutbox(EntityManager em,PublicadorPedidos publicador,PublicadorDeEventos eventos) { this.em=em; this.publicador=publicador; this.eventos=eventos; }
 public boolean entregar(String id) {
  var e=em.find(EventoOutboxEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
  if(e==null || e.publicado!=null) return false;
  e.intentos++;
  try(Jsonb json=JsonbBuilder.create()) {
   if("COLA".equals(e.destino)) publicador.publicar(json.fromJson(e.payload,Pedido.class),e.id);
   else if("TOPICO".equals(e.destino)) eventos.publicar(json.fromJson(e.payload,PedidoConfirmado.class));
   else throw new IllegalStateException("Destino desconocido: "+e.destino);
   e.publicado=Instant.now(); return true;
  } catch(Exception fallo) { Logger.getLogger(EntregaOutbox.class.getName()).warning("Outbox "+id+" pendiente: "+fallo.getMessage()); return false; }
 }
}
