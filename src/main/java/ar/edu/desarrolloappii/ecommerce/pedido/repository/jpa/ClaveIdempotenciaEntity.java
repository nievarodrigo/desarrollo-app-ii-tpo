package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Guarda la clave y su pedido para reconocer reintentos con Idempotent Receiver. */
@Entity
@Table(name = "clave_idempotencia", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario", "clave"}))
public class ClaveIdempotenciaEntity {
    @Id
    @Column(length = 36)
    public String id;

    @Column(nullable = false, length = 320)
    public String usuario;

    @Column(nullable = false, length = 100)
    public String clave;

    @Column(nullable = false, length = 200)
    public String huella;

    @Column(name = "pedido_id", length = 36)
    public String pedidoId;

    @Column(nullable = false)
    public java.time.Instant creada;
}
