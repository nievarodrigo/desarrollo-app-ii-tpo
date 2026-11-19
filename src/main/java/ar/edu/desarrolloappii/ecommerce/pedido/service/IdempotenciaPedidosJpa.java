package ar.edu.desarrolloappii.ecommerce.pedido.service;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.*;
import ar.edu.desarrolloappii.ecommerce.shared.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.annotation.Resource;
import jakarta.transaction.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
/** La tabla es la fuente de verdad. La guarda local sólo responde 409 sin esperar un INSERT concurrente en esta instancia. */
@ApplicationScoped @Transactional(rollbackOn=Throwable.class)
public class IdempotenciaPedidosJpa extends IdempotenciaPedidos {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 @Resource TransactionSynchronizationRegistry transacciones;
 private record EnCurso(String huella,Instant creada) {}
 private final Map<String,EnCurso> enCurso=new ConcurrentHashMap<>();
 private Clock reloj=Clock.systemUTC();
 public IdempotenciaPedidosJpa() {}
 public IdempotenciaPedidosJpa(EntityManager em,Clock reloj) { this.em=em; this.reloj=reloj; }
 @Override public Resultado ejecutar(String clave,String usuario,String huella,Supplier<Pedido> accion) {
  validar(clave); String normalizado=usuario.toLowerCase(Locale.ROOT); String llave=normalizado+"|"+clave;
  Instant ahora=reloj.instant(); var guardia=new EnCurso(huella,ahora);
  enCurso.entrySet().removeIf(e->e.getValue().creada().isBefore(ahora.minus(EN_CURSO_MAXIMO)));
  var previa=enCurso.putIfAbsent(llave,guardia);
  if(previa!=null) { comprobarHuella(previa.huella(),huella); throw ocupado(); }
  boolean registrado=false;
  try {
   if(transacciones!=null) {
    transacciones.registerInterposedSynchronization(new Synchronization() {
     public void beforeCompletion() {}
     public void afterCompletion(int estado) { enCurso.remove(llave,guardia); }
    }); registrado=true;
   }
   var encontradas=em.createQuery("select c from ClaveIdempotenciaEntity c where c.usuario=:u and c.clave=:c",ClaveIdempotenciaEntity.class)
    .setParameter("u",normalizado).setParameter("c",clave).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
   if(!encontradas.isEmpty()) {
    var existente=encontradas.get(0);
    if(existente.creada.isBefore(ahora.minus(existente.pedidoId==null?EN_CURSO_MAXIMO:VIGENCIA))) { em.remove(existente); em.flush(); }
    else {
     comprobarHuella(existente.huella,huella);
     if(existente.pedidoId==null) throw ocupado();
     return new Resultado(PedidoMapper.dominio(em.find(PedidoEntity.class,existente.pedidoId)),true);
    }
   }
   var e=new ClaveIdempotenciaEntity(); e.id=UUID.randomUUID().toString(); e.usuario=normalizado; e.clave=clave; e.huella=huella; e.creada=ahora;
   try { em.persist(e); em.flush(); } catch(PersistenceException carrera) {
    for(Throwable causa=carrera; causa!=null; causa=causa.getCause()) {
     if(causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) throw ocupado();
    }
    throw carrera;
   }
   Pedido pedido=accion.get(); em.flush(); e.pedidoId=pedido.getId(); em.flush(); return new Resultado(pedido,false);
  } finally { if(!registrado) enCurso.remove(llave,guardia); }
 }
 private static void comprobarHuella(String anterior,String nueva) { if(!anterior.equals(nueva)) throw new ContenidoNoProcesableException("Esa Idempotency-Key ya se usó con otro pedido."); }
 private static OperacionEnCursoException ocupado() { return new OperacionEnCursoException("Ya estamos procesando un pedido con esa Idempotency-Key."); }
}
