package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.TransicionInvalidaException;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoRepository;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.AccesoDenegadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import ar.edu.desarrolloappii.ecommerce.shared.exception.RecursoNoEncontradoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Valida, recalcula y registra el comando en el outbox dentro de la transacción.
 * Los constructores de tests conservan el adaptador de publicación en memoria.
 */
@ApplicationScoped
@jakarta.transaction.Transactional
public class PedidoService {
    private static final Logger LOG = Logger.getLogger(PedidoService.class.getName());
    private static final BigDecimal ENVIO_GRATIS_DESDE = new BigDecimal("150000");
    private static final BigDecimal COSTO_ENVIO = new BigDecimal("8900");

    @Inject CatalogoService catalogo;
    @Inject PedidoRepository repositorio;
    @Inject PublicadorPedidos publicador;
    @Inject PublicadorDeEventos eventos;
    EventosPendientes pendientes;
    @Inject OutboxPedidos outbox;

    public PedidoService() { }

    /** Constructor para tests unitarios, sin contenedor (sin tópico de eventos). */
    public PedidoService(CatalogoService catalogo, PedidoRepository repositorio, PublicadorPedidos publicador) {
        this(catalogo, repositorio, publicador, PublicadorDeEventos.NINGUNO, new EventosPendientes());
    }

    public PedidoService(CatalogoService catalogo, PedidoRepository repositorio, PublicadorPedidos publicador,
                         PublicadorDeEventos eventos, EventosPendientes pendientes) {
        this.catalogo = catalogo;
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.eventos = eventos;
        this.pendientes = pendientes;
    }

    /**
     * En el contenedor guarda pedido y outbox en la misma transacción JTA.
     * El relay publica después. Por constructor conserva el flujo anterior de los tests.
     */
    public Pedido crear(SolicitudPedido solicitud, Identidad solicitante) {
        BigDecimal total = validarYCalcular(solicitud, catalogo);
        List<SolicitudPedido.Item> copia = solicitud.getItems().stream()
                .map(i -> new SolicitudPedido.Item(i.getIdProducto(), i.getCantidad())).toList();
        Pedido pedido = new Pedido(UUID.randomUUID().toString(), "PENDIENTE", copia, total,
                solicitante.email());
        repositorio.guardar(pedido);
        if (outbox != null) { outbox.agregar(pedido); return pedido; }
        try {
            publicador.publicar(pedido);
        } catch (RuntimeException error) {
            LOG.log(Level.WARNING, "No se pudo publicar el pedido " + pedido.getId()
                    + "; queda en ERROR_ENVIO para reintentar.", error);
            try {
                repositorio.aplicar(pedido.getId(), EventoPedido.ENVIO_FALLIDO);
            } catch (TransicionInvalidaException transicion) {
                // JMS pudo entregar antes de informar un error ambiguo: el pedido ya avanzó
                // (p. ej. PROCESADO). No se lo regresa a ERROR_ENVIO.
                LOG.fine("Envío ambiguo: el consumidor ya había procesado " + pedido.getId());
            }
            return repositorio.obtener(pedido.getId());
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

    /** Un cliente sólo ve sus pedidos; un ADMIN ve todos. */
    public Pedido obtener(String id, Identidad solicitante) {
        return visiblePara(id, solicitante);
    }

    /** Aplica un evento de negocio. Si el estado actual no lo acepta, State lanza TransicionInvalidaException (409). */
    public Pedido aplicarEvento(String id, EventoPedido evento, Identidad solicitante) {
        Pedido anterior = visiblePara(id, solicitante);
        Pedido actualizado = repositorio.aplicar(id, evento);
        if (actualizado == null) throw noExiste(id);
        // Sólo la transición REAL a CONFIRMADO publica el evento: confirmar dos veces no lo repite.
        if (!"CONFIRMADO".equals(anterior.getEstado()) && "CONFIRMADO".equals(actualizado.getEstado())) {
            anunciarConfirmacion(actualizado);
        }
        return actualizado;
    }

    /** Registra PedidoConfirmado en el outbox; los tests por constructor usan el publicador. */
    void anunciarConfirmacion(Pedido pedido) {
        PedidoConfirmado evento = new PedidoConfirmado(UUID.randomUUID().toString(), PedidoConfirmado.TIPO, 1,
                java.time.OffsetDateTime.now().toString(), pedido.getId(), pedido.getId(), pedido.getTotal(), "ARS");
        if (outbox != null) { outbox.agregar(evento); return; }
        try {
            eventos.publicar(evento);
        } catch (RuntimeException error) {
            LOG.log(Level.WARNING, "No se pudo publicar PedidoConfirmado de " + pedido.getId() + "; queda pendiente.", error);
            pendientes.agregar(evento);
        }
    }

    private Pedido visiblePara(String id, Identidad solicitante) {
        Pedido pedido = repositorio.obtener(id);
        if (pedido == null) throw noExiste(id);
        boolean esDuenio = solicitante.email().equalsIgnoreCase(pedido.getCliente());
        if (solicitante.rol() != Rol.ADMIN && !esDuenio) {
            throw new AccesoDenegadoException("Ese pedido pertenece a otro cliente.");
        }
        return pedido;
    }

    private static RecursoNoEncontradoException noExiste(String id) {
        return new RecursoNoEncontradoException("No existe un pedido con id " + id);
    }
}
