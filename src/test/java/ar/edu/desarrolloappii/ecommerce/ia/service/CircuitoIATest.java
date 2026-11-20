package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.pedido.service.IdempotenciaPedidosTest.RelojMovil;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitoIATest {
    private final RelojMovil reloj = new RelojMovil(Instant.parse("2026-11-20T10:00:00Z"));
    private final CircuitoIA circuito = new CircuitoIA(reloj);

    @Test void conMenosDeDiezLlamadasNoAbreAunqueTodasFallen() {
        for (int i = 0; i < CircuitoIA.VENTANA - 1; i++) circuito.registrarFalla();
        assertEquals(CircuitoIA.Estado.CERRADO, circuito.estado());
    }

    @Test void abreConLaMitadDeFallasYDejaDeLlamar30Segundos() {
        for (int i = 0; i < 5; i++) circuito.registrarExito();
        for (int i = 0; i < 5; i++) circuito.registrarFalla();
        assertEquals(CircuitoIA.Estado.ABIERTO, circuito.estado());
        assertFalse(circuito.permiteLlamar());
        reloj.ahora = reloj.ahora.plusSeconds(29);
        assertFalse(circuito.permiteLlamar());
    }

    @Test void semiabiertoDejaPasarUnaSolaPruebaYSeCierraSiSaleBien() {
        for (int i = 0; i < 10; i++) circuito.registrarFalla();
        reloj.ahora = reloj.ahora.plusSeconds(30);
        assertEquals(CircuitoIA.Estado.SEMIABIERTO, circuito.estado());
        assertTrue(circuito.permiteLlamar());
        assertFalse(circuito.permiteLlamar(), "sólo una llamada de prueba");
        circuito.registrarExito();
        assertEquals(CircuitoIA.Estado.CERRADO, circuito.estado());
        assertTrue(circuito.permiteLlamar());
    }

    @Test void siLaPruebaFallaVuelveAAbrir() {
        for (int i = 0; i < 10; i++) circuito.registrarFalla();
        reloj.ahora = reloj.ahora.plusSeconds(30);
        assertTrue(circuito.permiteLlamar());
        circuito.registrarFalla();
        assertEquals(CircuitoIA.Estado.ABIERTO, circuito.estado());
        assertFalse(circuito.permiteLlamar());
    }
}
