/**
 * CAPA DE NEGOCIO del componente Catálogo — implementación local.
 *
 * Espeja regla por regla `catalogo/service/CatalogoServiceImpl.java`. Acá viven
 * LAS DECISIONES: qué producto es visible, cómo se busca, cómo se ordena. El
 * repositorio sólo entrega filas y la presentación sólo dibuja.
 *
 * ¿Por qué existe, si el backend ya tiene estas mismas reglas? Porque la tienda
 * se publica en DOS destinos (ver README, "Un mismo front, dos destinos"):
 *
 *   · WAR en WildFly     → hay backend → manda `CatalogoServiceRemoto`, que
 *                          delega estas reglas al `CatalogoServiceImpl` de Java.
 *   · Sitio estático      → no hay backend → manda esta implementación.
 *
 * No es lógica duplicada por descuido: es la MISMA especificación cumplida por
 * dos implementaciones del mismo contrato, y la fábrica elige una sola en
 * tiempo de arranque. Si mañana el catálogo sale de JPA, se borra este archivo
 * y nadie más se entera.
 *
 * COMPONENTE STATELESS: no guarda nada entre llamadas. Dos búsquedas iguales
 * dan el mismo resultado y el orden de las llamadas no importa. Es el análogo
 * exacto del `@Stateless` del backend, y el contraste con `CarritoService`.
 */

import { filtraPorCategoria, filtraPorTexto, sinCategoria } from './criterios-de-busqueda.js';

/** Quita acentos y pasa a minúsculas, para que `cafe` encuentre "Cafetera". */
function normalizar(texto = '') {
  return texto
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '');
}

/**
 * Comparadores de orden — patrón STRATEGY.
 *
 * Cada orden es un algoritmo intercambiable en tiempo de ejecución, elegido por
 * su clave. Agregar un orden nuevo es agregar una entrada acá: no se toca
 * `buscar()` ni aparece un `switch` que crece sin control. Es el mismo mapa de
 * `Comparator` que arma `comparador(Orden)` en el backend.
 */
const ESTRATEGIAS_DE_ORDEN = {
  relevancia: (a, b) =>
    Number(b.etiquetas.includes('destacado')) - Number(a.etiquetas.includes('destacado')) ||
    b.rating - a.rating,
  'precio-asc': (a, b) => a.precio - b.precio,
  'precio-desc': (a, b) => b.precio - a.precio,
  rating: (a, b) => b.rating - a.rating,
  nombre: (a, b) => a.nombre.localeCompare(b.nombre, 'es-AR')
};

/** Error de dominio. No sabe de HTTP: la presentación decide cómo mostrarlo. */
export class RecursoNoEncontradoError extends Error {
  constructor(mensaje) {
    super(mensaje);
    this.name = 'RecursoNoEncontradoError';
  }
}

/**
 * Implementación de `CatalogoService` con las reglas del dominio.
 *
 * Las dependencias entran por el constructor y son CONTRATOS, no clases
 * concretas: este service no sabe si los productos vienen de un JSON, de una
 * base o de una planilla. Es la inyección de dependencias que del otro lado
 * hace `@Inject`.
 */
export class CatalogoServiceLocal {
  /**
   * @param {{buscarTodos: Function, buscarPorId: Function}} productos
   * @param {{buscarTodas: Function}} categorias
   */
  constructor(productos, categorias) {
    this.productos = productos;
    this.categorias = categorias;
  }

  /** Productos que cumplen los criterios, ya filtrados y ordenados. */
  async buscar(criterios) {
    const todos = await this.productos.buscarTodos();

    const resultado = todos
      .filter((p) => p.activo)
      .filter((p) => this.#cumpleCategoria(p, criterios))
      .filter((p) => this.#cumpleTexto(p, criterios))
      .filter((p) => this.#cumpleStock(p, criterios))
      .filter((p) => this.#cumplePrecio(p, criterios))
      .sort(ESTRATEGIAS_DE_ORDEN[criterios.orden] ?? ESTRATEGIAS_DE_ORDEN.relevancia);

    return { items: resultado, total: resultado.length };
  }

  /** @throws {RecursoNoEncontradoError} si no existe o está inactivo */
  async obtener(id) {
    const producto = await this.productos.buscarPorId(id);
    if (!producto || !producto.activo) {
      throw new RecursoNoEncontradoError(`No existe un producto activo con id ${id}`);
    }
    return producto;
  }

  async listarCategorias() {
    return this.categorias.buscarTodas();
  }

  /** Facetas: cuántos resultados por categoría, ignorando el filtro de categoría. */
  async contarPorCategoria(criterios) {
    const { items } = await this.buscar(sinCategoria(criterios));
    const porCategoria = items.reduce((acc, p) => {
      acc[p.categoria] = (acc[p.categoria] ?? 0) + 1;
      return acc;
    }, {});
    return { total: items.length, porCategoria };
  }

  /** Precio más bajo y más alto del catálogo activo. */
  async rangoDePrecios() {
    const todos = await this.productos.buscarTodos();
    const precios = todos.filter((p) => p.activo).map((p) => p.precio);
    return {
      min: precios.length ? Math.min(...precios) : 0,
      max: precios.length ? Math.max(...precios) : 0
    };
  }

  /* --------------------------- reglas privadas --------------------------- */

  #cumpleCategoria(producto, criterios) {
    return !filtraPorCategoria(criterios) || producto.categoria === criterios.categoria;
  }

  /**
   * "Todas las palabras": `mouse inalambrico` exige ambos términos, en
   * cualquier orden y sin importar los acentos.
   */
  #cumpleTexto(producto, criterios) {
    if (!filtraPorTexto(criterios)) return true;
    const bolsa = normalizar(
      `${producto.nombre} ${producto.marca} ${producto.sku} ${producto.categoria} ${producto.descripcion}`
    );
    return normalizar(criterios.q)
      .trim()
      .split(/\s+/)
      .every((palabra) => bolsa.includes(palabra));
  }

  #cumpleStock(producto, criterios) {
    return !criterios.soloStock || producto.stock > 0;
  }

  #cumplePrecio(producto, criterios) {
    return criterios.precioMax === null || producto.precio <= criterios.precioMax;
  }
}
