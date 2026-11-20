import './stub-navegador.mjs';
import { test } from 'node:test';
import assert from 'node:assert/strict';

const panel = { innerHTML: '' };
const caja = { innerHTML: '', addEventListener() {}, focus() {} };
const modal = { dataset: {} };
globalThis.document = {
  querySelector: (selector) => ({
    '[data-cuenta-panel]': panel, '[data-cuenta-caja]': caja, '[data-cuenta-modal]': modal
  })[selector] ?? null,
  querySelectorAll: () => [],
  addEventListener() {},
  body: { classList: { add() {} }, dataset: {} }
};
globalThis.requestAnimationFrame = (fn) => fn();
const { iniciarUsuarioView, abrirAdmin } = await import(
  '../../main/webapp/assets/js/componentes/usuario/presentacion/usuario-view.js');
const { asegurarSemilla, registrar, ingresar } = await import(
  '../../main/webapp/assets/js/componentes/usuario/negocio/usuario-service.js');

await asegurarSemilla();
iniciarUsuarioView();

test('el menú muestra el nombre como texto y no como una etiqueta HTML', async () => {
  await registrar({ nombre: '<b>PRUEBA</b>', email: 'prueba@correo.com', clave: 'clave-larga-1' });
  assert.ok(panel.innerHTML.includes('&lt;b&gt;PRUEBA&lt;/b&gt;'));
  assert.ok(!panel.innerHTML.includes('<b>PRUEBA</b>'));
});

test('el padrón de admin también escapa los nombres registrados', async () => {
  await ingresar('admin@gomarket.com.ar', 'admin123');
  abrirAdmin(null);
  assert.ok(caja.innerHTML.includes('&lt;b&gt;PRUEBA&lt;/b&gt;'));
  assert.ok(!caja.innerHTML.includes('<b>PRUEBA</b>'));
});
