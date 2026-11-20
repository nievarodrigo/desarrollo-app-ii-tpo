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

/**
 * Consumidor de la cola P2P. JMS entrega "al menos una vez", así que es IDEMPOTENTE
 * en dos niveles:
 * <ol>
 *   <li>Inbox JPA en la misma transacción CMT: un {@code idMensaje} ya procesado se ignora.</li>
 *   <li>Patrón State: aplicar MENSAJE_PROCESADO a un pedido ya PROCESADO/CONFIRMADO no cambia nada.</li>
 * </ol>
 * Si el procesamiento falla, lanza una excepción: el contenedor hace rollback y Artemis
 * reintenta. Tras 3 intentos el mensaje va a la DLQ (ver {@code scripts/mensajeria.cli}).
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/ProcesarPedidoQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class ProcesarPedidoMDB implements MessageListener {
    private static final Logger LOG = Logger.getLogger(ProcesarPedidoMDB.class.getName());

    @Inject PedidoRepository repositorio;
    @Inject RegistroDeMensajes registro;

    public ProcesarPedidoMDB() { }

    /** Constructor para tests, sin contenedor. */
    public ProcesarPedidoMDB(PedidoRepository repositorio) { this(repositorio, new RegistroDeMensajes()); }

    public ProcesarPedidoMDB(PedidoRepository repositorio, RegistroDeMensajes registro) {
        this.repositorio = repositorio;
        this.registro = registro;
    }

    @Override
    public void onMessage(Message mensaje) {
        try {
            procesar(mensaje.getBody(String.class),
                    mensaje.getStringProperty(PublicadorPedidos.PROPIEDAD_ID_MENSAJE));
        } catch (JMSException error) {
            throw new IllegalStateException("No se pudo leer el mensaje de pedido.", error);
        }
    }

    void procesar(String json) { procesar(json, null); }

    /** Lógica del consumidor, separada del tipo Message de JMS para poder probarla. */
    void procesar(String json, String idMensaje) {
        if (registro.yaProcesado(idMensaje)) {
            LOG.info("Mensaje duplicado " + idMensaje + " ignorado (consumidor idempotente).");
            return;
        }
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Pedido pedido = jsonb.fromJson(json, Pedido.class);
            if (pedido == null || pedido.getId() == null) {
                throw new IllegalArgumentException("Mensaje inválido: no trae un pedido.");
            }
            if (repositorio.obtener(pedido.getId()) == null) rehidratar(pedido);
            // State + bloqueo JPA: la reentrega no regresa un pedido confirmado.
            repositorio.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
            registro.registrar(idMensaje);
            LOG.info("Pedido " + pedido.getId() + " procesado por la cola.");
        } catch (TransicionInvalidaException error) {
            // Ej.: el cliente canceló antes de que llegue el mensaje. Reintentar no lo arregla:
            // se descarta (con aviso en el log) en vez de reentregar para siempre.
            registro.registrar(idMensaje);
            LOG.warning("Mensaje descartado: " + error.getMessage());
        } catch (Exception error) {
            // Al fallar el MDB, el contenedor hace rollback y Artemis reentrega (y luego DLQ).
            throw new IllegalStateException("Falló el procesamiento del mensaje de pedido.", error);
        }
    }

    /**
     * Compatibilidad con mensajes anteriores a JPA y tests en memoria: si no existe el
     * pedido, sólo se reconstruye desde un mensaje completo. Un mensaje incompleto falla
     * y termina en la DLQ. Los pedidos JPA normales sobreviven al reinicio en la base.
     */
    private void rehidratar(Pedido pedido) {
        boolean completo = pedido.getCliente() != null && pedido.getTotal() != null
                && pedido.getItems() != null && !pedido.getItems().isEmpty()
                && ("PENDIENTE".equals(pedido.getEstado()) || "ERROR_ENVIO".equals(pedido.getEstado()));
        if (!completo) throw new IllegalArgumentException("Pedido desconocido y mensaje incompleto.");
        repositorio.guardar(pedido);
        LOG.warning("Pedido " + pedido.getId() + " reconstruido desde el mensaje persistente (reinicio del servidor).");
    }
}
