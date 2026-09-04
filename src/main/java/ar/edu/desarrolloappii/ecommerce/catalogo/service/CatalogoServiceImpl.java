package ar.edu.desarrolloappii.ecommerce.catalogo.service;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import ar.edu.desarrolloappii.ecommerce.catalogo.repository.CategoriaRepository;
import ar.edu.desarrolloappii.ecommerce.catalogo.repository.ProductoRepository;
import ar.edu.desarrolloappii.ecommerce.shared.exception.RecursoNoEncontradoException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reglas del componente Catálogo.
 *
 * Acá viven las decisiones: qué producto es visible, cómo se busca, cómo se
 * ordena. El controller no toma ninguna de estas decisiones y el repositorio
 * tampoco: sólo entrega filas.
 *
 * Es {@code @Stateless} para obtener el pool de instancias y las transacciones
 * declarativas del contenedor, tal como pide el stack (CDI + EJB + JTA). Hoy no
 * escribe nada, pero cuando entre JPA el manejo transaccional ya está resuelto
 * sin tocar el código.
 *
 * Las dependencias entran por {@code @Inject} y están declaradas como
 * interfaces: este service no sabe si los datos vienen de memoria o de una base.
 */
@Stateless
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    /** Requerido por el contenedor EJB. */
    protected CatalogoServiceImpl() {
        this(null, null);
    }

    @Inject
    public CatalogoServiceImpl(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    @Override
    public List<Producto> buscar(CriteriosDeBusqueda criterios) {
        return productos.buscarTodos().stream()
                .filter(Producto::isActivo)
                .filter(p -> cumpleCategoria(p, criterios))
                .filter(p -> cumpleTexto(p, criterios))
                .filter(p -> cumpleStock(p, criterios))
                .filter(p -> cumplePrecio(p, criterios))
                .sorted(comparador(criterios.orden()))
                .toList();
    }

    @Override
    public Producto obtener(long id) {
        return productos.buscarPorId(id)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un producto activo con id " + id));
    }

    @Override
    public List<Categoria> listarCategorias() {
        return categorias.buscarTodas();
    }

    @Override
    public Map<String, Long> contarPorCategoria(CriteriosDeBusqueda criterios) {
        /* Las facetas se calculan ignorando el filtro de categoría: si no, al
           elegir "Audio" todas las demás mostrarían cero. */
        return buscar(criterios.sinCategoria()).stream()
                .collect(Collectors.groupingBy(
                        Producto::getCategoria,
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    @Override
    public RangoDePrecios rangoDePrecios() {
        List<BigDecimal> precios = productos.buscarTodos().stream()
                .filter(Producto::isActivo)
                .map(Producto::getPrecio)
                .sorted()
                .toList();

        if (precios.isEmpty()) return RangoDePrecios.vacio();
        return new RangoDePrecios(precios.get(0), precios.get(precios.size() - 1));
    }

    /* ---------------------------- criterios ---------------------------- */

    private boolean cumpleCategoria(Producto producto, CriteriosDeBusqueda criterios) {
        return !criterios.filtraPorCategoria()
                || criterios.categoria().equals(producto.getCategoria());
    }

    /**
     * Busca todas las palabras del término dentro de nombre, marca, SKU,
     * categoría y descripción, sin distinguir mayúsculas ni acentos.
     */
    private boolean cumpleTexto(Producto producto, CriteriosDeBusqueda criterios) {
        if (!criterios.filtraPorTexto()) return true;

        String bolsa = normalizar(String.join(" ",
                producto.getNombre(),
                producto.getMarca(),
                producto.getSku(),
                producto.getCategoria(),
                producto.getDescripcion()));

        return normalizar(criterios.q()).isBlank()
                || List.of(normalizar(criterios.q()).split("\\s+")).stream().allMatch(bolsa::contains);
    }

    private boolean cumpleStock(Producto producto, CriteriosDeBusqueda criterios) {
        return !criterios.soloStock() || producto.getStock() > 0;
    }

    private boolean cumplePrecio(Producto producto, CriteriosDeBusqueda criterios) {
        return criterios.precioMax() == null
                || producto.getPrecio().compareTo(criterios.precioMax()) <= 0;
    }

    private Comparator<Producto> comparador(CriteriosDeBusqueda.Orden orden) {
        return switch (orden) {
            case PRECIO_ASC -> Comparator.comparing(Producto::getPrecio);
            case PRECIO_DESC -> Comparator.comparing(Producto::getPrecio).reversed();
            case RATING -> Comparator.comparingDouble(Producto::getRating).reversed();
            case NOMBRE -> Comparator.comparing(Producto::getNombre,
                    java.text.Collator.getInstance(Locale.forLanguageTag("es-AR")));
            /* Relevancia: primero los destacados y, dentro de cada grupo, el
               mejor puntuado. Es el mismo criterio que aplica el front. */
            case RELEVANCIA -> Comparator
                    .comparing(Producto::estaDestacado).reversed()
                    .thenComparing(Comparator.comparingDouble(Producto::getRating).reversed());
        };
    }

    /** Pasa a minúsculas y saca los acentos, para que "cafe" encuentre "café". */
    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .trim();
    }
}
