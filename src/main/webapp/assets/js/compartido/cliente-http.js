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
 * Base de la API, derivada del path del documento (se calcula al usarla).
 *
 * NO se fija en '/api' a propósito: el WAR se despliega bajo un context root
 * (`ecommerce.war` → `/ecommerce/`) y ahí los recursos JAX-RS viven en
 * `/ecommerce/api/v1/...`. Con base relativa la misma UI funciona servida desde la
 * raíz o bajo cualquier context root.
 *
 * Sin secretos ni credenciales: es sólo una ruta.
 */
const baseUrl = () => new URL('api/v1/', document.baseURI);

/* ------------------------------------------------------------------ */
/* Token del backend                                                  */
/* ------------------------------------------------------------------ */

const CLAVE_TOKEN = 'gomarket.tokenApi';
let versionSesion = 0;

/** JWT que emite `POST api/auth/login`. Vive en sessionStorage: se borra al cerrar la pestaña. */
let tokenApi = (() => {
  try { return globalThis.sessionStorage?.getItem(CLAVE_TOKEN) ?? null; } catch { return null; }
})();

/** Guarda (o borra, con `null`) el token que viaja como `Authorization: Bearer`. */
export function usarToken(token) {
  tokenApi = token || null;
  versionSesion += 1;
  try {
    if (tokenApi) globalThis.sessionStorage?.setItem(CLAVE_TOKEN, tokenApi);
    else globalThis.sessionStorage?.removeItem(CLAVE_TOKEN);
  } catch { /* almacenamiento bloqueado: el token queda sólo en memoria */ }
}

function autorizacion() {
  return tokenApi ? { Authorization: `Bearer ${tokenApi}` } : {};
}

/**
 * Pide un JWT al backend con las mismas credenciales del ingreso local. Si no hay
 * backend o la cuenta no existe allá, no es un error: la sesión local sigue andando
 * y las operaciones protegidas del backend responderán 401.
 * @returns {Promise<boolean>} si se obtuvo un token
 */
export async function ingresarEnBackend(email, clave) {
  if (!(await hayBackend())) return false;
  try {
    const respuesta = await fetch(new URL('auth/login', baseUrl()), {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, clave })
    });
    if (!respuesta.ok) { usarToken(null); return false; }
    usarToken((await respuesta.json()).token);
    return true;
  } catch {
    return false;
  }
}

export class ErrorDeRed extends Error {
  constructor(mensaje, causa) {
    super(mensaje, { cause: causa });
    this.name = 'ErrorDeRed';
  }
}

export class ErrorHttp extends Error {
  constructor(estado, mensaje = `La API respondió ${estado}`) {
    super(mensaje);
    this.name = 'ErrorHttp';
    this.estado = estado;
  }
}

export class SesionVencidaError extends ErrorHttp {
  constructor() {
    super(401, 'Tu sesión en el servidor venció o no está activa. Ingresá de nuevo para continuar.');
    this.name = 'SesionVencidaError';
  }
}

async function enviar(url, opciones) {
  try {
    return await fetch(url, opciones);
  } catch (causa) {
    throw new ErrorDeRed('No pudimos conectarnos con el servidor. Intentá de nuevo.', causa);
  }
}

async function solicitar(url, opciones, { requiereSesion = false, leerDetalle = false } = {}) {
  const versionAlEnviar = versionSesion;
  const respuesta = await enviar(url, opciones);
  if (respuesta.status === 401 && requiereSesion) {
    // Una respuesta atrasada no invalida un token nuevo ni vuelve a abrir el ingreso.
    if (versionSesion === versionAlEnviar) {
      usarToken(null);
      globalThis.dispatchEvent?.(new CustomEvent('gm:sesion-vencida'));
    }
    throw new SesionVencidaError();
  }
  if (!respuesta.ok) {
    let detalle;
    if (leerDetalle) {
      try {
        const problema = await respuesta.json();
        detalle = problema.detail || problema.error;
      } catch { /* respuesta sin JSON */ }
    }
    throw new ErrorHttp(respuesta.status, detalle || undefined);
  }
  return respuesta.json();
}

/**
 * GET sobre la API.
 * @param {string} ruta ruta relativa al recurso, con o sin barra inicial
 * @param {Record<string, unknown>} params query params; se omiten los vacíos
 * @param {{requiereSesion?: boolean}} opciones notifica el 401 de un recurso protegido
 */
export async function obtener(ruta, params = {}, opciones = {}) {
  /* La barra inicial se quita para que `ruta` se resuelva contra la base de la API y no
     contra la raíz del host. */
  const url = new URL(ruta.replace(/^\//, ''), baseUrl());
  Object.entries(params).forEach(([clave, valor]) => {
    if (valor !== null && valor !== undefined && valor !== '') url.searchParams.set(clave, valor);
  });

  return solicitar(url, { headers: { Accept: 'application/json', ...autorizacion() } }, opciones);
}

/**
 * POST JSON hacia la API. Una respuesta 202 indica aceptación, no procesamiento final.
 * @param {Record<string, string>} cabeceras extra, p. ej. `Idempotency-Key` para que un
 *   reintento no duplique la operación en el servidor.
 * @param {{requiereSesion?: boolean}} opciones notifica el 401 de un recurso protegido
 */
export async function publicar(ruta, cuerpo, cabeceras = {}, opciones = {}) {
  const url = new URL(ruta.replace(/^\//, ''), baseUrl());
  return solicitar(url, {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...autorizacion(), ...cabeceras },
    body: JSON.stringify(cuerpo)
  }, { ...opciones, leerDetalle: true });
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

  sondeo = fetch(new URL('salud', baseUrl()), { headers: { Accept: 'application/json' } })
    .then((r) => (r.ok ? r.json() : { disponible: false }))
    .then((estado) => estado?.disponible === true)
    .catch(() => false);

  return sondeo;
}

/** Sólo para inspección manual: a qué URL resuelve un recurso de la API. */
export function urlDeApi(ruta) {
  return new URL(ruta.replace(/^\//, ''), baseUrl()).href;
}
