import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import java.util.Properties;
import java.util.UUID;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Demo de la DLQ: manda a ProcesarPedidoQueue un mensaje que el consumidor no puede procesar.
 * El MDB falla, Artemis reintenta 3 veces y el mensaje termina en ProcesarPedidoDLQ.
 *
 *   $WF/bin/add-user.sh -a -u demo-jms -p "$CLAVE" -g guest      (una sola vez, clave a elección)
 *   java -Dusuario=demo-jms -Dclave="$CLAVE" -cp $WF/bin/client/jboss-client.jar scripts/EnviarMensajeInvalido.java
 *
 * (probar-resiliencia.sh hace todo esto solo, con una clave aleatoria en .secretos/.)
 *
 * Después: GET /api/v1/admin/mensajeria/dlq con un token de ADMIN.
 */
public class EnviarMensajeInvalido {
    public static void main(String[] args) throws Exception {
        String usuario = System.getProperty("usuario", "demo-jms");
        String clave = System.getProperty("clave");
        if (clave == null || clave.isBlank()) {
            System.err.println("Falta -Dclave=<clave del usuario JMS>. No hay clave por defecto: ningún secreto en el repo.");
            System.exit(2);
        }
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        env.put(Context.PROVIDER_URL, System.getProperty("url", "http-remoting://localhost:8080"));
        env.put(Context.SECURITY_PRINCIPAL, usuario);
        env.put(Context.SECURITY_CREDENTIALS, clave);
        ConnectionFactory fabrica = (ConnectionFactory) new InitialContext(env).lookup("jms/RemoteConnectionFactory");
        try (JMSContext jms = fabrica.createContext(usuario, clave)) {
            String id = "veneno-" + UUID.randomUUID();
            jms.createProducer().setProperty("idMensaje", id)
                    .send(jms.createQueue("ProcesarPedidoQueue"), args.length > 0 ? args[0] : "esto no es un pedido");
            System.out.println("Enviado mensaje inválido " + id);
        }
    }
}
