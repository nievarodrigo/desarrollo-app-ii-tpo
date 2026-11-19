package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import jakarta.persistence.*;
@Entity @Table(name="consumo_evento")
public class ConsumoEventoEntity {
 @Id @Column(length=200) public String id;
 @Column(nullable=false,length=30) public String suscriptor;
 @Column(nullable=false,columnDefinition="text") public String payload;
 @Column(precision=19,scale=2) public java.math.BigDecimal total;
 @Column(nullable=false) public java.time.Instant creado;
}
