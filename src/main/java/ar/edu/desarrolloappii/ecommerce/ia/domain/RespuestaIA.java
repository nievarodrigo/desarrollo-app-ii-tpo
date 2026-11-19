package ar.edu.desarrolloappii.ecommerce.ia.domain;

/**
 * Lo que devuelve el gateway al front.
 *
 * @param estado    sugerencia | aclarar | rechazada | manual | invalida
 * @param criterios filtros sugeridos (null salvo en "sugerencia")
 * @param origen    quién respondió: "modelo", "reglas" (fallback) o "politica" (lo frenó el gateway)
 * @param motivo    por qué se usó el fallback (timeout, error, salida-invalida, sin-proveedor, saturado) o null
 * @param milisegundos tiempo total dentro del gateway
 */
public record RespuestaIA(String estado, CriteriosSugeridos criterios, String origen, String motivo, long milisegundos) { }
