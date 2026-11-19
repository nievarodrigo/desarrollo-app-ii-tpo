package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.util.logging.Logger;

/**
 * Suscriptor DURABLE de {@code PedidoConfirmadoTopic}. Actualiza las estadísticas de ventas (pedidos confirmados y facturación).
 * Durable: si este suscriptor está caído, el broker le guarda los eventos y los recibe al volver.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/topic/PedidoConfirmadoTopic"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "gomarket-estadisticas"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "gomarket-estadisticas")
})
public class EstadisticasMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(EstadisticasMDB.class.getName());

    @Inject ConsumosDeEventos consumos;

    @Override
    public void onMessage(Message mensaje) {
        try {
            PedidoConfirmado evento = ConsumosDeEventos.leer(mensaje.getBody(String.class));
            if (consumos.consumir(ConsumosDeEventos.ESTADISTICAS, evento)) {
                LOG.info("[estadisticas] PedidoConfirmado " + evento.idPedido() + " (evento " + evento.idEvento() + ")");
            } else {
                LOG.info("[estadisticas] evento duplicado " + evento.idEvento() + " descartado (idempotente)");
            }
        } catch (JMSException error) {
            throw new IllegalStateException("No se pudo leer el evento.", error); // rollback → reentrega → DLQ
        }
    }
}
