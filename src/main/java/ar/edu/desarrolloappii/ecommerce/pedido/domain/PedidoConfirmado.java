package ar.edu.desarrolloappii.ecommerce.pedido.domain;

import java.math.BigDecimal;

/**
 * Evento de dominio que se publica en {@code PedidoConfirmadoTopic} cuando un pedido pasa a CONFIRMADO.
 * Mismo contrato que {@code entrega-diseno/contratos/evento-pedido-confirmado.json}.
 * Lleva el mínimo de datos: ni email ni datos de pago del cliente (Clase 11, mensajería).
 *
 * @param idEvento identificador único: cada suscriptor lo usa para descartar duplicados
 * @param version  versión del contrato del evento (para evolucionarlo sin romper suscriptores)
 */
public record PedidoConfirmado(String idEvento, String tipoEvento, int version, String fecha,
                               String idPedido, String correlationId, BigDecimal total, String moneda) {
    public static final String TIPO = "PedidoConfirmado";
}
