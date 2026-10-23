/**
 * Mínimo indispensable del navegador para poder probar en Node los componentes
 * que tienen persistencia.
 *
 * Sólo se necesita para Carrito y Usuario, y el motivo es exactamente el que
 * queremos demostrar: son los dos componentes con ESTADO. `CatalogoServiceLocal`
 * no necesita nada de esto porque es stateless y recibe todo por constructor —
 * mirá `catalogo-service-local.test.mjs`, que no importa este archivo.
 *
 * Reemplaza sólo el medio de persistencia (`localStorage`), no la lógica. El
 * `crypto.subtle` que usa el hash de contraseñas ya viene en Node 18+, así que
 * el hasheo que se prueba es el REAL, no un doble.
 *
 * Importalo SIEMPRE como primer import del archivo de test: los módulos ESM se
 * evalúan en el orden en que se declaran, y los servicios leen su estado
 * persistido al ser importados.
 */

const almacen = new Map();

globalThis.localStorage = {
  getItem: (clave) => (almacen.has(clave) ? almacen.get(clave) : null),
  setItem: (clave, valor) => almacen.set(clave, String(valor)),
  removeItem: (clave) => almacen.delete(clave),
  clear: () => almacen.clear()
};

globalThis.window = globalThis;

/** Para poder inspeccionar qué quedó realmente guardado. */
export function crudoDe(clave) {
  return almacen.get(`gomarket:${clave}`) ?? null;
}

export function vaciarAlmacen() {
  almacen.clear();
}

/**
 * Escribe una clave TAL CUAL, con su prefijo completo, sin pasar por la capa de
 * almacenamiento. Sirve para simular un navegador que viene de una versión
 * anterior de la app y todavía tiene datos con el prefijo viejo.
 */
export function sembrarCrudo(claveCompleta, valor) {
  almacen.set(claveCompleta, JSON.stringify(valor));
}
