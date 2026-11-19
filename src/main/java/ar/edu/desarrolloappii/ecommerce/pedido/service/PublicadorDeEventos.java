package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;

/**
 * Puerto pub/sub (patrón Observer entre aplicaciones, Clase 12): el service anuncia que algo pasó
 * sin saber quién escucha ni que atrás hay un tópico JMS.
 */
public interface PublicadorDeEventos {
    /** Publica el evento. Lanza una excepción si no se pudo enviar. */
    void publicar(PedidoConfirmado evento);

    /** Para tests y para cuando no hay broker: no publica nada. */
    PublicadorDeEventos NINGUNO = evento -> { };
}
