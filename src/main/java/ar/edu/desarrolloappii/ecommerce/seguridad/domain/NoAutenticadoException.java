package ar.edu.desarrolloappii.ecommerce.seguridad.domain;

import ar.edu.desarrolloappii.ecommerce.shared.exception.ErrorDeNegocio;
import java.util.Map;

/** Falta el token, es inválido/vencido o las credenciales no coinciden. HTTP 401. */
public class NoAutenticadoException extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    public NoAutenticadoException(String mensaje) {
        super(401, "No autenticado", mensaje);
    }

    @Override public Map<String, String> cabeceras() {
        return Map.of("WWW-Authenticate", "Bearer");
    }
}
