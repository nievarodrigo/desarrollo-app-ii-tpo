/**
 * Acceso a `localStorage` tolerante a fallos.
 * En modo incógnito o con cookies bloqueadas el acceso puede lanzar, así que
 * nunca dejamos que una preferencia rompa la página.
 */

const PREFIJO = 'tpostore:';

export function leer(clave, porDefecto = null) {
  try {
    const crudo = window.localStorage.getItem(PREFIJO + clave);
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
