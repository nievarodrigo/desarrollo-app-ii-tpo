package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import jakarta.persistence.*;
@Entity @Table(name="evento_outbox")
public class EventoOutboxEntity {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=50) public String tipo;
 @Column(nullable=false,length=20) public String destino;
 @Column(nullable=false,columnDefinition="text") public String payload;
 @Column(nullable=false) public java.time.Instant creado;
 public java.time.Instant publicado;
 @Column(nullable=false) public int intentos;
}
