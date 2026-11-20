package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Topic;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Adaptador JMS del tópico de eventos. Mensaje PERSISTENTE: los suscriptores son durables, así
 * que un suscriptor caído recibe el evento cuando vuelve.
 */
@ApplicationScoped
@jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.NOT_SUPPORTED)
@JMSDestinationDefinition(
        name = "java:/jms/topic/PedidoConfirmadoTopic",
        interfaceName = "jakarta.jms.Topic",
        destinationName = "PedidoConfirmadoTopic",
        resourceAdapter = "activemq-ra")
public class PublicadorDeEventosJms implements PublicadorDeEventos {
    @Inject JMSContext jms;
    @Resource(lookup = "java:/jms/topic/PedidoConfirmadoTopic") Topic topico;

    @Override
    public void publicar(PedidoConfirmado evento) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jms.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .setProperty("tipoEvento", evento.tipoEvento())
                    .setProperty(PublicadorPedidos.PROPIEDAD_ID_MENSAJE, evento.idEvento())
                    .send(topico, jsonb.toJson(evento));
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo serializar el evento.", error);
        }
    }
}
