package ar.edu.desarrolloappii.ecommerce.pedido.service;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.MensajeProcesadoEntity;
import jakarta.ejb.*;
import jakarta.persistence.*;
import java.time.Instant;
@Stateless
public class RegistroDeMensajesJpa extends RegistroDeMensajes {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 public RegistroDeMensajesJpa() {}
 public RegistroDeMensajesJpa(EntityManager em) { this.em=em; }
 @Override public boolean yaProcesado(String id) { return id!=null && em.find(MensajeProcesadoEntity.class,id)!=null; }
 @Override public void registrar(String id) {
  if(id==null) return;
  var e=new MensajeProcesadoEntity(); e.id=id; e.creado=Instant.now(); em.persist(e); em.flush();
 }
}
