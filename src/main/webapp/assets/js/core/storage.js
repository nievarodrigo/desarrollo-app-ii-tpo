/**
 * Acceso a `localStorage` tolerante a fallos.
 * En modo incógnito o con cookies bloqueadas el acceso puede lanzar, así que
 * nunca dejamos que una preferencia rompa la página.
 */

const PREFIJO = 'fachada:';

/* Marca anterior. Se migra al vuelo para que quien ya visitó la tienda no pierda
   el carrito ni el tema al cambiar el nombre. Se puede borrar dentro de unos meses. */
const PREFIJO_ANTERIOR = 'tpostore:';

export function leer(clave, porDefecto = null) {
  try {
    let crudo = window.localStorage.getItem(PREFIJO + clave);

    if (crudo === null) {
      const heredado = window.localStorage.getItem(PREFIJO_ANTERIOR + clave);
      if (heredado !== null) {
        window.localStorage.setItem(PREFIJO + clave, heredado);
        window.localStorage.removeItem(PREFIJO_ANTERIOR + clave);
        crudo = heredado;
      }
    }

    return crudo === null ? porDefecto : JSON.parse(crudo);
  } catch {
    return porDefecto;
  }
}

export function escribir(clave, valor) {
  try {
    window.localStorage.setItem(PREFIJO + clave, JSON.stringify(valor));
    return true;
  } catch {
    return false;
  }
}

export function borrar(clave) {
  try {
    window.localStorage.removeItem(PREFIJO + clave);
  } catch {
    /* sin storage disponible: no hay nada que limpiar */
  }
}
