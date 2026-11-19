package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.shared.exception.ContenidoNoProcesableException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.DatosInvalidosException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.OperacionEnCursoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdempotenciaPedidosTest {
    private static final String CLAVE = "3f0c2a8e-clave-de-prueba";
    private final IdempotenciaPedidos idempotencia = new IdempotenciaPedidos();
    private final AtomicInteger creados = new AtomicInteger();

    private Pedido crearPedido() {
        return new Pedido("p-" + creados.incrementAndGet(), "PENDIENTE", List.of(), BigDecimal.TEN, "ana@x.com");
    }

    @Test void reintentoConMismaClaveDevuelveElMismoPedidoSinCrearOtro() {
        var primero = idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido);
        var reintento = idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido);
        assertFalse(primero.repetido());
        assertTrue(reintento.repetido());
        assertSame(primero.pedido(), reintento.pedido());
        assertEquals(1, creados.get(), "el segundo intento no debe crear ni publicar otro pedido");
    }

    @Test void mismaClaveConOtroCuerpoEs422() {
        idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido);
        assertThrows(ContenidoNoProcesableException.class,
                () -> idempotencia.ejecutar(CLAVE, "ana@x.com", "h2", this::crearPedido));
    }

    @Test void laClaveEsPorUsuario() {
        idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido);
        var deOtro = idempotencia.ejecutar(CLAVE, "beto@x.com", "h1", this::crearPedido);
        assertFalse(deOtro.repetido());
        assertEquals(2, creados.get());
    }

    @Test void siLaOperacionFallaLaClaveSeLiberaParaReintentar() {
        assertThrows(IllegalStateException.class, () -> idempotencia.ejecutar(CLAVE, "ana@x.com", "h1",
                () -> { throw new IllegalStateException("broker caído"); }));
        var reintento = idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido);
        assertFalse(reintento.repetido());
    }

    @Test void mientrasLaPrimeraSigueEnCursoLaSegundaRecibe409() {
        assertThrows(OperacionEnCursoException.class, () -> idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", () -> {
            idempotencia.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido); // llega mientras la 1.ª procesa
            return crearPedido();
        }));
    }

    @Test void claveAusenteOMalFormadaEs400() {
        assertThrows(DatosInvalidosException.class, () -> IdempotenciaPedidos.validar(null));
        assertThrows(DatosInvalidosException.class, () -> IdempotenciaPedidos.validar("corta"));
        assertThrows(DatosInvalidosException.class, () -> IdempotenciaPedidos.validar("con espacios y <html>"));
        assertDoesNotThrow(() -> IdempotenciaPedidos.validar("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test void lasClavesVencenALas24Horas() {
        var reloj = new RelojMovil(Instant.parse("2026-11-20T10:00:00Z"));
        var conReloj = new IdempotenciaPedidos(reloj);
        conReloj.ejecutar(CLAVE, "ana@x.com", "h1", this::crearPedido);
        reloj.ahora = reloj.ahora.plus(IdempotenciaPedidos.VIGENCIA).plusSeconds(1);
        assertFalse(conReloj.ejecutar(CLAVE, "ana@x.com", "h2", this::crearPedido).repetido(),
                "vencida la clave, se puede reutilizar con otro cuerpo");
    }

    static final class RelojMovil extends Clock {
        Instant ahora;
        RelojMovil(Instant inicio) { this.ahora = inicio; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }
}
