package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CriteriosDeBusqueda;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.RangoDePrecios;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {
    private final CatalogoService catalogo = new CatalogoService() {
        @Override public Producto obtener(long id) {
            Producto p = new Producto();
            p.setId(id);
            p.setNombre("Producto " + id);
            p.setActivo(true);
            p.setStock(3);
            p.setPrecio(new BigDecimal("50000"));
            return p;
        }
        @Override public List<Producto> buscar(CriteriosDeBusqueda c) { return List.of(); }
        @Override public List<Categoria> listarCategorias() { return List.of(); }
        @Override public Map<String, Long> contarPorCategoria(CriteriosDeBusqueda c) { return Map.of(); }
        @Override public RangoDePrecios rangoDePrecios() { return RangoDePrecios.vacio(); }
    };

    private SolicitudPedido solicitud(SolicitudPedido.Item... items) {
        SolicitudPedido s = new SolicitudPedido();
        s.setItems(List.of(items));
        return s;
    }

    @Test void recalculaTotalYEnvioEnServidor() {
        assertEquals(new BigDecimal("58900"), PedidoService.validarYCalcular(
                solicitud(new SolicitudPedido.Item(1, 1)), catalogo));
        assertEquals(new BigDecimal("150000"), PedidoService.validarYCalcular(
                solicitud(new SolicitudPedido.Item(1, 3)), catalogo));
    }

    @Test void rechazaCantidadMayorAlStock() {
        assertThrows(IllegalArgumentException.class, () -> PedidoService.validarYCalcular(
                solicitud(new SolicitudPedido.Item(1, 4)), catalogo));
    }

    @Test void rechazaProductosDuplicados() {
        assertThrows(IllegalArgumentException.class, () -> PedidoService.validarYCalcular(
                solicitud(new SolicitudPedido.Item(1, 1), new SolicitudPedido.Item(1, 1)), catalogo));
    }

    @Test void rechazaPedidoVacio() {
        assertThrows(IllegalArgumentException.class, () -> PedidoService.validarYCalcular(
                solicitud(), catalogo));
    }
}
