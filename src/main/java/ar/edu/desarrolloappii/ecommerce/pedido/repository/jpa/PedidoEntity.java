package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
@Entity @Table(name="pedido")
public class PedidoEntity {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=40) public String estado;
 @Column(length=320) public String cliente;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal total;
 @ElementCollection @CollectionTable(name="pedido_item",joinColumns=@JoinColumn(name="pedido_id"))
 @OrderColumn(name="posicion") public List<Item> items = new ArrayList<>();
 @Embeddable public static class Item {
  @Column(nullable=false) public long producto;
  @Column(nullable=false) public int cantidad;
 }
}
