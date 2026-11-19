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
 * Suscriptor DURABLE de {@code PedidoConfirmadoTopic}. Registra quién, qué y cuándo para auditoría (últimos 500 eventos).
 * Durable: si este suscriptor está caído, el broker le guarda los eventos y los recibe al volver.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/topic/PedidoConfirmadoTopic"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "gomarket-auditoria"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "gomarket-auditoria")
})
public class AuditoriaMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(AuditoriaMDB.class.getName());

    @Inject ConsumosDeEventos consumos;

    @Override
    public void onMessage(Message mensaje) {
        try {
            PedidoConfirmado evento = ConsumosDeEventos.leer(mensaje.getBody(String.class));
            if (consumos.consumir(ConsumosDeEventos.AUDITORIA, evento)) {
                LOG.info("[auditoria] PedidoConfirmado " + evento.idPedido() + " (evento " + evento.idEvento() + ")");
            } else {
                LOG.info("[auditoria] evento duplicado " + evento.idEvento() + " descartado (idempotente)");
            }
        } catch (JMSException error) {
            throw new IllegalStateException("No se pudo leer el evento.", error); // rollback → reentrega → DLQ
        }
    }
}
