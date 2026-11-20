package ar.edu.desarrolloappii.ecommerce.pedido.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuzonMensajesMuertosTest {
    @Test void guardaLosMasRecientesPrimeroYRecortaElCuerpo() {
        BuzonMensajesMuertos buzon = new BuzonMensajesMuertos();
        buzon.registrar("a", "q", "x".repeat(1000));
        buzon.registrar("b", "q", "{}");
        assertEquals("b", buzon.listar().get(0).idMensaje());
        assertTrue(buzon.listar().get(1).cuerpo().length() <= 501);
        for (int i = 0; i < BuzonMensajesMuertos.CAPACIDAD + 10; i++) buzon.registrar("m" + i, "q", "{}");
        assertEquals(BuzonMensajesMuertos.CAPACIDAD, buzon.listar().size());
    }
}
