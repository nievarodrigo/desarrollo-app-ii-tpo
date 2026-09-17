/**
 * CAPA DE DATOS del componente Catálogo — repositorio de categorías.
 *
 * Espeja `catalogo/repository/CategoriaRepository.java`. Va separado del
 * repositorio de productos por la misma razón que en el backend: son dos
 * agregados distintos y el día que las categorías salgan de otra tabla (o de
 * otro sistema) sólo cambia esta clase.
 */

import { cargarCatalogo } from './catalogo-json.js';

export class CategoriaRepositoryEnMemoria {
  async buscarTodas() {
    const { categorias } = await cargarCatalogo();
    return structuredClone(categorias);
  }
}
