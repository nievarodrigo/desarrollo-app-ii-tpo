package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.NoAutenticadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final byte[] CLAVE = "una-clave-de-prueba-de-32-bytes-o-mas!".getBytes(StandardCharsets.UTF_8);
    private static final Instant AHORA = Instant.parse("2026-11-13T12:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final JwtService jwt = new JwtService(CLAVE, Duration.ofMinutes(30), reloj);

    private static String b64(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test void ida_y_vuelta_conserva_email_y_rol() {
        Identidad i = jwt.verificar(jwt.emitir(new Identidad("ana@gomarket.com.ar", Rol.CLIENTE)));
        assertEquals("ana@gomarket.com.ar", i.email());
        assertEquals(Rol.CLIENTE, i.rol());
    }

    @Test void el_token_tiene_tres_partes() {
        assertEquals(3, jwt.emitir(new Identidad("a@b.com", Rol.ADMIN)).split("\\.").length);
    }

    @Test void editar_el_rol_en_el_payload_rompe_la_firma() {
        String[] p = jwt.emitir(new Identidad("ana@gomarket.com.ar", Rol.CLIENTE)).split("\\.");
        String payloadAdmin = b64("{\"iss\":\"gomarket\",\"sub\":\"ana@gomarket.com.ar\",\"rol\":\"ADMIN\","
                + "\"iat\":0,\"exp\":99999999999}");
        assertThrows(NoAutenticadoException.class, () -> jwt.verificar(p[0] + "." + payloadAdmin + "." + p[2]));
    }

    @Test void un_token_firmado_con_otra_clave_se_rechaza() {
        JwtService otro = new JwtService("otra-clave-distinta-de-32-bytes-minimo!!".getBytes(StandardCharsets.UTF_8),
                Duration.ofMinutes(30), reloj);
        assertThrows(NoAutenticadoException.class,
                () -> jwt.verificar(otro.emitir(new Identidad("a@b.com", Rol.ADMIN))));
    }

    @Test void un_token_vencido_se_rechaza() {
        String token = jwt.emitir(new Identidad("a@b.com", Rol.CLIENTE));
        JwtService despues = new JwtService(CLAVE, Duration.ofMinutes(30),
                Clock.fixed(AHORA.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));
        assertThrows(NoAutenticadoException.class, () -> despues.verificar(token));
        assertDoesNotThrow(() -> new JwtService(CLAVE, Duration.ofMinutes(30),
                Clock.fixed(AHORA.plus(Duration.ofMinutes(29)), ZoneOffset.UTC)).verificar(token));
    }

    @Test void alg_none_se_rechaza() {
        String token = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "."
                + b64("{\"iss\":\"gomarket\",\"sub\":\"a@b.com\",\"rol\":\"ADMIN\",\"iat\":0,\"exp\":99999999999}") + ".";
        assertThrows(NoAutenticadoException.class, () -> jwt.verificar(token));
    }

    @Test void basura_se_rechaza_sin_otra_excepcion() {
        for (String basura : new String[] {"", "abc", "a.b.c", "....", "???.???.???"}) {
            assertThrows(NoAutenticadoException.class, () -> jwt.verificar(basura), basura);
        }
    }

    @Test void la_clave_corta_no_se_acepta() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("corta".getBytes(StandardCharsets.UTF_8), Duration.ofMinutes(1), reloj));
    }
    @Test void la_clave_corta_de_configuracion_impide_iniciar_el_servicio() {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getenv("GOMARKET_JWT_SECRET") == null);
        String anterior = System.getProperty("gomarket.jwt.secret");
        try {
            System.setProperty("gomarket.jwt.secret", "corta");
            assertThrows(IllegalArgumentException.class, () -> new JwtService().iniciar());
            System.setProperty("gomarket.jwt.secret", new String(CLAVE, StandardCharsets.UTF_8));
            JwtService configurado = new JwtService();
            configurado.iniciar();
            assertEquals(Rol.ADMIN, configurado.verificar(
                    configurado.emitir(new Identidad("a@b.com", Rol.ADMIN))).rol());
        } finally {
            if (anterior == null) System.clearProperty("gomarket.jwt.secret");
            else System.setProperty("gomarket.jwt.secret", anterior);
        }
    }

}
