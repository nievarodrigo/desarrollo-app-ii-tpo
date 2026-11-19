package ar.edu.desarrolloappii.ecommerce.pedido.service;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.*;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.EventoOutboxEntity;
import jakarta.ejb.*;
import jakarta.persistence.*;
import jakarta.json.bind.*;
import java.time.Instant;
import java.util.UUID;
@Stateless
public class OutboxPedidos {
 @PersistenceContext(unitName="gomarket") EntityManager em;
 public OutboxPedidos() {}
 public OutboxPedidos(EntityManager em) { this.em=em; }
 @Inject Event<FilaOutboxRegistrada> registrada;

 /** Aviso CDI: se procesa recién cuando la transacción que escribió la fila confirma (AFTER_SUCCESS). */
 public record FilaOutboxRegistrada(String id) { }

 public void agregar(Pedido pedido) {
  guardar(UUID.randomUUID().toString(), "ProcesarPedido", "COLA", pedido);
 }

 public void agregar(PedidoConfirmado evento) {
  guardar(evento.idEvento(), PedidoConfirmado.TIPO, "TOPICO", evento);
 }

 private void guardar(String id, String tipo, String destino, Object valor) {
  try (Jsonb json = JsonbBuilder.create()) {
   var fila = new EventoOutboxEntity();
   fila.id = id;
   fila.tipo = tipo;
   fila.destino = destino;
   fila.payload = json.toJson(valor);
   fila.creado = Instant.now();
   em.persist(fila);
  } catch (Exception error) {
   throw new IllegalStateException("No se pudo registrar el outbox", error);
  }
  // Publicar ya, sin esperar al relay: el observer corre después del commit (si hay rollback, no corre).
  if (registrada != null) registrada.fire(new FilaOutboxRegistrada(id));
 }
}
