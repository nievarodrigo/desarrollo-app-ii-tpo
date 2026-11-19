package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.UUID;

/**
 * Adaptador JMS: serializa el pedido a JSON y lo deja en la cola.
 *
 * El mensaje es PERSISTENTE: Artemis lo escribe en su journal en disco antes de
 * confirmar el envío, así que si el broker (o el servidor entero) se apaga, el
 * mensaje sigue ahí al volver. Cada envío lleva un {@code idMensaje} único para que
 * el consumidor descarte duplicados.
 */
@ApplicationScoped
@JMSDestinationDefinition(
        name = "java:/jms/queue/ProcesarPedidoQueue",
        interfaceName = "jakarta.jms.Queue",
        destinationName = "ProcesarPedidoQueue",
        resourceAdapter = "activemq-ra")
public class PublicadorPedidosJms implements PublicadorPedidos {
    @Inject JMSContext jms;
    @Resource(lookup = "java:/jms/queue/ProcesarPedidoQueue") Queue cola;

    @Override
    public void publicar(Pedido pedido) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jms.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .setProperty(PROPIEDAD_ID_MENSAJE, UUID.randomUUID().toString())
                    .send(cola, jsonb.toJson(pedido));
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo serializar el pedido.", error);
        }
    }
}
