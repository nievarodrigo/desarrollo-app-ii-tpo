package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.ClaveIdempotenciaEntity;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.PedidoEntity;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.PedidoMapper;
import ar.edu.desarrolloappii.ecommerce.shared.exception.ContenidoNoProcesableException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.OperacionEnCursoException;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Implementa Idempotent Receiver con la tabla como fuente de verdad para no repetir pedidos.
 * La guarda local permite responder 409 sin esperar un INSERT concurrente en esta instancia.
 */
@ApplicationScoped
@Transactional(rollbackOn = Throwable.class)
public class IdempotenciaPedidosJpa extends IdempotenciaPedidos {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    @Resource
    TransactionSynchronizationRegistry transacciones;

    /** Conserva la huella y el inicio de la operación para detectar reintentos en esta instancia. */
    private record EnCurso(String huella, Instant creada) {
    }

    private final Map<String, EnCurso> enCurso = new ConcurrentHashMap<>();
    private Clock reloj = Clock.systemUTC();

    /** Permite que el contenedor cree el receptor para inyectarle JPA y las transacciones. */
    public IdempotenciaPedidosJpa() {
    }

    /** Recibe el contexto JPA y el reloj para probar la idempotencia y sus vencimientos. */
    public IdempotenciaPedidosJpa(EntityManager em, Clock reloj) {
        this.em = em;
        this.reloj = reloj;
    }

    /** Ejecuta o recupera el pedido por clave para evitar duplicados dentro del mismo Unit of Work. */
    @Override
    public Resultado ejecutar(String clave, String usuario, String huella, Supplier<Pedido> accion) {
        validar(clave);
        String usuarioNormalizado = usuario.toLowerCase(Locale.ROOT);
        String llave = usuarioNormalizado + "|" + clave;
        Instant ahora = reloj.instant();
        var guardia = new EnCurso(huella, ahora);
        enCurso.entrySet().removeIf(entrada -> entrada.getValue().creada().isBefore(ahora.minus(EN_CURSO_MAXIMO)));
        var previa = enCurso.putIfAbsent(llave, guardia);
        if (previa != null) {
            comprobarHuella(previa.huella(), huella);
            throw ocupado();
        }
        boolean registrado = false;
        try {
            if (transacciones != null) {
                transacciones.registerInterposedSynchronization(new Synchronization() {
                    /** Conserva la guarda hasta que termine la transacción para impedir ejecuciones simultáneas. */
                    public void beforeCompletion() {
                    }

                    /** Libera la guarda al terminar la transacción para permitir los siguientes reintentos. */
                    public void afterCompletion(int estado) {
                        enCurso.remove(llave, guardia);
                    }
                });
                registrado = true;
            }
            var encontradas = em.createQuery("select c from ClaveIdempotenciaEntity c where c.usuario=:u and c.clave=:c", ClaveIdempotenciaEntity.class)
                    .setParameter("u", usuarioNormalizado)
                    .setParameter("c", clave)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
            if (!encontradas.isEmpty()) {
                var existente = encontradas.get(0);
                if (existente.creada.isBefore(ahora.minus(existente.pedidoId == null ? EN_CURSO_MAXIMO : VIGENCIA))) {
                    em.remove(existente);
                    em.flush();
                } else {
                    comprobarHuella(existente.huella, huella);
                    if (existente.pedidoId == null) {
                        throw ocupado();
                    }
                    return new Resultado(PedidoMapper.dominio(em.find(PedidoEntity.class, existente.pedidoId)), true);
                }
            }
            var clavePersistida = new ClaveIdempotenciaEntity();
            clavePersistida.id = UUID.randomUUID().toString();
            clavePersistida.usuario = usuarioNormalizado;
            clavePersistida.clave = clave;
            clavePersistida.huella = huella;
            clavePersistida.creada = ahora;
            try {
                em.persist(clavePersistida);
                em.flush();
            } catch (PersistenceException carrera) {
                for (Throwable causa = carrera; causa != null; causa = causa.getCause()) {
                    if (causa instanceof java.sql.SQLException errorSql && "23505".equals(errorSql.getSQLState())) {
                        throw ocupado();
                    }
                }
                throw carrera;
            }
            Pedido pedido = accion.get();
            em.flush();
            clavePersistida.pedidoId = pedido.getId();
            em.flush();
            return new Resultado(pedido, false);
        } finally {
            if (!registrado) {
                enCurso.remove(llave, guardia);
            }
        }
    }

    private static void comprobarHuella(String anterior, String nueva) {
        if (!anterior.equals(nueva)) {
            throw new ContenidoNoProcesableException("Esa Idempotency-Key ya se usó con otro pedido.");
        }
    }

    private static OperacionEnCursoException ocupado() {
        return new OperacionEnCursoException("Ya estamos procesando un pedido con esa Idempotency-Key.");
    }
}
