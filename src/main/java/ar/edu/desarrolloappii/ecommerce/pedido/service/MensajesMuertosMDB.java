package ar.edu.desarrolloappii.ecommerce.pedido.service;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.util.logging.Logger;

/**
 * Consumidor de la Dead Letter Queue de pedidos.
 *
 * Un mensaje llega acá cuando falló {@code max-delivery-attempts} veces (3, según
 * {@code scripts/mensajeria.cli}). No se reintenta más: se registra para que un ADMIN
 * lo vea en {@code GET /api/admin/mensajeria/dlq} y se deja asentado en el log.
 * Nunca lanza excepciones: un mensaje muerto no debe volver a circular.
 */
@JMSDestinationDefinition(
        name = "java:/jms/queue/ProcesarPedidoDLQ",
        interfaceName = "jakarta.jms.Queue",
        destinationName = "ProcesarPedidoDLQ",
        resourceAdapter = "activemq-ra")
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/ProcesarPedidoDLQ"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class MensajesMuertosMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(MensajesMuertosMDB.class.getName());

    @Inject BuzonMensajesMuertos buzon;

    @Override
    public void onMessage(Message mensaje) {
        try {
            String id = mensaje.getStringProperty(PublicadorPedidos.PROPIEDAD_ID_MENSAJE);
            // Artemis deja en _AMQ_ORIG_QUEUE la cola de la que vino el mensaje.
            String origen = mensaje.propertyExists("_AMQ_ORIG_QUEUE") ? mensaje.getStringProperty("_AMQ_ORIG_QUEUE") : null;
            buzon.registrar(id, origen, mensaje.getBody(String.class));
            LOG.severe("Mensaje " + id + " de " + origen + " llegó a la DLQ tras agotar los reintentos. Requiere revisión manual.");
        } catch (Exception error) {
            LOG.severe("No se pudo leer un mensaje de la DLQ: " + error.getMessage());
        }
    }
}
