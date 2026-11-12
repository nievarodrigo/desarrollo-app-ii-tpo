package ar.edu.desarrolloappii.ecommerce.shared.exception;

/** Datos de entrada inválidos: el mapper de negocio los traduce a HTTP 400. */
public class DatosInvalidosException extends ErrorDeNegocio {
    private static final long serialVersionUID = 1L;

    public DatosInvalidosException(String mensaje) {
        super(400, "Datos inválidos", mensaje);
    }
}
