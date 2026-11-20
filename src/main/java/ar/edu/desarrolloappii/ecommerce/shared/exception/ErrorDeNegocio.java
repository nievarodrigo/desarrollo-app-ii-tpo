package ar.edu.desarrolloappii.ecommerce.shared.exception;

import java.util.Map;

/**
 * Error que el negocio quiere que el cliente vea como un código HTTP concreto.
 *
 * El service/dominio lanza una subclase (sin saber nada de HTTP) y el
 * {@link ErrorDeNegocioMapper} la traduce a la respuesta. Así hay un solo lugar
 * donde se decide "esto es un 401, esto un 403, esto un 409".
 */
@jakarta.ejb.ApplicationException(inherited = true, rollback = false)
public abstract class ErrorDeNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String titulo;

    protected ErrorDeNegocio(int status, String titulo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.titulo = titulo;
    }

    public int status() { return status; }
    public String titulo() { return titulo; }

    /** Cabeceras HTTP extra que acompañan al error (por ejemplo Retry-After). */
    public Map<String, String> cabeceras() { return Map.of(); }
}
