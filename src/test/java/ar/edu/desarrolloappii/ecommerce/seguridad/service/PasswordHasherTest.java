package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test void verifica_la_clave_correcta_y_rechaza_la_incorrecta() {
        String guardado = hasher.hashear("cliente123");
        assertTrue(hasher.verificar("cliente123", guardado));
        assertFalse(hasher.verificar("cliente124", guardado));
    }

    @Test void no_guarda_la_clave_en_claro_y_usa_sal_distinta_cada_vez() {
        String a = hasher.hashear("misma-clave");
        String b = hasher.hashear("misma-clave");
        assertFalse(a.contains("misma-clave"));
        assertNotEquals(a, b);
    }

    @Test void un_hash_roto_no_tira_excepcion() {
        assertFalse(hasher.verificar("x", "cualquier-cosa"));
        assertFalse(hasher.verificar("x", "pbkdf2$no-numero$!!$!!"));
    }
}
