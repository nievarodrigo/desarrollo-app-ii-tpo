/**
 * Criterios de búsqueda del catálogo — objeto de valor.
 *
 * Espeja el `record CriteriosDeBusqueda` del backend
 * (`catalogo/service/CriteriosDeBusqueda.java`). La presentación traduce lo que
 * el usuario tocó a esto, y el negocio trabaja con esto. Así la firma de
 * `CatalogoService.buscar()` no crece un parámetro cada vez que agregamos un
 * filtro, y el negocio no sabe nada de inputs, de query params ni del DOM.
 */

export const TODAS = 'todas';

/** Criterios de ordenamiento admitidos. Mismos valores que el enum `Orden` de Java. */
export const ORDENES = ['relevancia', 'precio-asc', 'precio-desc', 'rating', 'nombre'];

/**
 * @typedef {object} Criterios
 * @property {string}      q         texto libre: nombre, marca, SKU o descripción
 * @property {string}      categoria slug de categoría, o `'todas'`
 * @property {string}      orden     uno de `ORDENES`
 * @property {boolean}     soloStock excluir los productos sin unidades
 * @property {number|null} precioMax tope de precio, o `null` si no hay tope
 */

/** Criterios sin ningún filtro aplicado. */
export function sinFiltros() {
  return { q: '', categoria: TODAS, orden: 'relevancia', soloStock: false, precioMax: null };
}

/**
 * Normaliza lo que venga de la UI a un objeto de valor completo y válido.
 * Un `precioMax` no finito (el deslizante en el tope) equivale a "sin tope".
 */
export function criterios(parcial = {}) {
  const base = sinFiltros();
  const precioMax = Number(parcial.precioMax);

  return {
    q: typeof parcial.q === 'string' ? parcial.q : base.q,
    categoria: parcial.categoria || base.categoria,
    orden: ORDENES.includes(parcial.orden) ? parcial.orden : base.orden,
    soloStock: Boolean(parcial.soloStock),
    precioMax: Number.isFinite(precioMax) ? precioMax : null
  };
}

/** Los mismos criterios pero sin restringir la categoría (para las facetas). */
export function sinCategoria(c) {
  return { ...c, categoria: TODAS };
}

export function filtraPorCategoria(c) {
  return Boolean(c.categoria) && c.categoria !== TODAS;
}

export function filtraPorTexto(c) {
  return Boolean(c.q) && c.q.trim().length > 0;
}

/**
 * Criterios listos para viajar como query params a los recursos JAX-RS.
 * Se omiten los que no filtran nada: mandar `precioMax` en el tope o
 * `categoria=todas` sólo ensucia la query.
 */
export function aQueryParams(c) {
  const params = { orden: c.orden };
  if (filtraPorTexto(c)) params.q = c.q.trim();
  if (filtraPorCategoria(c)) params.categoria = c.categoria;
  if (c.soloStock) params.soloStock = 'true';
  if (c.precioMax !== null) params.precioMax = c.precioMax;
  return params;
}
