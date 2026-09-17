/**
 * CAPA DE DATOS del componente Catálogo — la fuente de datos.
 *
 * Es el equivalente del `DataSource` que el contenedor le inyecta al backend:
 * no es un repositorio, es de dónde el repositorio saca las filas. Por eso los
 * repositorios (`producto-repository.js`, `categoria-repository.js`) dependen de
 * este módulo y nadie más lo importa.
 *
 * Los datos NO viven acá: viven en `assets/data/catalogo.json`, que es el mismo
 * archivo que lee el backend (`CatalogoEnMemoria` lo toma del classpath, ver el
 * bloque <resources> del pom.xml). Una sola fuente para el WAR y para el
 * despliegue estático: si duplicáramos los productos, se desincronizarían a la
 * primera corrección.
 */

const RUTA = 'assets/data/catalogo.json';

/** @type {Promise<{categorias: Array, productos: Array}>|null} */
let enCurso = null;

/**
 * Devuelve el catálogo completo. La promesa se cachea: por más veces que se
 * llame, el JSON se descarga una sola vez.
 */
export function cargarCatalogo() {
  if (enCurso) return enCurso;

  enCurso = fetch(new URL(RUTA, document.baseURI), { headers: { Accept: 'application/json' } })
    .then((respuesta) => {
      if (!respuesta.ok) throw new Error(`No pudimos leer el catálogo (${respuesta.status})`);
      return respuesta.json();
    })
    .then((datos) => ({
      categorias: datos.categorias ?? [],
      productos: datos.productos ?? []
    }))
    .catch((error) => {
      /* Si falla, limpiamos la caché para que un reintento vuelva a pedirlo. */
      enCurso = null;
      throw error;
    });

  return enCurso;
}
