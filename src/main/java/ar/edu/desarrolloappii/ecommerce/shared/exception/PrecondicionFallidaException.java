package ar.edu.desarrolloappii.ecommerce.shared.exception;

/** El {@code If-Match} no coincide con la versión actual del recurso: HTTP 412. */
public class PrecondicionFallidaException extends ErrorDeNegocio {
    private static final long serialVersionUID = 1L;

    public PrecondicionFallidaException(String mensaje) {
        super(412, "Precondición fallida", mensaje);
    }
}
