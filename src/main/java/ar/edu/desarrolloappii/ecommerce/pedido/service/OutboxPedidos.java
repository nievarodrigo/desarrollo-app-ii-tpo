package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.EventoOutboxEntity;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.UUID;

/** Registra mensajes con Transactional Outbox para confirmarlos junto con los cambios del pedido. */
@Stateless
public class OutboxPedidos {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    /** Permite que el contenedor cree el outbox para participar del Unit of Work actual. */
    public OutboxPedidos() {
    }

    /** Recibe el contexto JPA para probar el outbox dentro de una transacción controlada. */
    public OutboxPedidos(EntityManager em) {
        this.em = em;
    }

    @Inject
    Event<FilaOutboxRegistrada> registrada;

    /** Avisa por CDI qué fila se registró para intentar la entrega después de confirmar (AFTER_SUCCESS). */
    public record FilaOutboxRegistrada(String id) {
    }

    /** Registra el comando en el outbox para enviarlo a la cola después de confirmar el pedido. */
    public void agregar(Pedido pedido) {
        guardar(UUID.randomUUID().toString(), "ProcesarPedido", "COLA", pedido);
    }

    /** Registra la confirmación con su identificador para permitir reentregas deduplicables al tópico. */
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
        if (registrada != null) {
            registrada.fire(new FilaOutboxRegistrada(id));
        }
    }
}
