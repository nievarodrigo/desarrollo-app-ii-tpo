package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;

/** Puerto de mensajería: el service publica el pedido sin saber que detrás hay JMS. */
public interface PublicadorPedidos {
    /** Publica el comando "procesar pedido". Lanza una excepción si no se pudo enviar. */
    void publicar(Pedido pedido);
}
