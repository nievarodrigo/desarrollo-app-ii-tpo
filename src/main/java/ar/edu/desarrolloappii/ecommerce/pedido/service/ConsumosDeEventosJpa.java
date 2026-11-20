package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.ConsumoEventoEntity;
import jakarta.ejb.Stateless;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** Persiste el efecto y el inbox de cada suscriptor para deduplicar eventos con Idempotent Receiver. */
@Stateless
public class ConsumosDeEventosJpa extends ConsumosDeEventos {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    /** Permite que el contenedor cree el receptor para persistir cada consumo en su Unit of Work. */
    public ConsumosDeEventosJpa() {
    }

    /** Recibe el contexto JPA para probar los efectos y la deduplicación sin contenedor. */
    public ConsumosDeEventosJpa(EntityManager em) {
        this.em = em;
    }

    /** Registra el efecto una vez por suscriptor y evento para que las reentregas no lo dupliquen. */
    @Override
    public boolean consumir(String suscriptor, PedidoConfirmado evento) {
        if (!Set.of(NOTIFICACIONES, AUDITORIA, ESTADISTICAS).contains(suscriptor)) {
            throw new IllegalArgumentException("Suscriptor desconocido: " + suscriptor);
        }
        String id = suscriptor + ":" + evento.idEvento();
        if (em.find(ConsumoEventoEntity.class, id) != null) {
            return false;
        }
        try (Jsonb json = JsonbBuilder.create()) {
            var consumo = new ConsumoEventoEntity();
            consumo.id = id;
            consumo.suscriptor = suscriptor;
            consumo.payload = json.toJson(evento);
            consumo.total = evento.total();
            consumo.creado = Instant.now();
            em.persist(consumo);
            em.flush();
            return true;
        } catch (RuntimeException fallo) {
            throw fallo;
        } catch (Exception fallo) {
            throw new IllegalStateException(fallo);
        }
    }

    /** Reúne los efectos persistidos para mostrar notificaciones, estadísticas y auditoría de la demo. */
    @Override
    public Map<String, Object> resumen() {
        var auditoria = em.createQuery("select c.payload from ConsumoEventoEntity c where c.suscriptor=:s order by c.creado desc", String.class)
                .setParameter("s", AUDITORIA)
                .setMaxResults(500)
                .getResultStream()
                .map(ConsumosDeEventos::leer)
                .toList();
        BigDecimal total = em.createQuery("select sum(c.total) from ConsumoEventoEntity c where c.suscriptor=:s", BigDecimal.class)
                .setParameter("s", ESTADISTICAS)
                .getSingleResult();
        return Map.of("notificacionesEnviadas", contar(NOTIFICACIONES),
                "estadisticas", Map.of("pedidosConfirmados", contar(ESTADISTICAS),
                        "facturacion", total == null ? BigDecimal.ZERO : total),
                "auditoria", auditoria);
    }

    private long contar(String suscriptor) {
        return em.createQuery("select count(c) from ConsumoEventoEntity c where c.suscriptor=:s", Long.class)
                .setParameter("s", suscriptor)
                .getSingleResult();
    }
}
