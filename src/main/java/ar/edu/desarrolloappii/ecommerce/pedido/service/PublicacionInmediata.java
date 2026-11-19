package ar.edu.desarrolloappii.ecommerce.pedido.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Transactional Outbox, camino rápido: apenas confirma la transacción que guardó el pedido (o el
 * evento) y su fila del outbox, se intenta publicar esa fila. Si el broker está caído, la fila queda
 * pendiente y la publica {@link RelayOutbox} en su próxima vuelta (red de seguridad, cada 5 s).
 * Nunca se publica algo que no quedó guardado: si la transacción hace rollback, este observer no corre.
 */
@ApplicationScoped
public class PublicacionInmediata {
    @Inject EntregaOutbox entrega;

    void alConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) OutboxPedidos.FilaOutboxRegistrada fila) {
        try {
            entrega.entregar(fila.id()); // transacción propia (REQUIRES_NEW)
        } catch (RuntimeException error) {
            // El relay la reintenta: el request del cliente no se ve afectado.
        }
    }
}
