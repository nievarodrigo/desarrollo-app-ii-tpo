package ar.edu.desarrolloappii.ecommerce.shared.http;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EtagsTest {
    public record Producto(long id, String nombre, int stock) { }
    public record PedidoDto(String id, String estado, String total) { }

    @Test void mismaRepresentacionMismoEtag() {
        assertEquals(Etags.de(new Producto(1, "Mouse", 3)), Etags.de(new Producto(1, "Mouse", 3)));
    }

    @Test void cualquierCambioDaOtroEtag() {
        assertNotEquals(Etags.de(new Producto(1, "Mouse", 3)), Etags.de(new Producto(1, "Mouse", 2)));
        assertNotEquals(Etags.de(Map.of("items", List.of(1))), Etags.de(Map.of("items", List.of(2))));
    }

    @Test void ifMatchAceptaComillasDebilAsteriscoYListas() {
        String etag = Etags.huella("v1");
        assertTrue(Etags.coincide("\"" + etag + "\"", etag));
        assertTrue(Etags.coincide("W/\"" + etag + "\"", etag));
        assertTrue(Etags.coincide("*", etag));
        assertTrue(Etags.coincide("\"otro\", \"" + etag + "\"", etag));
        assertTrue(Etags.coincide(null, etag), "sin If-Match no hay precondición");
        assertFalse(Etags.coincide("\"viejo\"", etag));
    }

    @Test void elEtagCambiaCuandoCambiaElEstadoDelPedido() {
        var pendiente = new PedidoDto("p-1", "PENDIENTE", "1500.00");
        var procesado = new PedidoDto("p-1", "PROCESADO", "1500.00");
        var confirmado = new PedidoDto("p-1", "CONFIRMADO", "1500.00");

        String etagPendiente = Etags.de(pendiente);
        String etagProcesado = Etags.de(procesado);
        String etagConfirmado = Etags.de(confirmado);

        assertNotEquals(etagPendiente, etagProcesado, "el ETag debe cambiar al pasar de PENDIENTE a PROCESADO");
        assertNotEquals(etagProcesado, etagConfirmado, "el ETag debe cambiar al pasar de PROCESADO a CONFIRMADO");
        assertNotEquals(etagPendiente, etagConfirmado, "el ETag debe ser distinto entre PENDIENTE y CONFIRMADO");

        // Verificamos también la huella directa id|estado|total que calcula el controller
        assertNotEquals(Etags.huella("p-1|PENDIENTE|1500.00"), Etags.huella("p-1|PROCESADO|1500.00"));
        assertNotEquals(Etags.huella("p-1|PROCESADO|1500.00"), Etags.huella("p-1|CONFIRMADO|1500.00"));
    }

    @Test void elEtagEsIdemParaMismoPedidoSinCambios() {
        var p1 = new PedidoDto("p-1", "PENDIENTE", "1500.00");
        var p2 = new PedidoDto("p-1", "PENDIENTE", "1500.00");

        assertEquals(Etags.de(p1), Etags.de(p2));
        assertEquals(Etags.huella("p-1|PENDIENTE|1500.00"), Etags.huella("p-1|PENDIENTE|1500.00"));
    }
}
