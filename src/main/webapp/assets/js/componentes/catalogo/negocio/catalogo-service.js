/**
 * COMPONENTE CATÁLOGO — contrato público y fábrica.
 *
 * Patrón FACADE. Es la única puerta de entrada al componente desde afuera:
 * la presentación importa ESTE módulo y nada más. No conoce los repositorios,
 * no sabe si hay backend, no sabe qué implementación quedó activa. Palabras del
 * `CatalogoService.java`: "la capa de presentación depende de esta interfaz y no
 * conoce ni el repositorio ni la base de datos".
 *
 * ── Contrato ────────────────────────────────────────────────────────────────
 *   buscar(criterios)            → { items, total }   filtrados y ordenados
 *   obtener(id)                  → Producto           lanza RecursoNoEncontradoError
 *   listarCategorias()           → Categoria[]
 *   contarPorCategoria(criterios)→ { total, porCategoria }
 *   rangoDePrecios()             → { min, max }
 *
 * ── Patrón FACTORY ─────────────────────────────────────────────────────────
 * `implementacion()` decide, UNA sola vez y en tiempo de arranque, cuál de las
 * dos implementaciones del contrato se usa:
 *
 *   hay backend  → CatalogoServiceRemoto  (Adapter: delega al Jakarta EE)
 *   no hay       → CatalogoServiceLocal   (reglas propias + repositorio en memoria)
 *
 * Quien llama nunca instancia una implementación concreta. Es exactamente lo
 * que hace `@Inject` del otro lado: pedir el contrato y dejar que otro resuelva
 * quién lo cumple.
 *
 * ── STATELESS ───────────────────────────────────────────────────────────────
 * Ninguna de las dos implementaciones guarda estado conversacional. La única
 * cosa que se memoriza es QUIÉN atiende (la decisión de la fábrica), que es
 * configuración de arranque, no estado de un cliente. Dos búsquedas iguales dan
 * el mismo resultado sin importar el orden ni quién las pidió.
 */

import { hayBackend } from '../../../compartido/cliente-http.js';
import { criterios } from './criterios-de-busqueda.js';
import { ProductoRepositoryEnMemoria } from '../datos/producto-repository.js';
import { CategoriaRepositoryEnMemoria } from '../datos/categoria-repository.js';
import { CatalogoServiceLocal, RecursoNoEncontradoError } from './catalogo-service-local.js';
import { CatalogoServiceRemoto } from './catalogo-service-remoto.js';

export { RecursoNoEncontradoError };

/** @type {Promise<object>|null} */
let elegida = null;

/** 'remoto' | 'local' | null mientras la fábrica todavía no resolvió. */
let modo = null;

/** Qué implementación quedó activa. La UI lo muestra como insignia de origen. */
export function modoDeDatos() {
  return modo;
}

/** FACTORY: resuelve e instancia la implementación del contrato, una sola vez. */
function implementacion() {
  if (elegida) return elegida;

  elegida = hayBackend().then((disponible) => {
    modo = disponible ? 'remoto' : 'local';
    return disponible
      ? new CatalogoServiceRemoto()
      : new CatalogoServiceLocal(new ProductoRepositoryEnMemoria(), new CategoriaRepositoryEnMemoria());
  });

  return elegida;
}

/* ------------------------------------------------------------------ */
/* Falla simulada — sólo para la demo                                 */
/* ------------------------------------------------------------------ */

/**
 * Hace fallar la PRÓXIMA búsqueda, una sola vez, para poder mostrar en vivo el
 * estado de error de la grilla sin desenchufar nada. Es una costura de prueba
 * del componente, no una regla de negocio: por eso vive en la fachada y no
 * dentro de ninguna implementación.
 */
let fallaForzada = false;

export function forzarProximoError(valor = true) {
  fallaForzada = valor;
}

function consumirFalla() {
  if (!fallaForzada) return false;
  fallaForzada = false;
  return true;
}

/* ------------------------------------------------------------------ */
/* Contrato público                                                   */
/* ------------------------------------------------------------------ */

/**
 * Productos que cumplen los criterios, ya filtrados y ordenados.
 * @param {object} filtros criterios parciales tal como los tiene la UI
 */
export async function buscar(filtros = {}) {
  if (consumirFalla()) throw new Error('No pudimos conectarnos con el catálogo.');
  return (await implementacion()).buscar(criterios(filtros));
}

/** @throws {RecursoNoEncontradoError} si no existe o está inactivo */
export async function obtener(id) {
  return (await implementacion()).obtener(id);
}

export async function listarCategorias() {
  return (await implementacion()).listarCategorias();
}

/**
 * Facetas por categoría.
 *
 * Va aparte de `buscar()` a propósito: es una consulta secundaria y no debe
 * consumir la falla simulada ni derribar la vista si se cae.
 */
export async function contarPorCategoria(filtros = {}) {
  return (await implementacion()).contarPorCategoria(criterios(filtros));
}

export async function rangoDePrecios() {
  return (await implementacion()).rangoDePrecios();
}
