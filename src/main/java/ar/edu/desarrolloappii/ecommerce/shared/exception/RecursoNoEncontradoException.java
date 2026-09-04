package ar.edu.desarrolloappii.ecommerce.shared.exception;

import jakarta.ejb.ApplicationException;

/**
 * El recurso pedido no existe.
 *
 * La lanza el negocio y la traduce a HTTP 404 el {@link RecursoNoEncontradoMapper}.
 * De esa forma el service no importa nada de JAX-RS: no sabe que existe HTTP.
 */
@ApplicationException(rollback = false)
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
