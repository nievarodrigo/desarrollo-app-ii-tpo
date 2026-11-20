import { test } from 'node:test';
import assert from 'node:assert/strict';
import { CatalogoServiceRemoto } from '../../main/webapp/assets/js/componentes/catalogo/negocio/catalogo-service-remoto.js';
import { RecursoNoEncontradoError } from '../../main/webapp/assets/js/componentes/catalogo/negocio/catalogo-service-local.js';
import { ErrorDeRed, ErrorHttp } from '../../main/webapp/assets/js/compartido/cliente-http.js';

globalThis.document = { baseURI: 'https://gomarket.example/ecommerce/' };
const catalogo = new CatalogoServiceRemoto();

test('el adaptador sólo convierte un HTTP 404 en producto no encontrado', async (t) => {
  const fetch = t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 404 }));
  await assert.rejects(catalogo.obtener(12), RecursoNoEncontradoError);
  assert.equal(fetch.mock.calls[0].arguments[0].pathname, '/ecommerce/api/v1/productos/12');
});

test('una falla de conexión mantiene su causa y permite distinguirla de un 404', async (t) => {
  const causa = new TypeError('Failed to fetch');
  t.mock.method(globalThis, 'fetch', async () => { throw causa; });
  await assert.rejects(catalogo.obtener(12), (error) => error instanceof ErrorDeRed && error.cause === causa);
});

test('un HTTP 503 sigue siendo un error del servidor', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 503 }));
  await assert.rejects(catalogo.obtener(12), (error) => error instanceof ErrorHttp && error.estado === 503);
});
