/**
 * Transporte HTTP hacia la API del backend. Infraestructura, no negocio.
 *
 * Este módulo no sabe qué es un producto ni qué es un carrito: sólo sabe armar
 * una URL, mandar un GET y traducir una respuesta que no es 2xx en un error de
 * dominio del front. Cualquier componente que necesite hablar con la API lo usa,
 * y ninguno duplica la lógica de la URL base.
 *
 * Equivale, del lado del cliente, a lo que el contenedor Jakarta EE le provee al
 * backend: un servicio técnico transversal que los componentes consumen sin
 * implementarlo.
 */

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

export class ErrorDeRed extends Error {
  constructor(mensaje) {
    super(mensaje);
    this.name = 'ErrorDeRed';
  }
}

/**
 * GET sobre la API.
 * @param {string} ruta ruta relativa al recurso, con o sin barra inicial
 * @param {Record<string, unknown>} params query params; se omiten los vacíos
 */
export async function obtener(ruta, params = {}) {
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

/* ------------------------------------------------------------------ */
/* Detección del backend                                              */
/* ------------------------------------------------------------------ */

/** @type {Promise<boolean>|null} */
let sondeo = null;

/**
 * ¿Hay un backend Jakarta EE atendiendo, o estamos en el despliegue estático?
 *
 * Se resuelve UNA sola vez consultando `GET api/salud`:
 *   · WAR en WildFly → responde `SaludResource` con `disponible: true`.
 *   · Sitio estático → un rewrite de `vercel.json` devuelve `disponible: false`.
 *
 * Se hace así, y no provocando un 404 a propósito ni mirando el hostname, para
 * que ninguno de los dos destinos ensucie la consola con errores de red.
 */
export function hayBackend() {
  if (sondeo) return sondeo;

  sondeo = fetch(new URL('salud', BASE_URL), { headers: { Accept: 'application/json' } })
    .then((r) => (r.ok ? r.json() : { disponible: false }))
    .then((estado) => estado?.disponible === true)
    .catch(() => false);

  return sondeo;
}

/** Sólo para inspección manual: a qué URL resuelve un recurso de la API. */
export function urlDeApi(ruta) {
  return new URL(ruta.replace(/^\//, ''), BASE_URL).href;
}
