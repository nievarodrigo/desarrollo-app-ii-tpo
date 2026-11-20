package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.MensajeProcesadoEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;

/** Persiste el inbox de Idempotent Receiver para que una reentrega no repita el procesamiento. */
@Stateless
public class RegistroDeMensajesJpa extends RegistroDeMensajes {
    @PersistenceContext(unitName = "gomarket")
    EntityManager em;

    /** Permite que el contenedor cree el inbox para compartir el Unit of Work del procesamiento. */
    public RegistroDeMensajesJpa() {
    }

    /** Recibe el contexto JPA para probar la deduplicación persistente sin contenedor. */
    public RegistroDeMensajesJpa(EntityManager em) {
        this.em = em;
    }

    /** Consulta el identificador persistido para descartar mensajes que ya tuvieron efecto. */
    @Override
    public boolean yaProcesado(String id) {
        return id != null && em.find(MensajeProcesadoEntity.class, id) != null;
    }

    /** Registra un identificador no nulo y sincroniza JPA para detectar duplicados en el Unit of Work. */
    @Override
    public void registrar(String id) {
        if (id == null) {
            return;
        }
        var mensajeProcesado = new MensajeProcesadoEntity();
        mensajeProcesado.id = id;
        mensajeProcesado.creado = Instant.now();
        em.persist(mensajeProcesado);
        em.flush();
    }
}
