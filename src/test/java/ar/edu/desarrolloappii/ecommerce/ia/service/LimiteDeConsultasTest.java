package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.pedido.service.IdempotenciaPedidosTest;
import ar.edu.desarrolloappii.ecommerce.shared.exception.DemasiadasSolicitudesException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LimiteDeConsultasTest {
    @Test void cortaAl31DelMinutoPorClienteYSeReiniciaEnLaVentanaSiguiente() {
        var reloj = new IdempotenciaPedidosTest.RelojMovil(Instant.parse("2026-11-20T10:00:00Z"));
        LimiteDeConsultas limite = new LimiteDeConsultas(reloj);
        for (int i = 0; i < LimiteDeConsultas.POR_MINUTO; i++) limite.registrar("1.2.3.4");
        var error = assertThrows(DemasiadasSolicitudesException.class, () -> limite.registrar("1.2.3.4"));
        assertEquals(429, error.status());
        assertNotNull(error.cabeceras().get("Retry-After"));
        assertDoesNotThrow(() -> limite.registrar("5.6.7.8"), "otro cliente no se ve afectado");
        reloj.ahora = reloj.ahora.plusSeconds(61);
        assertDoesNotThrow(() -> limite.registrar("1.2.3.4"));
    }
}
