package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoEnMemoria;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pub/sub: PedidoConfirmado en el tópico, tres suscriptores idempotentes y reenvío si el broker cae. */
class EventosPedidoTest {
    static class TopicoEnMemoria implements PublicadorDeEventos {
        final List<PedidoConfirmado> publicados = new ArrayList<>();
        boolean caido;
        @Override public void publicar(PedidoConfirmado evento) {
            if (caido) throw new IllegalStateException("broker caído");
            publicados.add(evento);
        }
    }

    private final Identidad cliente = new Identidad("cliente@gomarket.com.ar", Rol.CLIENTE);
    private final Identidad admin = new Identidad("admin@gomarket.com.ar", Rol.ADMIN);
    private PedidoEnMemoria repositorio;
    private TopicoEnMemoria topico;
    private EventosPendientes pendientes;
    private PedidoService servicio;

    @BeforeEach void preparar() {
        repositorio = new PedidoEnMemoria();
        topico = new TopicoEnMemoria();
        pendientes = new EventosPendientes();
        servicio = new PedidoService(new CatalogoDePrueba(), repositorio, p -> { }, topico, pendientes);
    }

    private Pedido procesado() {
        SolicitudPedido s = new SolicitudPedido();
        s.setItems(List.of(new SolicitudPedido.Item(1, 1)));
        Pedido p = servicio.crear(s, cliente);
        repositorio.aplicar(p.getId(), EventoPedido.MENSAJE_PROCESADO);
        return p;
    }

    @Test void confirmarPublicaUnSoloEventoConElContratoDelDiseno() {
        Pedido p = procesado();
        servicio.aplicarEvento(p.getId(), EventoPedido.CONFIRMAR, admin);
        servicio.aplicarEvento(p.getId(), EventoPedido.CONFIRMAR, admin); // confirmar de nuevo: sin evento nuevo
        assertEquals(1, topico.publicados.size());
        PedidoConfirmado e = topico.publicados.get(0);
        assertEquals("PedidoConfirmado", e.tipoEvento());
        assertEquals(1, e.version());
        assertEquals(p.getId(), e.idPedido());
        assertEquals("ARS", e.moneda());
        assertNotNull(e.idEvento());
    }

    @Test void cancelarNoPublicaPedidoConfirmado() {
        Pedido p = procesado();
        servicio.aplicarEvento(p.getId(), EventoPedido.CANCELAR, cliente);
        assertTrue(topico.publicados.isEmpty());
    }

    @Test void conElBrokerCaidoLaConfirmacionNoFallaYElEventoSeReenviaDespues() {
        Pedido p = procesado();
        topico.caido = true;
        assertEquals("CONFIRMADO", servicio.aplicarEvento(p.getId(), EventoPedido.CONFIRMAR, admin).getEstado());
        assertEquals(1, pendientes.listar().size());
        ReenvioDePedidos reenvio = new ReenvioDePedidos(repositorio, x -> { }, topico, pendientes);
        assertEquals(0, reenvio.reintentarEventos(), "sigue caído");
        topico.caido = false;
        assertEquals(1, reenvio.reintentarEventos());
        assertTrue(pendientes.listar().isEmpty());
        assertEquals(1, topico.publicados.size());
    }

    @Test void cadaSuscriptorRecibeSuCopiaYDescartaDuplicados() throws Exception {
        ConsumosDeEventos consumos = new ConsumosDeEventos();
        PedidoConfirmado e = new PedidoConfirmado("ev-1", "PedidoConfirmado", 1, "2026-11-20T10:00:00-03:00",
                "p-1", "p-1", new BigDecimal("150000"), "ARS");
        String json;
        try (Jsonb jsonb = JsonbBuilder.create()) { json = jsonb.toJson(e); }
        for (String s : List.of(ConsumosDeEventos.NOTIFICACIONES, ConsumosDeEventos.AUDITORIA, ConsumosDeEventos.ESTADISTICAS)) {
            assertTrue(consumos.consumir(s, ConsumosDeEventos.leer(json)), "primera entrega para " + s);
            assertFalse(consumos.consumir(s, ConsumosDeEventos.leer(json)), "reentrega descartada para " + s);
        }
        var r = consumos.resumen();
        assertEquals(1L, r.get("notificacionesEnviadas"));
        assertEquals(1, ((List<?>) r.get("auditoria")).size());
        assertEquals(Map(1L, new BigDecimal("150000")), r.get("estadisticas"));
    }

    @Test void unEventoInvalidoFallaParaQueTermineEnLaDLQ() {
        assertThrows(IllegalArgumentException.class, () -> ConsumosDeEventos.leer("{\"tipoEvento\":\"PedidoConfirmado\"}"));
    }

    private static java.util.Map<String, Object> Map(long confirmados, BigDecimal facturacion) {
        return java.util.Map.of("pedidosConfirmados", confirmados, "facturacion", facturacion);
    }
}
