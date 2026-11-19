package ar.edu.desarrolloappii.ecommerce.pedido.controller;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PedidoResourceTest {
    private static SolicitudPedido solicitud(SolicitudPedido.Item... items) {
        SolicitudPedido s = new SolicitudPedido();
        s.setItems(List.of(items));
        return s;
    }

    @Test void laHuellaNoDependeDelOrdenDeLosItems() {
        assertEquals(
                PedidoResource.huellaDe(solicitud(new SolicitudPedido.Item(1, 2), new SolicitudPedido.Item(5, 1))),
                PedidoResource.huellaDe(solicitud(new SolicitudPedido.Item(5, 1), new SolicitudPedido.Item(1, 2))));
    }

    @Test void otraCantidadEsOtraHuella() {
        assertNotEquals(
                PedidoResource.huellaDe(solicitud(new SolicitudPedido.Item(1, 2))),
                PedidoResource.huellaDe(solicitud(new SolicitudPedido.Item(1, 3))));
    }
}
