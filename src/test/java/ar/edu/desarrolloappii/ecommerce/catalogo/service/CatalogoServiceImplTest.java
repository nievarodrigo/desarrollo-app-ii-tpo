package ar.edu.desarrolloappii.ecommerce.catalogo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Categoria;
import ar.edu.desarrolloappii.ecommerce.catalogo.domain.Producto;
import ar.edu.desarrolloappii.ecommerce.catalogo.repository.CategoriaRepository;
import ar.edu.desarrolloappii.ecommerce.catalogo.repository.ProductoRepository;
import ar.edu.desarrolloappii.ecommerce.shared.exception.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas del componente Catálogo.
 *
 * El service se instancia a mano con repositorios falsos: no hace falta
 * levantar el contenedor porque las dependencias entran por constructor y
 * están declaradas como interfaces. Esa es la ventaja concreta de programar
 * contra contratos, no una formalidad de la rúbrica.
 */
class CatalogoServiceImplTest {

    private CatalogoService catalogo;

    /** Repositorio falso: devuelve una lista fija, sin base de datos. */
    private record ProductosFalsos(List<Producto> datos) implements ProductoRepository {
        @Override
        public List<Producto> buscarTodos() {
            return datos;
        }

        @Override
        public Optional<Producto> buscarPorId(long id) {
            return datos.stream().filter(p -> p.getId() == id).findFirst();
        }
    }

    private record CategoriasFalsas(List<Categoria> datos) implements CategoriaRepository {
        @Override
        public List<Categoria> buscarTodas() {
            return datos;
        }
    }

    private static Producto producto(long id, String nombre, String categoria,
                                     long precio, int stock, double rating,
                                     boolean activo, String... etiquetas) {
        Producto p = new Producto();
        p.setId(id);
        p.setSku("SKU-" + id);
        p.setNombre(nombre);
        p.setMarca("Marca");
        p.setCategoria(categoria);
        p.setPrecio(BigDecimal.valueOf(precio));
        p.setStock(stock);
        p.setRating(rating);
        p.setActivo(activo);
        p.setDescripcion("Descripción de " + nombre);
        p.setEtiquetas(List.of(etiquetas));
        return p;
    }

    @BeforeEach
    void prepararCatalogo() {
        List<Producto> datos = List.of(
                producto(1, "Auriculares Cancelación Activa", "audio", 289900, 8, 4.8, true, "destacado"),
                producto(2, "Parlante Portátil", "audio", 96500, 0, 4.4, true),
                producto(3, "Monitor 27 pulgadas", "tecnologia", 612000, 7, 4.5, true, "destacado"),
                producto(4, "Cafetera de Filtro", "hogar", 227400, 9, 4.6, true),
                producto(5, "Producto Dado de Baja", "hogar", 10000, 5, 5.0, false));

        catalogo = new CatalogoServiceImpl(
                new ProductosFalsos(datos),
                new CategoriasFalsas(List.of()));
    }

    @Nested
    @DisplayName("Visibilidad")
    class Visibilidad {

        @Test
        @DisplayName("un producto inactivo nunca aparece en el catálogo")
        void ocultaLosInactivos() {
            List<Producto> resultado = catalogo.buscar(CriteriosDeBusqueda.sinFiltros());

            assertEquals(4, resultado.size());
            assertTrue(resultado.stream().noneMatch(p -> p.getId() == 5));
        }

        @Test
        @DisplayName("pedir un producto inactivo por id da 'no encontrado'")
        void inactivoNoSeObtienePorId() {
            assertThrows(RecursoNoEncontradoException.class, () -> catalogo.obtener(5));
        }

        @Test
        @DisplayName("pedir un id inexistente da 'no encontrado'")
        void inexistenteNoSeObtiene() {
            assertThrows(RecursoNoEncontradoException.class, () -> catalogo.obtener(999));
        }
    }

    @Nested
    @DisplayName("Búsqueda")
    class Busqueda {

        @Test
        @DisplayName("encuentra sin distinguir acentos ni mayúsculas")
        void ignoraAcentosYMayusculas() {
            var criterios = new CriteriosDeBusqueda("CANCELACION", "todas",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, false, null);

            assertEquals(1, catalogo.buscar(criterios).size());
        }

        @Test
        @DisplayName("exige que estén TODAS las palabras del término")
        void exigeTodasLasPalabras() {
            var coinciden = new CriteriosDeBusqueda("auriculares activa", "todas",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, false, null);
            var noCoinciden = new CriteriosDeBusqueda("auriculares heladera", "todas",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, false, null);

            assertEquals(1, catalogo.buscar(coinciden).size());
            assertEquals(0, catalogo.buscar(noCoinciden).size());
        }
    }

    @Nested
    @DisplayName("Filtros")
    class Filtros {

        @Test
        @DisplayName("filtra por categoría")
        void filtraPorCategoria() {
            var criterios = new CriteriosDeBusqueda("", "audio",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, false, null);

            assertEquals(2, catalogo.buscar(criterios).size());
        }

        @Test
        @DisplayName("'sólo con stock' descarta los agotados")
        void descartaSinStock() {
            var criterios = new CriteriosDeBusqueda("", "audio",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, true, null);

            List<Producto> resultado = catalogo.buscar(criterios);
            assertEquals(1, resultado.size());
            assertTrue(resultado.get(0).getStock() > 0);
        }

        @Test
        @DisplayName("el tope de precio es inclusivo")
        void topeDePrecioInclusivo() {
            var criterios = new CriteriosDeBusqueda("", "todas",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, false, BigDecimal.valueOf(227400));

            List<Producto> resultado = catalogo.buscar(criterios);
            assertEquals(2, resultado.size());
            assertTrue(resultado.stream().anyMatch(p -> p.getId() == 4));
        }
    }

    @Nested
    @DisplayName("Ordenamiento")
    class Ordenamiento {

        @Test
        @DisplayName("por menor precio")
        void porMenorPrecio() {
            var criterios = new CriteriosDeBusqueda("", "todas",
                    CriteriosDeBusqueda.Orden.PRECIO_ASC, false, null);

            assertEquals(2, catalogo.buscar(criterios).get(0).getId());
        }

        @Test
        @DisplayName("por relevancia: primero los destacados, después por rating")
        void porRelevancia() {
            List<Producto> resultado = catalogo.buscar(CriteriosDeBusqueda.sinFiltros());

            assertTrue(resultado.get(0).estaDestacado());
            assertTrue(resultado.get(1).estaDestacado());
            assertEquals(1, resultado.get(0).getId(), "el destacado con mejor rating va primero");
        }
    }

    @Nested
    @DisplayName("Facetas")
    class Facetas {

        @Test
        @DisplayName("los conteos ignoran el filtro de categoría")
        void conteosIgnoranCategoria() {
            var criterios = new CriteriosDeBusqueda("", "audio",
                    CriteriosDeBusqueda.Orden.RELEVANCIA, false, null);

            Map<String, Long> conteo = catalogo.contarPorCategoria(criterios);

            assertEquals(2L, conteo.get("audio"));
            assertEquals(1L, conteo.get("tecnologia"), "si respetara el filtro, esto sería 0");
            assertEquals(1L, conteo.get("hogar"));
        }

        @Test
        @DisplayName("el rango de precios sólo mira productos activos")
        void rangoSoloActivos() {
            RangoDePrecios rango = catalogo.rangoDePrecios();

            assertEquals(BigDecimal.valueOf(96500), rango.min(), "el inactivo de $10.000 no cuenta");
            assertEquals(BigDecimal.valueOf(612000), rango.max());
        }
    }
}
