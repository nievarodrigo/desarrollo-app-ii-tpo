package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.NoAutenticadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.annotation.PostConstruct;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Emite y verifica JWT firmados con HS256 (HMAC-SHA256), sólo con la JDK.
 *
 * Un JWT son tres partes en Base64URL separadas por puntos: cabecera.payload.firma.
 * El payload lleva: iss (emisor), sub (email), rol, iat (emitido) y exp (vence).
 * La firma se calcula con una clave secreta que sólo conoce el servidor: si alguien
 * edita el rol del payload, la firma deja de coincidir y el token se rechaza.
 * Eso es lo que no podía hacer el rol guardado en localStorage (SEC-01).
 *
 * La clave sale de la variable de entorno GOMARKET_JWT_SECRET (o la propiedad de
 * sistema gomarket.jwt.secret). Si no está, se genera una al azar al arrancar:
 * sirve para demo, pero los tokens dejan de valer al reiniciar.
 */
@ApplicationScoped
public class JwtService {
    public static final String EMISOR = "gomarket";
    private static final Logger LOG = Logger.getLogger(JwtService.class.getName());
    private static final String CABECERA = base64url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private byte[] clave;
    private Duration vida;
    private Clock reloj;

    /** Para CDI (el proxy del bean). La configuración real se carga en {@link #iniciar()}. */
    public JwtService() { }

    @PostConstruct
    void iniciar() {
        this.clave = claveDeConfiguracion();
        this.vida = Duration.ofMinutes(30);
        this.reloj = Clock.systemUTC();
    }

    public JwtService(byte[] clave, Duration vida, Clock reloj) {
        if (clave == null || clave.length < 32) {
            throw new IllegalArgumentException("La clave JWT debe tener al menos 32 bytes.");
        }
        this.clave = clave.clone();
        this.vida = vida;
        this.reloj = reloj;
    }

    public long segundosDeVida() { return vida.toSeconds(); }

    public String emitir(Identidad identidad) {
        long ahora = reloj.instant().getEpochSecond();
        String payload = Json.createObjectBuilder()
                .add("iss", EMISOR)
                .add("sub", identidad.email())
                .add("rol", identidad.rol().name())
                .add("iat", ahora)
                .add("exp", ahora + vida.toSeconds())
                .build().toString();
        String sinFirma = CABECERA + "." + base64url(payload.getBytes(StandardCharsets.UTF_8));
        return sinFirma + "." + base64url(firmar(sinFirma));
    }

    /** @throws NoAutenticadoException con un mensaje genérico si el token no sirve (no se explica por qué). */
    public Identidad verificar(String token) {
        try {
            String[] partes = token.split("\\.", -1);
            if (partes.length != 3 || !CABECERA.equals(partes[0])) throw invalido(); // rechaza alg=none y otros
            byte[] esperada = firmar(partes[0] + "." + partes[1]);
            byte[] recibida = Base64.getUrlDecoder().decode(partes[2]);
            if (!MessageDigest.isEqual(esperada, recibida)) throw invalido();

            byte[] json = Base64.getUrlDecoder().decode(partes[1]);
            try (JsonReader lector = Json.createReader(new StringReader(new String(json, StandardCharsets.UTF_8)))) {
                JsonObject payload = lector.readObject();
                if (!EMISOR.equals(payload.getString("iss"))) throw invalido();
                if (reloj.instant().getEpochSecond() >= payload.getJsonNumber("exp").longValue()) {
                    throw new NoAutenticadoException("El token venció. Volvé a iniciar sesión.");
                }
                return new Identidad(payload.getString("sub"), Rol.valueOf(payload.getString("rol")));
            }
        } catch (NoAutenticadoException e) {
            throw e;
        } catch (RuntimeException e) { // Base64 roto, JSON roto, rol desconocido, campo faltante...
            throw invalido();
        }
    }

    private static NoAutenticadoException invalido() {
        return new NoAutenticadoException("Token inválido.");
    }

    private byte[] firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no disponible en esta JDK.", e);
        }
    }

    private static String base64url(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }

    private static byte[] claveDeConfiguracion() {
        String secreto = System.getenv("GOMARKET_JWT_SECRET");
        if (secreto == null || secreto.isBlank()) secreto = System.getProperty("gomarket.jwt.secret");
        if (secreto != null && !secreto.isBlank()) return secreto.getBytes(StandardCharsets.UTF_8);
        LOG.warning("GOMARKET_JWT_SECRET no está definida: se genera una clave al azar. "
                + "Los tokens dejarán de valer al reiniciar el servidor.");
        byte[] azar = new byte[32];
        new SecureRandom().nextBytes(azar);
        return azar;
    }
}
