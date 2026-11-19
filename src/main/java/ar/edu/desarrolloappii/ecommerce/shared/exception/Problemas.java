package ar.edu.desarrolloappii.ecommerce.shared.exception;

import ar.edu.desarrolloappii.ecommerce.shared.dto.ProblemDetail;
import jakarta.ws.rs.WebApplicationException;
import java.util.Map;

/**
 * Traduce CUALQUIER excepción a Problem Details (RFC 9457).
 *
 * Recorre la cadena de causas porque los EJB envuelven las excepciones de negocio
 * en {@code EJBException}: sin esto, un 404 lanzado desde un {@code @Stateless}
 * saldría como 500. Lo que no se reconoce es un 500 genérico, sin stack trace ni
 * mensajes internos hacia el cliente.
 */
public final class Problemas {
    private Problemas() { }

    /** Problema HTTP más las cabeceras que lo acompañan (Retry-After, Allow...). */
    public record Resultado(ProblemDetail problema, Map<String, String> cabeceras) { }

    public static Resultado desde(Throwable error, String instance) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ErrorDeNegocio negocio) {
                return new Resultado(new ProblemDetail("about:blank", negocio.titulo(), negocio.status(),
                        negocio.getMessage(), instance), negocio.cabeceras());
            }
            if (causa instanceof RecursoNoEncontradoException noEncontrado) {
                return new Resultado(ProblemDetail.notFound(noEncontrado.getMessage(), instance), Map.of());
            }
            if (causa instanceof WebApplicationException web && web.getResponse() != null) {
                int status = web.getResponse().getStatus();
                return new Resultado(new ProblemDetail("about:blank", tituloDe(status), status,
                        detalleDe(status), instance), Map.of());
            }
            if (causa.getCause() == causa) break;
        }
        return new Resultado(new ProblemDetail("about:blank", "Error interno", 500,
                "Ocurrió un error inesperado. Intentá de nuevo más tarde.", instance), Map.of());
    }

    static String tituloDe(int status) {
        return switch (status) {
            case 400 -> "Solicitud inválida";
            case 401 -> "No autenticado";
            case 403 -> "Acceso denegado";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 406 -> "Formato no aceptable";
            case 409 -> "Conflicto";
            case 412 -> "Precondición fallida";
            case 415 -> "Tipo de contenido no soportado";
            case 422 -> "Contenido no procesable";
            case 429 -> "Demasiadas solicitudes";
            case 503 -> "Servicio no disponible";
            default -> status >= 500 ? "Error interno" : "Error en la solicitud";
        };
    }

    /** Mensajes fijos: nunca se reenvía el texto interno de la excepción del framework. */
    static String detalleDe(int status) {
        return switch (status) {
            case 404 -> "La ruta pedida no existe en la API.";
            case 405 -> "La ruta existe pero no admite ese método HTTP.";
            case 406 -> "La API sólo responde JSON.";
            case 415 -> "El cuerpo debe enviarse como application/json.";
            default -> status >= 500 ? "Ocurrió un error inesperado. Intentá de nuevo más tarde."
                    : "La solicitud no pudo procesarse.";
        };
    }
}
