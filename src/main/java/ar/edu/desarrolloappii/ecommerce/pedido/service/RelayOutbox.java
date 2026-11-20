package ar.edu.desarrolloappii.ecommerce.pedido.service;

import jakarta.ejb.DependsOn;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Recorre Transactional Outbox para reintentar mensajes que todavía no se publicaron. */
@Singleton
@DependsOn("Migraciones")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RelayOutbox {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    @Inject
    EntregaOutbox entrega;

    /** Activa la entrega periódica para recuperar publicaciones pendientes sin intervención manual. */
    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void programado() {
        publicarPendientes();
    }

    /** Cuenta eventos pendientes del tópico para mostrar cuánto falta publicar del outbox. */
    public long contarPendientes() {
        return em.createQuery("select count(e) from EventoOutboxEntity e where e.publicado is null and e.destino='TOPICO'", Long.class)
                .getSingleResult();
    }

    /** Intenta entregar hasta cien filas por antigüedad para avanzar el outbox con una transacción por fila. */
    public int publicarPendientes() {
        var ids = em.createQuery("select e.id from EventoOutboxEntity e where e.publicado is null order by e.creado", String.class)
                .setMaxResults(100)
                .getResultList();
        int publicados = 0;
        for (String id : ids) {
            if (entrega.entregar(id)) {
                publicados++;
            }
        }
        return publicados;
    }
}
