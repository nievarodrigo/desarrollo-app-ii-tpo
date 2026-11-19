package ar.edu.desarrolloappii.ecommerce.shared.http;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EtagsTest {
    public record Producto(long id, String nombre, int stock) { }

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
}
