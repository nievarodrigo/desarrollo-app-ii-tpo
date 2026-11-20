package ar.edu.desarrolloappii.ecommerce.pedido.domain;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PedidoStateTest {
    private Pedido pedido(String estado) { return new Pedido("p1", estado, List.of(), BigDecimal.TEN); }
    @Test void procesaYReentregaSinRegresarEstado() {
        Pedido p = pedido("PENDIENTE").transicionar(EventoPedido.MENSAJE_PROCESADO);
        assertEquals("PROCESADO", p.getEstado());
        p = p.transicionar(EventoPedido.MENSAJE_PROCESADO).transicionar(EventoPedido.CONFIRMAR);
        assertEquals("CONFIRMADO", p.transicionar(EventoPedido.MENSAJE_PROCESADO).getEstado());
    }
    @Test void errorDeEnvioPermiteReintentoExplicito() {
        Pedido p = pedido("PENDIENTE").transicionar(EventoPedido.ENVIO_FALLIDO);
        assertEquals("ERROR_ENVIO", p.getEstado());
        assertThrows(TransicionInvalidaException.class, () -> p.transicionar(EventoPedido.CONFIRMAR));
        assertEquals("PENDIENTE", p.transicionar(EventoPedido.REINTENTAR_ENVIO).getEstado());
    }
    @Test void reservaRechazadaNoSePuedeProcesar() {
        Pedido p = pedido("PENDIENTE_VALIDACION").transicionar(EventoPedido.RESERVA_RECHAZADA);
        assertEquals("RECHAZADO", p.getEstado());
        assertThrows(TransicionInvalidaException.class, () -> p.transicionar(EventoPedido.MENSAJE_PROCESADO));
    }
    @Test void reservaAceptadaConservaIdentidadTotalYEstadoAnterior() {
        Pedido original = pedido("PENDIENTE_VALIDACION");
        Pedido p = original.transicionar(EventoPedido.RESERVA_ACEPTADA);
        assertEquals("PENDIENTE", p.getEstado());
        assertEquals(original.getId(), p.getId());
        assertEquals(original.getTotal(), p.getTotal());
        assertEquals("PENDIENTE_VALIDACION", original.getEstado());
    }
    @Test void rechazaEstadoDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> pedido("PAGADO_INVENTADO"));
    }
    @Test void mensajeTardioResuelveEnvioAmbiguo() {
        assertEquals("PROCESADO", pedido("ERROR_ENVIO")
                .transicionar(EventoPedido.MENSAJE_PROCESADO).getEstado());
    }

    @Test void cancelarVale_hasta_antes_de_confirmar_y_despues_no() {
        assertEquals("CANCELADO", pedido("PENDIENTE").transicionar(EventoPedido.CANCELAR).getEstado());
        assertEquals("CANCELADO", pedido("PROCESADO").transicionar(EventoPedido.CANCELAR).getEstado());
        assertEquals("CANCELADO", pedido("ERROR_ENVIO").transicionar(EventoPedido.CANCELAR).getEstado());
        assertThrows(TransicionInvalidaException.class,
                () -> pedido("CONFIRMADO").transicionar(EventoPedido.CANCELAR));
    }
    @Test void canceladoEsFinal() {
        Pedido p = pedido("CANCELADO");
        for (EventoPedido e : EventoPedido.values()) {
            assertThrows(TransicionInvalidaException.class, () -> p.transicionar(e), e.name());
        }
    }
    @Test void confirmarSoloDesdeProcesado() {
        assertThrows(TransicionInvalidaException.class, () -> pedido("PENDIENTE").transicionar(EventoPedido.CONFIRMAR));
        assertEquals("CONFIRMADO", pedido("PROCESADO").transicionar(EventoPedido.CONFIRMAR).getEstado());
    }
    @Test void laTransicionInvalidaEsUn409ConMensajeClaro() {
        TransicionInvalidaException e = assertThrows(TransicionInvalidaException.class,
                () -> pedido("CONFIRMADO").transicionar(EventoPedido.CANCELAR));
        assertEquals(409, e.status());
        assertTrue(e.getMessage().contains("CANCELAR") && e.getMessage().contains("CONFIRMADO"));
    }
    @Test void transicionarConservaAlDuenio() {
        Pedido p = new Pedido("p1", "PENDIENTE", List.of(), BigDecimal.TEN, "ana@gomarket.com.ar");
        assertEquals("ana@gomarket.com.ar", p.transicionar(EventoPedido.MENSAJE_PROCESADO).getCliente());
    }
}
