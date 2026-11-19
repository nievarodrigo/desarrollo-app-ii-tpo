package ar.edu.desarrolloappii.ecommerce.shared.exception;

/** La solicitud se entiende pero contradice algo ya hecho (p. ej. reusar una Idempotency-Key con otro cuerpo): HTTP 422. */
public class ContenidoNoProcesableException extends ErrorDeNegocio {
    private static final long serialVersionUID = 1L;

    public ContenidoNoProcesableException(String mensaje) {
        super(422, "Contenido no procesable", mensaje);
    }
}
