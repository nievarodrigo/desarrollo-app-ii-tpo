import { test } from 'node:test';
import assert from 'node:assert/strict';
import { iniciarCargaCategorias } from '../../main/webapp/assets/js/componentes/catalogo/presentacion/carga-categorias.js';

test('tras un fallo permite reintentar, evita duplicar la carga y recupera el foco', async () => {
  let resolver;
  let intentos = 0;
  let limpiezas = 0;
  let foco = false;
  const estado = { hidden: true };
  const mensaje = {};
  const reintentar = new EventTarget();
  reintentar.ownerDocument = { activeElement: reintentar };
  reintentar.setAttribute = reintentar.removeAttribute = () => {};
  const lista = {
    setAttribute() {}, removeAttribute() {},
    replaceChildren() { limpiezas += 1; },
    querySelector: () => ({ focus() { foco = true; } })
  };
  const cargar = async () => {
    if (++intentos === 1) throw new Error('sin conexión');
    await new Promise((resolve) => { resolver = resolve; });
  };

  await iniciarCargaCategorias({ lista, estado, mensaje, reintentar, cargar });
  assert.equal(estado.hidden, false);
  assert.match(mensaje.textContent, /No pudimos/);
  assert.equal(limpiezas, 1);

  reintentar.dispatchEvent(new Event('click'));
  reintentar.dispatchEvent(new Event('click'));
  assert.equal(intentos, 2);
  assert.match(mensaje.textContent, /Cargando/);
  resolver();
  await new Promise(setImmediate);
  assert.equal(estado.hidden, true);
  assert.equal(foco, true);
});
