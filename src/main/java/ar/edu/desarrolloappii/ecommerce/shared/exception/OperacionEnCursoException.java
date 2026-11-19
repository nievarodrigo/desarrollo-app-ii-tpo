package ar.edu.desarrolloappii.ecommerce.shared.exception;

import java.util.Map;

/** Ya hay una solicitud con la misma Idempotency-Key procesándose: HTTP 409 con Retry-After. */
public class OperacionEnCursoException extends ErrorDeNegocio {
    private static final long serialVersionUID = 1L;

    public OperacionEnCursoException(String mensaje) {
        super(409, "Operación en curso", mensaje);
    }

    @Override public Map<String, String> cabeceras() { return Map.of("Retry-After", "1"); }
}
