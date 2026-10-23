package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CuerpoInvalidoMapperTest {

    @Test void jsonMalformadoEsErrorDelCliente() {
        ProblemDetail problema = CuerpoInvalidoMapper.problema(
                new ProcessingException("RESTEASY008200", new JsonbException("Unexpected char")),
                "/ecommerce/api/pedidos");
        assertEquals(400, problema.status());
        assertEquals("/ecommerce/api/pedidos", problema.instance());
    }

    @Test void otraFallaDeProcesamientoSigueSiendoDelServidor() {
        ProblemDetail problema = CuerpoInvalidoMapper.problema(
                new ProcessingException("falla interna"), "/ecommerce/api/pedidos");
        assertEquals(500, problema.status());
    }
}
