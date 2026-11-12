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

/** Adaptador JMS: serializa el pedido a JSON y lo deja en la cola (mensaje persistente). */
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
            jms.createProducer().setDeliveryMode(DeliveryMode.PERSISTENT)
                    .send(cola, jsonb.toJson(pedido));
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo serializar el pedido.", error);
        }
    }
}
