package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.AccesoDenegadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.NoAutenticadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutorizadorDeAccesoTest {
    /** Un "controller" de mentira con los tres tipos de endpoint. */
    @RolesAllowed({"CLIENTE", "ADMIN"})
    static class RecursoFalso {
        public void porClaseAutenticado() { }
        @RolesAllowed("ADMIN") public void soloAdmin() { }
        @PermitAll public void publico() { }
        @DenyAll public void cerrado() { }
    }

    static class RecursoSinAnotaciones {
        public void abierto() { }
    }

    private final JwtService jwt = new JwtService("clave-de-pruebas-con-mas-de-32-bytes!!".getBytes(StandardCharsets.UTF_8),
            Duration.ofMinutes(30), Clock.systemUTC());
    private final AutorizadorDeAcceso autorizador = new AutorizadorDeAcceso(jwt);

    private static Method metodo(Class<?> tipo, String nombre) throws NoSuchMethodException {
        return tipo.getMethod(nombre);
    }

    private String bearer(Rol rol) { return "Bearer " + jwt.emitir(new Identidad("x@gomarket.com.ar", rol)); }

    @Test void sin_token_en_un_endpoint_protegido_es_401() throws Exception {
        NoAutenticadoException e = assertThrows(NoAutenticadoException.class,
                () -> autorizador.autorizar(metodo(RecursoFalso.class, "soloAdmin"), null));
        assertEquals(401, e.status());
        assertEquals("Bearer", e.cabeceras().get("WWW-Authenticate"));
    }

    @Test void token_falso_es_401() throws Exception {
        assertThrows(NoAutenticadoException.class,
                () -> autorizador.autorizar(metodo(RecursoFalso.class, "soloAdmin"), "Bearer abc.def.ghi"));
        assertThrows(NoAutenticadoException.class,
                () -> autorizador.autorizar(metodo(RecursoFalso.class, "soloAdmin"), "Basic dXNlcjpwYXNz"));
    }

    @Test void un_cliente_en_un_endpoint_de_admin_es_403() throws Exception {
        AccesoDenegadoException e = assertThrows(AccesoDenegadoException.class,
                () -> autorizador.autorizar(metodo(RecursoFalso.class, "soloAdmin"), bearer(Rol.CLIENTE)));
        assertEquals(403, e.status());
    }

    @Test void un_admin_pasa_y_se_conoce_su_identidad() throws Exception {
        Identidad i = autorizador.autorizar(metodo(RecursoFalso.class, "soloAdmin"), bearer(Rol.ADMIN)).orElseThrow();
        assertEquals(Rol.ADMIN, i.rol());
    }

    @Test void la_anotacion_de_clase_aplica_a_los_metodos_sin_anotacion() throws Exception {
        assertThrows(NoAutenticadoException.class,
                () -> autorizador.autorizar(metodo(RecursoFalso.class, "porClaseAutenticado"), null));
        assertTrue(autorizador.autorizar(metodo(RecursoFalso.class, "porClaseAutenticado"), bearer(Rol.CLIENTE)).isPresent());
    }

    @Test void permitAll_y_los_recursos_sin_anotaciones_son_publicos() throws Exception {
        assertTrue(autorizador.autorizar(metodo(RecursoFalso.class, "publico"), null).isEmpty());
        assertTrue(autorizador.autorizar(metodo(RecursoSinAnotaciones.class, "abierto"), null).isEmpty());
    }

    @Test void denyAll_siempre_es_403() throws Exception {
        assertThrows(AccesoDenegadoException.class,
                () -> autorizador.autorizar(metodo(RecursoFalso.class, "cerrado"), bearer(Rol.ADMIN)));
    }
}
