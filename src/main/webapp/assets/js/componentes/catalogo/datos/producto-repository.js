/**
 * CAPA DE DATOS del componente Catálogo — patrón Repository / DAO.
 *
 * Espeja `catalogo/repository/ProductoRepository.java`. El contrato es
 * deliberadamente pobre:
 *
 *     buscarTodos() → Producto[]      todos, SIN filtrar
 *     buscarPorId(id) → Producto|null
 *
 * El filtrado, el orden y la visibilidad NO viven acá: son decisiones del
 * negocio. Un repositorio que filtra es un repositorio que sabe demasiado de la
 * capa de arriba — el anti-patrón que vimos en Clase 3 (slide 36).
 *
 * Hoy lo cumple `ProductoRepositoryEnMemoria`, que lee el JSON semilla.
 * Cuando el front consuma la API, el negocio se apoya en
 * `catalogo-service-remoto.js` y este repositorio no se toca.
 */

import { cargarCatalogo } from './catalogo-json.js';

/**
 * Latencia simulada.
 *
 * Vive en la capa de DATOS, no en la de negocio, porque es lo que tarda la E/S
 * y la E/S es responsabilidad de esta capa. La primera lectura tiene la demora
 * real del `fetch`; de ahí en adelante el JSON queda cacheado y sin esta
 * demora los esqueletos de carga de la UI aparecerían y desaparecerían en el
 * mismo frame.
 *
 * Que esté acá y no en `CatalogoServiceLocal` tiene una consecuencia concreta:
 * los tests del negocio le inyectan un repositorio falso y corren instantáneos.
 */
const LATENCIA_MS = [220, 480];

function demorar() {
  const [min, max] = LATENCIA_MS;
  return new Promise((resolve) => setTimeout(resolve, min + Math.random() * (max - min)));
}

/**
 * Implementación en memoria sobre `assets/data/catalogo.json`.
 * Equivale a `CatalogoEnMemoria.java`, que lee el MISMO archivo del classpath.
 */
export class ProductoRepositoryEnMemoria {
  /** Todos los productos, sin filtrar. El filtrado es decisión del negocio. */
  async buscarTodos() {
    const { productos } = await cargarCatalogo();
    await demorar();
    return structuredClone(productos);
  }

  /** @returns {Promise<object|null>} */
  async buscarPorId(id) {
    const { productos } = await cargarCatalogo();
    await demorar();
    const encontrado = productos.find((p) => p.id === Number(id));
    return encontrado ? structuredClone(encontrado) : null;
  }
}
