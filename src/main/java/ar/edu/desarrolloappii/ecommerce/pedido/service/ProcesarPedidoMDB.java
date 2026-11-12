package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.TransicionInvalidaException;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoRepository;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.logging.Logger;

/** Un único consumidor lógico de la cola P2P; JMS puede reentregar mensajes. */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/ProcesarPedidoQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class ProcesarPedidoMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(ProcesarPedidoMDB.class.getName());

    @Inject PedidoRepository repositorio;

    public ProcesarPedidoMDB() { }

    /** Constructor para tests, sin contenedor. */
    public ProcesarPedidoMDB(PedidoRepository repositorio) { this.repositorio = repositorio; }

    @Override
    public void onMessage(Message mensaje) {
        try {
            procesar(mensaje.getBody(String.class));
        } catch (JMSException error) {
            throw new IllegalStateException("No se pudo leer el mensaje de pedido.", error);
        }
    }

    /** Lógica del consumidor, separada del tipo Message de JMS para poder probarla. */
    void procesar(String json) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Pedido pedido = jsonb.fromJson(json, Pedido.class);
            if (pedido == null || pedido.getId() == null || repositorio.obtener(pedido.getId()) == null) {
                throw new IllegalArgumentException("Pedido desconocido o mensaje inválido.");
            }
            // State + computeIfPresent atómico: la reentrega no regresa un pedido confirmado.
            repositorio.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
            LOG.info("Pedido " + pedido.getId() + " procesado por la cola.");
        } catch (TransicionInvalidaException error) {
            // Ej.: el cliente canceló antes de que llegue el mensaje. Reintentar no lo arregla:
            // se descarta (con aviso en el log) en vez de reentregar para siempre.
            LOG.warning("Mensaje descartado: " + error.getMessage());
        } catch (Exception error) {
            // Al fallar el MDB, el contenedor puede reentregar; no se acusa recibo silenciosamente.
            throw new IllegalStateException("Falló el procesamiento del mensaje de pedido.", error);
        }
    }
}
