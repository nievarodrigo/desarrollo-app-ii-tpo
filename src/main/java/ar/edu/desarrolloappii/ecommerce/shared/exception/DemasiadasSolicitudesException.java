package ar.edu.desarrolloappii.ecommerce.shared.exception;

import java.util.Map;

/** Se superó un límite de uso (por ejemplo, consultas a la IA por minuto): HTTP 429 con Retry-After. */
public class DemasiadasSolicitudesException extends ErrorDeNegocio {
    private static final long serialVersionUID = 1L;
    private final long reintentarEnSegundos;

    public DemasiadasSolicitudesException(String mensaje, long reintentarEnSegundos) {
        super(429, "Demasiadas solicitudes", mensaje);
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    @Override public Map<String, String> cabeceras() {
        return Map.of("Retry-After", Long.toString(reintentarEnSegundos));
    }
}
