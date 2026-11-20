package ar.edu.desarrolloappii.ecommerce.pedido.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistroDeMensajesTest {
    @Test void recuerdaLosProcesadosYOlvidaLosMasViejosAlLlenarse() {
        RegistroDeMensajes registro = new RegistroDeMensajes();
        registro.registrar("m-0");
        assertTrue(registro.yaProcesado("m-0"));
        assertFalse(registro.yaProcesado("m-1"));
        assertFalse(registro.yaProcesado(null), "sin id no se puede deduplicar: decide el State");
        for (int i = 1; i <= RegistroDeMensajes.CAPACIDAD; i++) registro.registrar("m-" + i);
        assertFalse(registro.yaProcesado("m-0"), "el más viejo sale para no crecer sin límite");
        assertTrue(registro.yaProcesado("m-" + RegistroDeMensajes.CAPACIDAD));
    }
}
