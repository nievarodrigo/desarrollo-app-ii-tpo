package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import jakarta.persistence.*;
@Entity @Table(name="mensaje_procesado")
public class MensajeProcesadoEntity {
 @Id @Column(length=200) public String id;
 @Column(nullable=false) public java.time.Instant creado;
}
