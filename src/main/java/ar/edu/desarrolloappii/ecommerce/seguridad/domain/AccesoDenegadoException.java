package ar.edu.desarrolloappii.ecommerce.seguridad.domain;

import ar.edu.desarrolloappii.ecommerce.shared.exception.ErrorDeNegocio;

/** Está autenticado pero su rol (o su identidad) no alcanza. HTTP 403. */
public class AccesoDenegadoException extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    public AccesoDenegadoException(String mensaje) {
        super(403, "Acceso denegado", mensaje);
    }
}
