package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CatalogoService;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.CriteriosDeBusqueda;
import ar.edu.desarrolloappii.ecommerce.catalogo.service.RangoDePrecios;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Catálogo falso: todo producto existe, tiene 3 unidades y cuesta $50.000. */
class CatalogoDePrueba implements CatalogoService {
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
}
