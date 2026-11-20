/**
 * CAPA DE NEGOCIO del componente Catálogo — implementación remota.
 *
 * Patrón ADAPTER. Cumple el mismo contrato `CatalogoService` que la versión
 * local, pero no aplica ni una sola regla: traduce entre dos mundos y delega.
 *
 *     CriteriosDeBusqueda  →  query params de JAX-RS
 *     DTO JSON del backend →  objeto de dominio del front
 *
 * ¿Por qué Adapter y no "un módulo que hace fetch"? Porque el backend ya tiene
 * el componente Catálogo completo, con SU capa de negocio
 * (`CatalogoServiceImpl`) y SU capa de datos (`CatalogoEnMemoria`). Cuando hay
 * backend, las reglas se ejecutan allá; este archivo es el enchufe que hace que
 * la presentación del front no se entere. Es el caso de "capas lógicas vs.
 * capas físicas" de Clase 3 (slide 24): las tres capas siguen existiendo, sólo
 * que dos de ellas corren en otra máquina.
 *
 * Y por eso la presentación de la tienda es idéntica en los dos destinos: habla
 * con un contrato, no con una implementación.
 *
 * Contratos consumidos (ver README, sección "API"):
 *     GET api/categorias
 *     GET api/productos          ?q &categoria &orden &soloStock &precioMax
 *     GET api/productos/facetas  (mismos params)
 *     GET api/productos/{id}
 */

import { ErrorHttp, obtener as obtenerDeApi } from '../../../compartido/cliente-http.js';
import { aQueryParams } from './criterios-de-busqueda.js';
import { RecursoNoEncontradoError } from './catalogo-service-local.js';

export class CatalogoServiceRemoto {
  /** `GET api/productos` — el backend filtra, ordena y cuenta. */
  async buscar(criterios) {
    const { items, total } = await obtenerDeApi('/productos', aQueryParams(criterios));
    return { items, total };
  }

  /**
   * `GET api/productos/{id}`
   *
   * El 404 del backend se traduce al MISMO error de dominio que lanza la
   * implementación local. Los demás fallos conservan su causa para tratarse
   * como problemas de disponibilidad.
   */
  async obtener(id) {
    try {
      return await obtenerDeApi(`/productos/${id}`);
    } catch (error) {
      if (error instanceof ErrorHttp && error.estado === 404) {
        throw new RecursoNoEncontradoError(`No existe un producto activo con id ${id}`);
      }
      throw error;
    }
  }

  /** `GET api/categorias` */
  listarCategorias() {
    return obtenerDeApi('/categorias');
  }

  /** `GET api/productos/facetas` */
  async contarPorCategoria(criterios) {
    const { total, porCategoria } = await obtenerDeApi('/productos/facetas', aQueryParams(criterios));
    return { total, porCategoria };
  }

  /**
   * El rango sale de las mismas facetas, sin criterios.
   *
   * El backend lo expone dentro de `/productos/facetas` en vez de un recurso
   * aparte: es el mismo recorrido de datos y no justifica otro viaje HTTP.
   */
  async rangoDePrecios() {
    const { precioMin, precioMax } = await obtenerDeApi('/productos/facetas', {});
    return { min: precioMin, max: precioMax };
  }
}
