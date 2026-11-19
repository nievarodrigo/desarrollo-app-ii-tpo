package ar.edu.desarrolloappii.ecommerce.shared.http;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Calcula ETags fuertes a partir de la representación JSON de un recurso.
 *
 * Dos representaciones iguales dan el mismo ETag; cualquier cambio (precio, stock,
 * estado del pedido) da uno distinto. Con eso la API responde 304 a un GET con
 * {@code If-None-Match} y 412 a un cambio con {@code If-Match} desactualizado.
 */
public final class Etags {
    private static final Jsonb JSONB = JsonbBuilder.create();

    private Etags() { }

    /** ETag (sin comillas) del objeto serializado a JSON. */
    public static String de(Object representacion) {
        return huella(JSONB.toJson(representacion));
    }

    /** SHA-256 en hexadecimal, recortado a 32 caracteres: suficiente para distinguir versiones. */
    public static String huella(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("La JVM no trae SHA-256.", imposible);
        }
    }

    /**
     * ¿El valor de {@code If-Match} coincide con el ETag actual?
     * Acepta {@code *}, comillas, prefijo débil {@code W/} y listas separadas por coma.
     */
    public static boolean coincide(String ifMatch, String etagActual) {
        if (ifMatch == null || ifMatch.isBlank()) return true;
        for (String candidato : ifMatch.split(",")) {
            String limpio = candidato.trim();
            if (limpio.equals("*")) return true;
            if (limpio.startsWith("W/")) limpio = limpio.substring(2);
            limpio = limpio.replace("\"", "");
            if (limpio.equals(etagActual)) return true;
        }
        return false;
    }
}
