package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lo que hace cada suscriptor del tópico con un {@link PedidoConfirmado}. Cada suscriptor recibe su
 * propia copia (pub/sub) y es IDEMPOTENTE por {@code idEvento}: una reentrega no duplica el efecto.
 * Separado de los MDB para poder probarlo sin servidor.
 */
@ApplicationScoped
public class ConsumosDeEventos {
    public static final String NOTIFICACIONES = "notificaciones";
    public static final String AUDITORIA = "auditoria";
    public static final String ESTADISTICAS = "estadisticas";

    private final Map<String, Set<String>> vistosPorSuscriptor = new TreeMap<>();
    private final Deque<PedidoConfirmado> auditoria = new ArrayDeque<>();
    private long notificacionesEnviadas;
    private long pedidosConfirmados;
    private BigDecimal facturacion = BigDecimal.ZERO;

    public static PedidoConfirmado leer(String json) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            PedidoConfirmado e = jsonb.fromJson(json, PedidoConfirmado.class);
            if (e == null || e.idEvento() == null || e.idPedido() == null) {
                throw new IllegalArgumentException("Evento inválido: faltan idEvento o idPedido.");
            }
            return e;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    /** @return false si ese suscriptor ya había procesado el evento (duplicado descartado). */
    public synchronized boolean consumir(String suscriptor, PedidoConfirmado evento) {
        if (!vistosPorSuscriptor.computeIfAbsent(suscriptor, k -> new HashSet<>()).add(evento.idEvento())) return false;
        switch (suscriptor) {
            case NOTIFICACIONES -> notificacionesEnviadas++;
            case AUDITORIA -> {
                auditoria.addFirst(evento);
                while (auditoria.size() > 500) auditoria.removeLast();
            }
            case ESTADISTICAS -> {
                pedidosConfirmados++;
                if (evento.total() != null) facturacion = facturacion.add(evento.total());
            }
            default -> throw new IllegalArgumentException("Suscriptor desconocido: " + suscriptor);
        }
        return true;
    }

    public synchronized Map<String, Object> resumen() {
        Map<String, Object> r = new TreeMap<>();
        r.put("notificacionesEnviadas", notificacionesEnviadas);
        r.put("estadisticas", Map.of("pedidosConfirmados", pedidosConfirmados, "facturacion", facturacion));
        r.put("auditoria", List.copyOf(auditoria));
        return Collections.unmodifiableMap(r);
    }
}
