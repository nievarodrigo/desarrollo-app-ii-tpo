package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoRepository;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Implementa Repository con JPA y Data Mapper para persistir pedidos sin exponer entidades. */
@Stateless
public class PedidoJpa implements PedidoRepository {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    /** Permite que el contenedor cree el Repository para inyectarle el contexto JPA. */
    public PedidoJpa() {
    }

    /** Recibe el contexto JPA para probar el Repository sin contenedor. */
    public PedidoJpa(EntityManager em) {
        this.em = em;
    }

    /** Registra el pedido mediante Data Mapper para incorporarlo al Unit of Work actual. */
    public void guardar(Pedido pedido) {
        em.persist(PedidoMapper.entidad(pedido));
    }

    /** Busca y bloquea el pedido para coordinar los cambios concurrentes dentro del Unit of Work. */
    public Pedido obtener(String id) {
        return PedidoMapper.dominio(em.find(PedidoEntity.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    /** Aplica la transición del dominio y actualiza la entidad para persistir sólo un estado válido. */
    public Pedido aplicar(String id, EventoPedido evento) {
        var entidad = em.find(PedidoEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entidad == null) {
            return null;
        }
        var pedidoActualizado = PedidoMapper.dominio(entidad).transicionar(evento);
        entidad.estado = pedidoActualizado.getEstado();
        return pedidoActualizado;
    }

    /** Lista pedidos por estado mediante Data Mapper para devolver objetos de dominio desde el Repository. */
    public List<Pedido> listarPorEstado(String estado) {
        return em.createQuery("select p from PedidoEntity p where p.estado=:estado", PedidoEntity.class)
                .setParameter("estado", estado)
                .getResultStream()
                .map(PedidoMapper::dominio)
                .toList();
    }
}
