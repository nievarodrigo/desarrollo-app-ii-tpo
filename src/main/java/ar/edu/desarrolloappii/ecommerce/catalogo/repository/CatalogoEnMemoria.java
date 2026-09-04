package ar.edu.desarrolloappii.ecommerce.catalogo.repository;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación en memoria de los repositorios del catálogo.
 *
 * Lee la semilla de {@code /datos/catalogo.json}, que es EXACTAMENTE el mismo
 * archivo que sirve el front en {@code assets/data/catalogo.json}: el pom lo
 * agrega al classpath desde {@code src/main/webapp/assets/data}. Una sola
 * fuente de datos evita que el catálogo del backend y el del front se
 * desincronicen.
 *
 * Es {@code @ApplicationScoped} para cargar el JSON una única vez por
 * despliegue. Cuando entre JPA, esta clase se reemplaza por una que use
 * {@code EntityManager} y el JSON pasa a ser sólo carga inicial: los contratos
 * {@link ProductoRepository} y {@link CategoriaRepository} no cambian.
 */
@ApplicationScoped
public class CatalogoEnMemoria implements ProductoRepository, CategoriaRepository {

    private static final Logger LOG = Logger.getLogger(CatalogoEnMemoria.class.getName());
    private static final String RECURSO = "/datos/catalogo.json";

    private List<Producto> productos = List.of();
    private List<Categoria> categorias = List.of();

    /** Estructura del archivo semilla. Sólo se usa para leerlo. */
    public static class Semilla {
        private List<Categoria> categorias = new ArrayList<>();
        private List<Producto> productos = new ArrayList<>();

        public List<Categoria> getCategorias() { return categorias; }
        public void setCategorias(List<Categoria> categorias) { this.categorias = categorias; }

        public List<Producto> getProductos() { return productos; }
        public void setProductos(List<Producto> productos) { this.productos = productos; }
    }

    @PostConstruct
    void cargar() {
        try (InputStream entrada = getClass().getResourceAsStream(RECURSO);
             Jsonb jsonb = JsonbBuilder.create()) {

            if (entrada == null) {
                throw new IllegalStateException("No se encontró el recurso " + RECURSO);
            }

            Semilla semilla = jsonb.fromJson(entrada, Semilla.class);
            this.categorias = List.copyOf(semilla.getCategorias());
            this.productos = List.copyOf(semilla.getProductos());

            LOG.log(Level.INFO, "Catálogo cargado: {0} productos, {1} categorías",
                    new Object[] { productos.size(), categorias.size() });

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el lector del catálogo", e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el catálogo desde " + RECURSO, e);
        }
    }

    @Override
    public List<Producto> buscarTodos() {
        return productos;
    }

    @Override
    public Optional<Producto> buscarPorId(long id) {
        return productos.stream().filter(p -> p.getId() == id).findFirst();
    }

    @Override
    public List<Categoria> buscarTodas() {
        return categorias;
    }
}
