/**
 * CAPA DE DATOS del componente Carrito — patrón Repository / DAO.
 *
 * Persiste y recupera los ítems del carrito. No valida stock, no calcula
 * totales, no decide nada: guarda lo que le dan y devuelve lo que guardó. Eso
 * es todo lo que tiene que saber una capa de datos (Clase 3, slide 18).
 *
 * Hoy el medio es `localStorage`, que hace que el carrito sobreviva al refresco
 * y al cierre del navegador. Cuando exista `POST /api/carritos/{id}` en el
 * backend, se agrega un `CarritoRepositoryRest` que cumple este MISMO contrato
 * y `CarritoService` no se toca ni una línea — igual que pasa con
 * `ProductoRepository` del otro lado.
 *
 *     obtener()       → Item[]
 *     guardar(items)  → boolean   (false si el medio no está disponible)
 *     limpiar()       → void
 *
 * @typedef {object} Item
 * @property {number} id
 * @property {string} sku
 * @property {string} nombre
 * @property {string} marca
 * @property {number} precio
 * @property {string} arte
 * @property {number} stock
 * @property {number} cantidad
 */

import { leer, escribir, borrar } from '../../../compartido/almacenamiento.js';

const CLAVE = 'carrito';

export class CarritoRepositoryLocal {
  /**
   * Ítems persistidos, saneados.
   *
   * El saneamiento es responsabilidad del repositorio porque el medio es
   * editable por el usuario: cualquiera puede abrir el devtools y escribir
   * basura en `localStorage`. Lo que sale de acá tiene que tener la FORMA
   * correcta; que además cumpla las reglas del negocio lo decide el service.
   */
  obtener() {
    const crudo = leer(CLAVE, []);
    if (!Array.isArray(crudo)) return [];

    return crudo
      .filter((it) => it && typeof it.id === 'number' && typeof it.precio === 'number')
      .map((it) => ({ ...it, cantidad: Number(it.cantidad) || 1 }));
  }

  /** @returns {boolean} `false` si no hay medio de persistencia disponible. */
  guardar(items) {
    return escribir(CLAVE, items);
  }

  limpiar() {
    borrar(CLAVE);
  }
}
