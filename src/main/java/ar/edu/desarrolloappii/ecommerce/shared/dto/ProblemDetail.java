package ar.edu.desarrolloappii.ecommerce.shared.dto;

/** Error HTTP con formato Problem Details (RFC 9457). */
public record ProblemDetail(String type, String title, int status,
                            String detail, String instance) {
    public static final String MEDIA_TYPE = "application/problem+json";

    public static ProblemDetail badRequest(String detail, String instance) {
        return new ProblemDetail("about:blank", "Solicitud inválida", 400, detail, instance);
    }

    public static ProblemDetail notFound(String detail, String instance) {
        return new ProblemDetail("about:blank", "Recurso no encontrado", 404, detail, instance);
    }
}
