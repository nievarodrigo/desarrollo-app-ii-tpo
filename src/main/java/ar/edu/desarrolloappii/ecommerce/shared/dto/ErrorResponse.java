package ar.edu.desarrolloappii.ecommerce.shared.dto;

/** Cuerpo uniforme de los errores de la API. */
public record ErrorResponse(int estado, String error, String mensaje) {
}
