package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.DemasiadosIntentosException;
import jakarta.ejb.EJBException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProblemasTest {
    private static final String RUTA = "/ecommerce/api/productos/99";

    @Test void un404EnvueltoPorElContenedorEjbSigueSiendo404() {
        var resultado = Problemas.desde(new EJBException(new RecursoNoEncontradoException("No existe 99")), RUTA);
        assertEquals(404, resultado.problema().status());
        assertEquals("No existe 99", resultado.problema().detail());
        assertEquals(RUTA, resultado.problema().instance());
    }

    @Test void conservaStatusYCabecerasDeLosErroresDeNegocio() {
        var resultado = Problemas.desde(new EJBException(new DemasiadosIntentosException(30)), RUTA);
        assertEquals(429, resultado.problema().status());
        assertEquals("30", resultado.cabeceras().get("Retry-After"));
    }

    @Test void unErrorInesperadoEs500SinFiltrarDetallesInternos() {
        var resultado = Problemas.desde(new NullPointerException("campo secreto nulo"), RUTA);
        assertEquals(500, resultado.problema().status());
        assertFalse(resultado.problema().detail().contains("secreto"));
        assertEquals("about:blank", resultado.problema().type());
    }

    @Test void losNuevosErroresDeResilienciaTienenSuStatus() {
        assertEquals(412, Problemas.desde(new PrecondicionFallidaException("x"), RUTA).problema().status());
        assertEquals(422, Problemas.desde(new ContenidoNoProcesableException("x"), RUTA).problema().status());
        assertEquals(409, Problemas.desde(new OperacionEnCursoException("x"), RUTA).problema().status());
        assertEquals(400, Problemas.desde(new DatosInvalidosException("x"), RUTA).problema().status());
    }

    @Test void datosInvalidosPorClaveDeIdempotenciaInvalidaEs400ProblemDetails() {
        var excepcion = new DatosInvalidosException("La Idempotency-Key debe tener entre 8 y 100 caracteres alfanuméricos.");
        var resultado = Problemas.desde(excepcion, RUTA);

        assertEquals(400, resultado.problema().status());
        assertEquals("Datos inválidos", resultado.problema().title());
        assertEquals("La Idempotency-Key debe tener entre 8 y 100 caracteres alfanuméricos.", resultado.problema().detail());
        assertEquals(RUTA, resultado.problema().instance());
        assertEquals("about:blank", resultado.problema().type());
    }
}
