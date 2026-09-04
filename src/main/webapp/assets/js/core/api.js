/**
 * Capa de acceso a datos del front.
 *
 * La UI NUNCA importa los datos directamente: habla siempre con este módulo.
 * Los contratos coinciden con los recursos JAX-RS del backend Jakarta EE
 * (`TPO/MOCKUP_TPO.md` §11), así que la interfaz no cambia según de dónde
 * salgan los datos.
 *
 * MODO 'auto' (por defecto): al arrancar se le pregunta a `GET api/salud` si
 * hay backend.
 *   · Desplegado como WAR en WildFly → responde el `SaludResource` → HTTP.
 *   · Desplegado como sitio estático → responde un JSON que declara que no hay
 *     backend → mock leyendo `assets/data/catalogo.json`.
 * De esa forma el MISMO código funciona en los dos destinos, sin ramas por
 * entorno ni errores de red en consola.
 */

import { cargarCatalogo } from '../data/catalog.js';

/** 'auto' | 'http' | 'mock' — forzar uno sirve para probar. */
const MODO = 'auto';

/**
 * Base de la API, derivada del path del documento.
 *
 * NO se fija en '/api' a propósito: el WAR se despliega bajo un context root
 * (`ecommerce.war` → `/ecommerce/`) y ahí los recursos JAX-RS viven en
 * `/ecommerce/api/...`. Con base relativa la misma UI funciona servida desde la
 * raíz o bajo cualquier context root.
 *
 * Sin secretos ni credenciales: es sólo una ruta.
 */
const BASE_URL = new URL('api/', document.baseURI);

/** Latencia simulada en modo mock, para que los estados de carga sean reales. */
const LATENCIA_MS = [220, 480];

/** Falla de red simulada, de un solo uso, para poder ver el estado de error. */
let fallaForzada = false;

export function forzarProximoError(valor = true) {
  fallaForzada = valor;
}

class ErrorDeRed extends Error {
  constructor(mensaje) {
    super(mensaje);
    this.name = 'ErrorDeRed';
  }
}

/* ------------------------------------------------------------------ */
/* Resolución del modo                                                 */
/* ------------------------------------------------------------------ */

/** @type {Promise<'http'|'mock'>|null} */
let modoResuelto = null;

function resolverModo() {
  if (MODO !== 'auto') return Promise.resolve(MODO);
  if (modoResuelto) return modoResuelto;

  modoResuelto = fetch(new URL('salud', BASE_URL), { headers: { Accept: 'application/json' } })
    .then((r) => (r.ok ? r.json() : { disponible: false }))
    .then((estado) => (estado?.disponible === true ? 'http' : 'mock'))
    .catch(() => 'mock');

  return modoResuelto;
}

/** Modo efectivo ya resuelto, para mostrarlo en la UI. `null` si todavía no se sabe. */
let modoEfectivo = null;
export function modoDeDatos() {
  return modoEfectivo;
}

/* ------------------------------------------------------------------ */
/* Utilidades del modo mock                                            */
/* ------------------------------------------------------------------ */

function demorar() {
  const [min, max] = LATENCIA_MS;
  return new Promise((resolve) => setTimeout(resolve, min + Math.random() * (max - min)));
}

function normalizar(texto = '') {
  return texto
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

/**
 * Filtra y ordena en memoria replicando lo que hace `CatalogoService` en el
 * backend. Las dos implementaciones tienen que dar el mismo resultado.
 * @param {{q?:string, categoria?:string, orden?:string, soloStock?:boolean, precioMax?:number}} filtros
 */
function aplicarFiltros(lista, filtros = {}) {
  const { q = '', categoria = 'todas', orden = 'relevancia', soloStock = false, precioMax = null } = filtros;
  const termino = normalizar(q).trim();

  let resultado = lista.filter((p) => p.activo);

  if (categoria && categoria !== 'todas') {
    resultado = resultado.filter((p) => p.categoria === categoria);
  }

  if (termino) {
    const palabras = termino.split(/\s+/);
    resultado = resultado.filter((p) => {
      const bolsa = normalizar(`${p.nombre} ${p.marca} ${p.sku} ${p.categoria} ${p.descripcion}`);
      return palabras.every((palabra) => bolsa.includes(palabra));
    });
  }

  if (soloStock) resultado = resultado.filter((p) => p.stock > 0);

  if (typeof precioMax === 'number' && Number.isFinite(precioMax)) {
    resultado = resultado.filter((p) => p.precio <= precioMax);
  }

  const ordenadores = {
    relevancia: (a, b) =>
      Number(b.etiquetas.includes('destacado')) - Number(a.etiquetas.includes('destacado')) ||
      b.rating - a.rating,
    'precio-asc': (a, b) => a.precio - b.precio,
    'precio-desc': (a, b) => b.precio - a.precio,
    rating: (a, b) => b.rating - a.rating,
    nombre: (a, b) => a.nombre.localeCompare(b.nombre, 'es-AR')
  };

  return [...resultado].sort(ordenadores[orden] ?? ordenadores.relevancia);
}

/* ------------------------------------------------------------------ */
/* Transporte HTTP                                                     */
/* ------------------------------------------------------------------ */

async function pedirHttp(ruta, params = {}) {
  /* La barra inicial se quita para que `ruta` se resuelva contra BASE_URL y no
     contra la raíz del host. */
  const url = new URL(ruta.replace(/^\//, ''), BASE_URL);
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor !== null && valor !== undefined && valor !== '') url.searchParams.set(clave, valor);
  });

  const respuesta = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!respuesta.ok) throw new ErrorDeRed(`La API respondió ${respuesta.status}`);
  return respuesta.json();
}

/**
 * Ejecuta la variante HTTP o la variante mock según el modo resuelto.
 * Centralizarlo acá evita repetir el `if (modo === 'http')` en cada función.
 */
async function segunModo(porHttp, porMock) {
  const modo = await resolverModo();
  modoEfectivo = modo;
  if (modo === 'http') return porHttp();
  await demorar();
  return porMock();
}

/* ------------------------------------------------------------------ */
/* Contratos públicos                                                  */
/* ------------------------------------------------------------------ */

/** `GET api/categorias` */
export function listarCategorias() {
  return segunModo(
    () => pedirHttp('/categorias'),
    async () => structuredClone((await cargarCatalogo()).categorias)
  );
}

/**
 * `GET api/productos`
 * @returns {Promise<{items: Array, total: number}>}
 */
export function listarProductos(filtros = {}) {
  return segunModo(
    () => {
      if (fallaForzada) {
        fallaForzada = false;
        return Promise.reject(new ErrorDeRed('Error de red simulado.'));
      }
      return pedirHttp('/productos', filtros);
    },
    async () => {
      if (fallaForzada) {
        fallaForzada = false;
        throw new ErrorDeRed('No pudimos conectarnos con el catálogo.');
      }
      const items = aplicarFiltros((await cargarCatalogo()).productos, filtros);
      return { items: structuredClone(items), total: items.length };
    }
  );
}

/**
 * `GET api/productos/facetas` — conteo por categoría y rango de precios.
 *
 * Va aparte de `listarProductos` a propósito: es una consulta secundaria y no
 * debe consumir la falla simulada ni derribar la vista si se cae.
 */
export function contarPorCategoria(filtros = {}) {
  return segunModo(
    () => pedirHttp('/productos/facetas', filtros),
    async () => {
      const { productos } = await cargarCatalogo();
      const items = aplicarFiltros(productos, { ...filtros, categoria: 'todas' });
      const porCategoria = items.reduce((acc, p) => {
        acc[p.categoria] = (acc[p.categoria] ?? 0) + 1;
        return acc;
      }, {});
      const activos = productos.filter((p) => p.activo).map((p) => p.precio);
      return {
        total: items.length,
        porCategoria,
        precioMin: activos.length ? Math.min(...activos) : 0,
        precioMax: activos.length ? Math.max(...activos) : 0
      };
    }
  );
}

/** `GET api/productos/{id}` */
export function obtenerProducto(id) {
  return segunModo(
    () => pedirHttp(`/productos/${id}`),
    async () => {
      const encontrado = (await cargarCatalogo()).productos.find((p) => p.id === Number(id));
      if (!encontrado) throw new ErrorDeRed('Producto inexistente');
      return structuredClone(encontrado);
    }
  );
}

/** Rango de precios del catálogo, para el filtro deslizante. */
export async function rangoDePrecios() {
  const { precioMin, precioMax } = await contarPorCategoria({});
  return { min: precioMin, max: precioMax };
}

/** Sólo para inspección manual: a qué URL resuelve un recurso de la API. */
export function urlDeApi(ruta) {
  return new URL(ruta.replace(/^\//, ''), BASE_URL).href;
}
