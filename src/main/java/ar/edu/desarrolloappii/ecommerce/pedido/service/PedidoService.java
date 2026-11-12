package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoEnMemoria;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Queue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Valida, recalcula y publica el comando; no espera al consumidor. */
@ApplicationScoped
@JMSDestinationDefinition(
        name = "java:/jms/queue/ProcesarPedidoQueue",
        interfaceName = "jakarta.jms.Queue",
        destinationName = "ProcesarPedidoQueue",
        resourceAdapter = "activemq-ra")
public class PedidoService {
    private static final BigDecimal ENVIO_GRATIS_DESDE = new BigDecimal("150000");
    private static final BigDecimal COSTO_ENVIO = new BigDecimal("8900");

    @Inject CatalogoService catalogo;
    @Inject PedidoEnMemoria repositorio;
    @Inject JMSContext jms;
    @Resource(lookup = "java:/jms/queue/ProcesarPedidoQueue") Queue cola;

    public Pedido crear(SolicitudPedido solicitud) {
        BigDecimal total = validarYCalcular(solicitud, catalogo);
        List<SolicitudPedido.Item> copia = solicitud.getItems().stream()
                .map(i -> new SolicitudPedido.Item(i.getIdProducto(), i.getCantidad())).toList();
        Pedido pedido = new Pedido(UUID.randomUUID().toString(), "PENDIENTE", copia, total);
        repositorio.guardar(pedido);
        try (Jsonb jsonb = JsonbBuilder.create()) {
            jms.createProducer().setDeliveryMode(jakarta.jms.DeliveryMode.PERSISTENT)
                    .send(cola, jsonb.toJson(pedido));
        } catch (RuntimeException error) {
            repositorio.aplicar(pedido.getId(), EventoPedido.ENVIO_FALLIDO);
            throw error;
        } catch (Exception error) {
            repositorio.aplicar(pedido.getId(), EventoPedido.ENVIO_FALLIDO);
            throw new IllegalStateException("No se pudo serializar el pedido.", error);
        }
        return pedido;
    }

    /** Regla pura, testeable sin broker: nunca se acepta precio enviado por el cliente. */
    static BigDecimal validarYCalcular(SolicitudPedido solicitud, CatalogoService catalogo) {
        if (solicitud == null || solicitud.getItems() == null || solicitud.getItems().isEmpty()
                || solicitud.getItems().size() > 50) {
            throw new IllegalArgumentException("El pedido debe tener entre 1 y 50 productos.");
        }
        Set<Long> ids = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SolicitudPedido.Item item : solicitud.getItems()) {
            if (item == null || item.getIdProducto() <= 0 || item.getCantidad() <= 0
                    || !ids.add(item.getIdProducto())) {
                throw new IllegalArgumentException("Hay un producto o cantidad inválida o repetida.");
            }
            Producto producto = catalogo.obtener(item.getIdProducto());
            if (!producto.hayStock() || item.getCantidad() > producto.getStock()) {
                throw new IllegalArgumentException("No hay stock suficiente para " + producto.getNombre() + ".");
            }
            subtotal = subtotal.add(producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
        }
        return subtotal.compareTo(ENVIO_GRATIS_DESDE) >= 0
                ? subtotal : subtotal.add(COSTO_ENVIO);
    }

    public Pedido obtener(String id) { return repositorio.obtener(id); }
}
