/**
 * Acceso a `localStorage` tolerante a fallos.
 * En modo incógnito o con cookies bloqueadas el acceso puede lanzar, así que
 * nunca dejamos que una preferencia rompa la página.
 */

const PREFIJO = 'gomarket:';

/* Nombres anteriores de la marca, del más reciente al más viejo. Se migran al
   vuelo para que quien ya visitó la tienda no pierda el carrito ni el tema al
   cambiar el nombre. Se pueden borrar dentro de unos meses. */
const PREFIJOS_ANTERIORES = ['fachada:', 'tpostore:'];

export function leer(clave, porDefecto = null) {
  try {
    let crudo = window.localStorage.getItem(PREFIJO + clave);

    if (crudo === null) {
      for (const anterior of PREFIJOS_ANTERIORES) {
        const heredado = window.localStorage.getItem(anterior + clave);
        if (heredado === null) continue;
        window.localStorage.setItem(PREFIJO + clave, heredado);
        window.localStorage.removeItem(anterior + clave);
        crudo = heredado;
        break;
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
