package ar.edu.desarrolloappii.ecommerce.pedido.domain;

import ar.edu.desarrolloappii.ecommerce.shared.exception.ErrorDeNegocio;

/** El pedido está en un estado que no acepta ese evento. Se responde HTTP 409. */
public class TransicionInvalidaException extends ErrorDeNegocio {

    private static final long serialVersionUID = 1L;

    public TransicionInvalidaException(String estado, EventoPedido evento) {
        super(409, "Transición de estado inválida",
                "No se puede aplicar " + evento + " a un pedido en estado " + estado + ".");
    }
}
