/**
 * Pruebas del componente CATÁLOGO — capa de negocio.
 *
 * Espejo de `CatalogoServiceImplTest.java`: los MISMOS casos, sobre la
 * implementación del front. Si una de las dos implementaciones se desvía de la
 * especificación, un test lo canta.
 *
 * Fijate en lo que NO hay acá: no hay navegador, no hay DOM, no hay red y no
 * hay `localStorage`. Se puede porque `CatalogoServiceLocal` no depende de
 * ninguno de los tres — recibe sus repositorios por constructor y programa
 * contra sus contratos. Eso es lo que Clase 3 (slide 21) llama "testing
 * aislado", y es la prueba concreta de que las capas están de verdad separadas
 * y no sólo dibujadas en el diagrama.
 *
 * Correr con:  node --test src/test/js/
 */

import { test, describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';

import { CatalogoServiceLocal, RecursoNoEncontradoError } from '../../main/webapp/assets/js/componentes/catalogo/negocio/catalogo-service-local.js';
import { criterios } from '../../main/webapp/assets/js/componentes/catalogo/negocio/criterios-de-busqueda.js';

/* ------------------------------------------------------------------ */
/* Dobles de prueba                                                   */
/* ------------------------------------------------------------------ */

/**
 * Repositorio falso. Cumple el contrato en tres líneas, sin base de datos y
 * sin archivos: exactamente lo que hace el test de Java con sus repositorios
 * en memoria.
 */
class ProductoRepositoryFalso {
  constructor(productos) {
    this.productos = productos;
  }
  async buscarTodos() {
    return structuredClone(this.productos);
  }
  async buscarPorId(id) {
    const p = this.productos.find((x) => x.id === Number(id));
    return p ? structuredClone(p) : null;
  }
}

class CategoriaRepositoryFalso {
  constructor(categorias) {
    this.categorias = categorias;
  }
  async buscarTodas() {
    return structuredClone(this.categorias);
  }
}

/** Producto con valores por defecto sensatos; cada test pisa lo que le importa. */
function producto(parcial) {
  return {
    id: 1,
    sku: 'SKU-1',
    nombre: 'Producto',
    marca: 'Marca',
    categoria: 'tecnologia',
    descripcion: 'Descripción del producto',
    precio: 100000,
    precioAnterior: null,
    stock: 10,
    rating: 4.0,
    resenias: 10,
    etiquetas: [],
    destacados: [],
    arte: 'generico',
    activo: true,
    ...parcial
  };
}

const CATALOGO = [
  producto({ id: 1, nombre: 'Cafetera Express', categoria: 'hogar', precio: 180000, rating: 4.5, stock: 8 }),
  producto({ id: 2, nombre: 'Mouse Inalámbrico', categoria: 'tecnologia', precio: 45000, rating: 4.2, stock: 0 }),
  producto({ id: 3, nombre: 'Monitor Curvo', categoria: 'tecnologia', precio: 620000, rating: 4.8, stock: 3, etiquetas: ['destacado'] }),
  producto({ id: 4, nombre: 'Silla Ergonómica', categoria: 'oficina', precio: 320000, rating: 4.1, stock: 5 }),
  producto({ id: 5, nombre: 'Teclado Retro', categoria: 'tecnologia', precio: 95000, rating: 4.9, stock: 12 }),
  producto({ id: 9, nombre: 'Producto Descontinuado', categoria: 'hogar', precio: 10000, activo: false })
];

const CATEGORIAS = [
  { slug: 'tecnologia', nombre: 'Tecnología' },
  { slug: 'hogar', nombre: 'Hogar' },
  { slug: 'oficina', nombre: 'Oficina' }
];

let servicio;

beforeEach(() => {
  servicio = new CatalogoServiceLocal(
    new ProductoRepositoryFalso(CATALOGO),
    new CategoriaRepositoryFalso(CATEGORIAS)
  );
});

const ids = ({ items }) => items.map((p) => p.id);

/* ------------------------------------------------------------------ */

describe('Visibilidad', () => {
  test('un producto inactivo nunca aparece en el catálogo', async () => {
    const resultado = await servicio.buscar(criterios({}));
    assert.ok(!ids(resultado).includes(9), 'el inactivo se filtró');
    assert.equal(resultado.total, 5);
  });

  test('pedir un producto inactivo por id da "no encontrado"', async () => {
    await assert.rejects(() => servicio.obtener(9), RecursoNoEncontradoError);
  });

  test('pedir un id inexistente da "no encontrado"', async () => {
    await assert.rejects(() => servicio.obtener(999), RecursoNoEncontradoError);
  });

  test('un producto activo se obtiene por id', async () => {
    const p = await servicio.obtener(3);
    assert.equal(p.nombre, 'Monitor Curvo');
  });
});

describe('Búsqueda', () => {
  test('encuentra sin distinguir acentos ni mayúsculas', async () => {
    assert.deepEqual(ids(await servicio.buscar(criterios({ q: 'cafe' }))), [1]);
    assert.deepEqual(ids(await servicio.buscar(criterios({ q: 'ERGONOMICA' }))), [4]);
    assert.deepEqual(ids(await servicio.buscar(criterios({ q: 'inalambrico' }))), [2]);
  });

  test('exige que estén TODAS las palabras del término', async () => {
    assert.deepEqual(ids(await servicio.buscar(criterios({ q: 'mouse inalambrico' }))), [2]);
    assert.deepEqual(ids(await servicio.buscar(criterios({ q: 'mouse curvo' }))), []);
  });

  test('busca también por marca, SKU, categoría y descripción', async () => {
    assert.equal((await servicio.buscar(criterios({ q: 'SKU-1' }))).total, 5);
    assert.equal((await servicio.buscar(criterios({ q: 'oficina' }))).total, 1);
  });

  test('un término vacío no filtra nada', async () => {
    assert.equal((await servicio.buscar(criterios({ q: '   ' }))).total, 5);
  });
});

describe('Filtros', () => {
  test('filtra por categoría', async () => {
    const resultado = await servicio.buscar(criterios({ categoria: 'tecnologia' }));
    assert.deepEqual(ids(resultado).sort(), [2, 3, 5]);
  });

  test('"todas" no filtra por categoría', async () => {
    assert.equal((await servicio.buscar(criterios({ categoria: 'todas' }))).total, 5);
  });

  test('"sólo con stock" descarta los agotados', async () => {
    const resultado = await servicio.buscar(criterios({ soloStock: true }));
    assert.ok(!ids(resultado).includes(2), 'el agotado se filtró');
    assert.equal(resultado.total, 4);
  });

  test('el tope de precio es inclusivo', async () => {
    const resultado = await servicio.buscar(criterios({ precioMax: 180000 }));
    assert.ok(ids(resultado).includes(1), 'un producto de exactamente 180000 entra');
    assert.deepEqual(ids(resultado).sort(), [1, 2, 5]);
  });

  test('los filtros se combinan entre sí', async () => {
    const resultado = await servicio.buscar(
      criterios({ categoria: 'tecnologia', soloStock: true, precioMax: 100000 })
    );
    assert.deepEqual(ids(resultado), [5]);
  });
});

describe('Ordenamiento', () => {
  test('por menor precio', async () => {
    assert.deepEqual(ids(await servicio.buscar(criterios({ orden: 'precio-asc' }))), [2, 5, 1, 4, 3]);
  });

  test('por mayor precio', async () => {
    assert.deepEqual(ids(await servicio.buscar(criterios({ orden: 'precio-desc' }))), [3, 4, 1, 5, 2]);
  });

  test('por relevancia: primero los destacados, después por rating', async () => {
    const orden = ids(await servicio.buscar(criterios({ orden: 'relevancia' })));
    assert.equal(orden[0], 3, 'el destacado va primero aunque no tenga el mejor rating solo');
    assert.deepEqual(orden.slice(1), [5, 1, 2, 4], 'el resto por rating descendente');
  });

  test('por nombre, respetando el alfabeto español', async () => {
    const nombres = (await servicio.buscar(criterios({ orden: 'nombre' }))).items.map((p) => p.nombre);
    assert.deepEqual(nombres, [...nombres].sort((a, b) => a.localeCompare(b, 'es-AR')));
  });

  test('un orden desconocido cae en relevancia y no rompe', async () => {
    const desconocido = ids(await servicio.buscar(criterios({ orden: 'inventado' })));
    const relevancia = ids(await servicio.buscar(criterios({ orden: 'relevancia' })));
    assert.deepEqual(desconocido, relevancia);
  });
});

describe('Facetas', () => {
  test('los conteos ignoran el filtro de categoría', async () => {
    const { porCategoria, total } = await servicio.contarPorCategoria(criterios({ categoria: 'hogar' }));
    assert.equal(total, 5, 'cuenta sobre todo el catálogo, no sólo sobre hogar');
    assert.deepEqual(porCategoria, { hogar: 1, tecnologia: 3, oficina: 1 });
  });

  test('los conteos SÍ respetan los demás filtros', async () => {
    const { porCategoria } = await servicio.contarPorCategoria(criterios({ soloStock: true }));
    assert.equal(porCategoria.tecnologia, 2, 'el mouse agotado no se cuenta');
  });

  test('el rango de precios sale del catálogo activo', async () => {
    const { min, max } = await servicio.rangoDePrecios();
    assert.equal(min, 45000, 'el inactivo de $10.000 no cuenta');
    assert.equal(max, 620000);
  });
});

describe('Contratos y aislamiento', () => {
  test('lista las categorías que le da su repositorio', async () => {
    const categorias = await servicio.listarCategorias();
    assert.deepEqual(categorias.map((c) => c.slug), ['tecnologia', 'hogar', 'oficina']);
  });

  test('es STATELESS: la misma búsqueda repetida da el mismo resultado', async () => {
    const primera = await servicio.buscar(criterios({ q: 'teclado' }));
    await servicio.buscar(criterios({ categoria: 'hogar', soloStock: true }));
    const segunda = await servicio.buscar(criterios({ q: 'teclado' }));
    assert.deepEqual(ids(primera), ids(segunda), 'ninguna llamada anterior lo afectó');
  });

  test('no muta el catálogo que le entrega el repositorio', async () => {
    const resultado = await servicio.buscar(criterios({}));
    resultado.items[0].precio = 1;
    const otraVez = await servicio.buscar(criterios({}));
    assert.notEqual(otraVez.items[0].precio, 1, 'el repositorio sigue intacto');
  });

  test('funciona con un catálogo vacío sin lanzar', async () => {
    const vacio = new CatalogoServiceLocal(new ProductoRepositoryFalso([]), new CategoriaRepositoryFalso([]));
    assert.equal((await vacio.buscar(criterios({}))).total, 0);
    assert.deepEqual(await vacio.rangoDePrecios(), { min: 0, max: 0 });
  });
});
