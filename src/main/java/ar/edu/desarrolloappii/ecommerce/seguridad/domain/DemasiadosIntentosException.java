package ar.edu.desarrolloappii.ecommerce.seguridad.domain;

import ar.edu.desarrolloappii.ecommerce.shared.exception.ErrorDeNegocio;
import java.util.Map;

/** Demasiados ingresos fallidos seguidos: la cuenta queda bloqueada un rato. HTTP 429. */
public class DemasiadosIntentosException extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    private final long reintentarEnSegundos;

    public DemasiadosIntentosException(long reintentarEnSegundos) {
        super(429, "Demasiados intentos",
                "Demasiados intentos fallidos. Probá de nuevo en " + reintentarEnSegundos + " segundos.");
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long reintentarEnSegundos() { return reintentarEnSegundos; }

    @Override public Map<String, String> cabeceras() {
        return Map.of("Retry-After", Long.toString(reintentarEnSegundos));
    }
}
