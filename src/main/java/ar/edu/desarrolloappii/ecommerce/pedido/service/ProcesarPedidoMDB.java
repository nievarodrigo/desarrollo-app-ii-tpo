package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoEnMemoria;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/** Un único consumidor lógico de la cola P2P; JMS puede reentregar mensajes. */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/ProcesarPedidoQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class ProcesarPedidoMDB implements MessageListener {
    @Inject PedidoEnMemoria repositorio;

    @Override
    public void onMessage(Message mensaje) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Pedido pedido = jsonb.fromJson(mensaje.getBody(String.class), Pedido.class);
            if (pedido == null || pedido.getId() == null || repositorio.obtener(pedido.getId()) == null) {
                throw new IllegalArgumentException("Pedido desconocido o mensaje inválido.");
            }
            // State + compute atómico: la reentrega no regresa un pedido confirmado.
            repositorio.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
        } catch (Exception error) {
            // Al fallar el MDB, el contenedor puede reentregar; no se acusa recibo silenciosamente.
            throw new IllegalStateException("Falló el procesamiento del mensaje de pedido.", error);
        }
    }
}
