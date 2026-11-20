import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ErrorHttp, SesionVencidaError, ingresarEnBackend, obtener, publicar, usarToken } from '../../main/webapp/assets/js/compartido/cliente-http.js';

globalThis.document = { baseURI: 'https://gomarket.example/ecommerce/' };
const guardado = new Map();
globalThis.sessionStorage = {
  getItem: (clave) => guardado.get(clave) ?? null,
  setItem: (clave, valor) => guardado.set(clave, valor),
  removeItem: (clave) => guardado.delete(clave)
};
const protegida = { requiereSesion: true };
const respuesta = (status) => new Response(null, { status });

function escucharSesion(t) {
  const eventos = [];
  t.mock.method(globalThis, 'dispatchEvent', (evento) => eventos.push(evento.type));
  return eventos;
}
globalThis.dispatchEvent = () => {};

test('GET y POST protegidos aplican el mismo manejo del 401', async (t) => {
  const eventos = escucharSesion(t);
  const fetch = t.mock.method(globalThis, 'fetch', async () => respuesta(401));
  usarToken('token-lectura');
  await assert.rejects(obtener('pedidos/123', {}, protegida), SesionVencidaError);
  assert.equal(guardado.size, 0);

  usarToken('token-compra');
  await assert.rejects(publicar('pedidos', { items: [] }, { 'Idempotency-Key': 'compra-1' }, protegida), SesionVencidaError);
  assert.equal(guardado.size, 0);
  assert.deepEqual(eventos, ['gm:sesion-vencida', 'gm:sesion-vencida']);
  const opciones = fetch.mock.calls[1].arguments[1];
  assert.equal(opciones.headers.Authorization, 'Bearer token-compra');
  assert.equal(opciones.headers['Idempotency-Key'], 'compra-1');
  assert.equal(fetch.mock.callCount(), 2);
});

test('dos rechazos de la misma sesión anuncian el vencimiento una sola vez', async (t) => {
  const eventos = escucharSesion(t);
  t.mock.method(globalThis, 'fetch', async () => respuesta(401));
  usarToken('token-expirado');

  const r = await Promise.allSettled([
    obtener('pedidos/123', {}, protegida),
    obtener('pedidos/456', {}, protegida)
  ]);
  assert.ok(r.every((p) => p.reason instanceof SesionVencidaError));
  assert.deepEqual(eventos, ['gm:sesion-vencida']);
});

test('un 401 atrasado no borra un token nuevo', async (t) => {
  const eventos = escucharSesion(t);
  let resolver;
  t.mock.method(globalThis, 'fetch', () => new Promise((resolve) => { resolver = resolve; }));
  usarToken('anterior');
  const pendiente = obtener('pedidos/123', {}, protegida);

  usarToken('nuevo');
  resolver(respuesta(401));
  await assert.rejects(pendiente, SesionVencidaError);
  assert.deepEqual(eventos, []);
  assert.equal(guardado.get('gomarket.tokenApi'), 'nuevo');
});

test('un 403 protegido y un 401 público no cierran la sesión', async (t) => {
  const eventos = escucharSesion(t);
  t.mock.method(globalThis, 'fetch', async (url) => respuesta(url.pathname.endsWith('/interpretar') ? 401 : 403));
  usarToken('vigente');

  await assert.rejects(obtener('pedidos/123', {}, protegida), (error) => error instanceof ErrorHttp && error.estado === 403);
  await assert.rejects(publicar('ia/interpretar', { consulta: 'auriculares' }), (error) =>
    error instanceof ErrorHttp && !(error instanceof SesionVencidaError) && error.estado === 401);
  assert.deepEqual(eventos, []);
  assert.equal(guardado.get('gomarket.tokenApi'), 'vigente');
});

test('una credencial rechazada en el ingreso no emite sesión vencida', async (t) => {
  const eventos = escucharSesion(t);
  t.mock.method(globalThis, 'fetch', async (url) => url.pathname.endsWith('/salud')
    ? Response.json({ disponible: true })
    : respuesta(401));
  usarToken(null);
  assert.equal(await ingresarEnBackend('prueba@example.test', 'clave-de-prueba'), false);
  assert.deepEqual(eventos, []);
});
