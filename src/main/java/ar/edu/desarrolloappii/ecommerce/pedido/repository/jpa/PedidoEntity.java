package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Representa el pedido persistido para separar el dominio de JPA mediante Data Mapper. */
@Entity
@Table(name = "pedido")
public class PedidoEntity {
    @Id
    @Column(length = 36)
    public String id;

    @Column(nullable = false, length = 40)
    public String estado;

    @Column(length = 320)
    public String cliente;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal total;

    @ElementCollection
    @CollectionTable(name = "pedido_item", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicion")
    public List<Item> items = new ArrayList<>();

    /** Representa un ítem persistido para mapear los productos sin acoplar el dominio a JPA. */
    @Embeddable
    public static class Item {
        @Column(nullable = false)
        public long producto;

        @Column(nullable = false)
        public int cantidad;
    }
}
