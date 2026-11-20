import { test } from 'node:test';
import assert from 'node:assert/strict';
import { crearProgresoBusquedaIA } from '../../main/webapp/assets/js/componentes/catalogo/presentacion/progreso-busqueda-ia.js';

test('el progreso dura hasta que el gateway devuelve los criterios', async () => {
  const cambios = [];
  let resolver;
  const gateway = (consulta) => new Promise((resolve) => {
    assert.equal(consulta, 'auriculares con stock');
    resolver = resolve;
  });
  const busqueda = crearProgresoBusquedaIA(gateway, (estado) => cambios.push(estado));
  const pendiente = busqueda.interpretar('auriculares con stock');
  assert.equal(busqueda.ocupado, true);
  assert.deepEqual(cambios, [true]);

  const respuesta = { estado: 'sugerencia', criterios: { soloStock: true } };
  resolver(respuesta);
  assert.deepEqual(await pendiente, respuesta);
  assert.equal(busqueda.ocupado, false);
  assert.deepEqual(cambios, [true, false]);
});

test('si falla el gateway se limpia el progreso y se puede volver a intentar', async () => {
  const cambios = [];
  const fallo = new Error('Sin conexión');
  let intentos = 0;
  const busqueda = crearProgresoBusquedaIA(async () => {
    if (++intentos === 1) throw fallo;
    return { estado: 'manual' };
  }, (estado) => cambios.push(estado));

  await assert.rejects(busqueda.interpretar('teclado compacto'), fallo);
  assert.equal(busqueda.ocupado, false);
  assert.deepEqual(await busqueda.interpretar('teclado compacto'), { estado: 'manual' });
  assert.deepEqual(cambios, [true, false, true, false]);
});
