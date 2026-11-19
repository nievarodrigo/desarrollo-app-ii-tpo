package ar.edu.desarrolloappii.ecommerce.ia.domain;

/**
 * Contrato de salida que se le exige al modelo (structured outputs). El servidor la
 * vuelve a validar igual: un esquema no reemplaza a la validación propia.
 *
 * @param estado sugerencia | aclarar | rechazada
 * @param criterios sólo presentes cuando estado = sugerencia
 */
public record SalidaModelo(String estado, CriteriosSugeridos criterios) { }
