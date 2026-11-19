package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.TransicionInvalidaException;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoRepository;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import java.util.logging.Logger;

/**
 * Reintenta publicar los pedidos que quedaron en ERROR_ENVIO porque el broker estaba caído.
 *
 * Corre cada 15 segundos (timer EJB no persistente). Para cada pedido: ERROR_ENVIO →
 * REINTENTAR_ENVIO → PENDIENTE y lo publica. Si el broker sigue caído, vuelve a
 * ERROR_ENVIO y se intenta en la próxima vuelta. Así ningún pedido aceptado se pierde.
 */
@Singleton
public class ReenvioDePedidos {
    private static final Logger LOG = Logger.getLogger(ReenvioDePedidos.class.getName());

    @Inject PedidoRepository repositorio;
    @Inject PublicadorPedidos publicador;

    public ReenvioDePedidos() { }

    /** Para tests, sin contenedor. */
    public ReenvioDePedidos(PedidoRepository repositorio, PublicadorPedidos publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    @Schedule(second = "*/15", minute = "*", hour = "*", persistent = false)
    public void programado() {
        reintentarPendientes();
    }

    /** @return cuántos pedidos se pudieron publicar en esta vuelta. */
    public int reintentarPendientes() {
        int publicados = 0;
        for (Pedido pedido : repositorio.listarPorEstado("ERROR_ENVIO")) {
            try {
                Pedido pendiente = repositorio.aplicar(pedido.getId(), EventoPedido.REINTENTAR_ENVIO);
                if (pendiente == null) continue;
                publicador.publicar(pendiente);
                publicados++;
                LOG.info("Pedido " + pedido.getId() + " republicado tras la caída del broker.");
            } catch (TransicionInvalidaException cambio) {
                // Lo cancelaron o lo procesaron mientras tanto: no hay nada que reenviar.
            } catch (RuntimeException brokerCaido) {
                marcarFallido(pedido.getId());
                LOG.warning("El broker sigue sin responder; " + pedido.getId() + " se reintenta más tarde.");
                break; // si el broker está caído, el resto también va a fallar
            }
        }
        return publicados;
    }

    private void marcarFallido(String id) {
        try {
            repositorio.aplicar(id, EventoPedido.ENVIO_FALLIDO);
        } catch (TransicionInvalidaException yaAvanzo) {
            // entrega ambigua: el consumidor ya lo tomó
        }
    }
}
