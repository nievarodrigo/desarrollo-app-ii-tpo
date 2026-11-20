package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.ConflictoException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.DatosInvalidosException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.DemasiadosIntentosException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.NoAutenticadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Usuario;
import ar.edu.desarrolloappii.ecommerce.seguridad.repository.UsuarioEnMemoria;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    /** Reloj que se puede adelantar a mano, para probar el bloqueo de 15 minutos. */
    static class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-13T12:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private static final PasswordHasher HASHER = new PasswordHasher();
    private final RelojManual reloj = new RelojManual();
    private AuthService auth;
    private JwtService jwt;

    @BeforeEach void preparar() {
        jwt = new JwtService("clave-de-pruebas-con-mas-de-32-bytes!!".getBytes(StandardCharsets.UTF_8),
                Duration.ofMinutes(30), reloj);
        auth = new AuthService(new UsuarioEnMemoria(HASHER), HASHER, jwt, reloj);
    }

    @Test void ingreso_correcto_devuelve_un_jwt_con_el_rol_del_usuario() {
        AuthService.Sesion s = auth.ingresar("admin@gomarket.com.ar", "admin123");
        Identidad i = jwt.verificar(s.token());
        assertEquals(Rol.ADMIN, i.rol());
        assertEquals("admin@gomarket.com.ar", i.email());
    }

    @Test void email_inexistente_y_clave_mala_dan_el_mismo_mensaje() {
        String mensajeClaveMala = assertThrows(NoAutenticadoException.class,
                () -> auth.ingresar("cliente@gomarket.com.ar", "mala")).getMessage();
        String mensajeNoExiste = assertThrows(NoAutenticadoException.class,
                () -> auth.ingresar("nadie@gomarket.com.ar", "mala")).getMessage();
        assertEquals(mensajeClaveMala, mensajeNoExiste);
    }

    @Test void cinco_fallos_bloquean_aun_con_la_clave_correcta_y_se_libera_a_los_15_minutos() {
        for (int i = 0; i < 5; i++) {
            assertThrows(NoAutenticadoException.class, () -> auth.ingresar("cliente@gomarket.com.ar", "mala"));
        }
        DemasiadosIntentosException e = assertThrows(DemasiadosIntentosException.class,
                () -> auth.ingresar("cliente@gomarket.com.ar", "cliente123"));
        assertEquals(429, e.status());
        assertTrue(e.reintentarEnSegundos() > 0);
        assertEquals(Long.toString(e.reintentarEnSegundos()), e.cabeceras().get("Retry-After"));

        reloj.avanzar(Duration.ofMinutes(16));
        assertDoesNotThrow(() -> auth.ingresar("cliente@gomarket.com.ar", "cliente123"));
    }

    @Test void un_ingreso_exitoso_reinicia_el_contador() {
        for (int i = 0; i < 4; i++) {
            assertThrows(NoAutenticadoException.class, () -> auth.ingresar("cliente@gomarket.com.ar", "mala"));
        }
        auth.ingresar("cliente@gomarket.com.ar", "cliente123");
        assertThrows(NoAutenticadoException.class, () -> auth.ingresar("cliente@gomarket.com.ar", "mala"));
        assertDoesNotThrow(() -> auth.ingresar("cliente@gomarket.com.ar", "cliente123"));
    }

    @Test void el_registro_siempre_crea_un_cliente_con_la_clave_hasheada() {
        Usuario u = auth.registrar("Luz Paz", "Luz@Correo.com", "clave-larga-1");
        assertEquals(Rol.CLIENTE, u.rol());
        assertEquals("luz@correo.com", u.email());
        assertFalse(u.claveHash().contains("clave-larga-1"));
        assertEquals(Rol.CLIENTE, jwt.verificar(auth.ingresar("luz@correo.com", "clave-larga-1").token()).rol());
    }

    @Test void el_registro_valida_los_datos_y_rechaza_duplicados() {
        assertEquals(400, assertThrows(DatosInvalidosException.class,
                () -> auth.registrar("", "a@b.com", "clave-larga-1")).status());
        assertThrows(DatosInvalidosException.class, () -> auth.registrar("Ana", "no-es-mail", "clave-larga-1"));
        assertThrows(DatosInvalidosException.class, () -> auth.registrar("Ana", "a@b.com", "corta"));
        assertThrows(ConflictoException.class,
                () -> auth.registrar("Otra", "cliente@gomarket.com.ar", "clave-larga-1"));
    }

    @Test void el_listado_de_usuarios_no_expone_la_clave_en_claro() {
        assertEquals(2, auth.listarUsuarios().size());
        auth.listarUsuarios().forEach(u -> {
            assertFalse(u.claveHash().contains("admin123"));
            assertFalse(u.claveHash().contains("cliente123"));
        });
    }
}
