package ar.edu.desarrolloappii.ecommerce.seguridad.domain;

import ar.edu.desarrolloappii.ecommerce.shared.exception.ErrorDeNegocio;

/** El recurso ya existe (por ejemplo, un email ya registrado). HTTP 409. */
public class ConflictoException extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(409, "Conflicto", mensaje);
    }
}
