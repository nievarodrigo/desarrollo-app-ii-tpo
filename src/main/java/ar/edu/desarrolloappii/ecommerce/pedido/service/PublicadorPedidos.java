package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;

/** Puerto de mensajería: el service publica el pedido sin saber que detrás hay JMS. */
public interface PublicadorPedidos {
    /** Nombre de la propiedad JMS con el identificador único de cada mensaje (para deduplicar). */
    String PROPIEDAD_ID_MENSAJE = "idMensaje";

    /** Publica el comando "procesar pedido". Lanza una excepción si no se pudo enviar. */
    void publicar(Pedido pedido);

    /** El relay conserva el identificador en todas las reentregas. */
    default void publicar(Pedido pedido, String idMensaje) { publicar(pedido); }
}
