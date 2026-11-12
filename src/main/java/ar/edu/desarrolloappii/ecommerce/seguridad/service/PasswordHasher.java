package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import jakarta.inject.Singleton;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Guarda claves con un hash lento y con sal (SEC-09): PBKDF2-HMAC-SHA256.
 *
 * Argon2id sería la primera opción de OWASP, pero no viene en la JDK; PBKDF2 sí y
 * es aceptado por OWASP con 600.000 iteraciones. Formato guardado:
 * {@code pbkdf2$iteraciones$sal$hash} (todo en Base64).
 */
@Singleton
public class PasswordHasher {
    static final int ITERACIONES = 600_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private final SecureRandom azar = new SecureRandom();

    public String hashear(String clave) {
        byte[] sal = new byte[BYTES_SAL];
        azar.nextBytes(sal);
        byte[] hash = derivar(clave, sal, ITERACIONES);
        return "pbkdf2$" + ITERACIONES + "$" + b64(sal) + "$" + b64(hash);
    }

    public boolean verificar(String clave, String almacenado) {
        try {
            String[] partes = almacenado.split("\\$");
            if (partes.length != 4 || !"pbkdf2".equals(partes[0])) return false;
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(clave, Base64.getDecoder().decode(partes[2]), Integer.parseInt(partes[1]));
            return MessageDigest.isEqual(esperado, calculado); // comparación en tiempo constante
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible en esta JDK.", e);
        }
    }

    private static String b64(byte[] datos) { return Base64.getEncoder().encodeToString(datos); }
}
