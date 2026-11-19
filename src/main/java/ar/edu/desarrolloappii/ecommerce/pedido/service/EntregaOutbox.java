package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.EventoOutboxEntity;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Entrega Transactional Outbox con una transacción por fila para aislar cada intento.
 * JMS envía fuera de JTA: una caída entre envío y marca produce reentrega con el mismo id.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class EntregaOutbox {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    @Inject
    PublicadorPedidos publicador;

    @Inject
    PublicadorDeEventos eventos;

    /** Permite que el contenedor cree la entrega para asignarle una transacción por fila. */
    public EntregaOutbox() {
    }

    /** Recibe JPA y los publicadores para probar la entrega y los reintentos sin contenedor. */
    public EntregaOutbox(EntityManager em, PublicadorPedidos publicador, PublicadorDeEventos eventos) {
        this.em = em;
        this.publicador = publicador;
        this.eventos = eventos;
    }

    /** Publica y marca la fila bajo bloqueo para coordinar intentos y dejar pendientes los envíos fallidos. */
    public boolean entregar(String id) {
        var fila = em.find(EventoOutboxEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (fila == null || fila.publicado != null) {
            return false;
        }
        fila.intentos++;
        try (Jsonb json = JsonbBuilder.create()) {
            if ("COLA".equals(fila.destino)) {
                publicador.publicar(json.fromJson(fila.payload, Pedido.class), fila.id);
            } else if ("TOPICO".equals(fila.destino)) {
                eventos.publicar(json.fromJson(fila.payload, PedidoConfirmado.class));
            } else {
                throw new IllegalStateException("Destino desconocido: " + fila.destino);
            }
            fila.publicado = Instant.now();
            return true;
        } catch (Exception fallo) {
            Logger.getLogger(EntregaOutbox.class.getName()).warning("Outbox " + id + " pendiente: " + fallo.getMessage());
            return false;
        }
    }
}
