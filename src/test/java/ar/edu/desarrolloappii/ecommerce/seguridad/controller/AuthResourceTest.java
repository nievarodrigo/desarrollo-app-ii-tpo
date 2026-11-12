package ar.edu.desarrolloappii.ecommerce.seguridad.controller;

import ar.edu.desarrolloappii.ecommerce.shared.exception.DatosInvalidosException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthResourceTest {
    @Test void login_sin_cuerpo_es_error_de_entrada_400() {
        assertEquals(400, assertThrows(DatosInvalidosException.class,
                () -> new AuthResource().login(null)).status());
    }

    @Test void registro_sin_cuerpo_es_error_de_entrada_400() {
        assertEquals(400, assertThrows(DatosInvalidosException.class,
                () -> new AuthResource().registro(null)).status());
    }
}
