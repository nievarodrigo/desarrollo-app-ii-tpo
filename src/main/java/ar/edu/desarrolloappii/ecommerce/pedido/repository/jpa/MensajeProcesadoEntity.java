package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Guarda los identificadores ya procesados para deduplicar con Idempotent Receiver. */
@Entity
@Table(name = "mensaje_procesado")
public class MensajeProcesadoEntity {
    @Id
    @Column(length = 200)
    public String id;

    @Column(nullable = false)
    public java.time.Instant creado;
}
