import { test } from 'node:test';
import assert from 'node:assert/strict';

test('el aviso anuncia texto, se mantiene con foco y se puede cerrar con el botón', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const contenedor = { children: [], append(nodo) { this.children.push(nodo); nodo.isConnected = true; } };
  const anuncio = {};
  const aviso = new EventTarget();
  const mensaje = {};
  const boton = new EventTarget();
  const anterior = { isConnected: true, focus() { document.activeElement = this; } };
  aviso.dataset = {};
  aviso.querySelector = (selector) => (selector === '.aviso__cerrar' ? boton : mensaje);
  aviso.remove = () => { contenedor.children = []; aviso.isConnected = false; };
  globalThis.document = {
    activeElement: anterior,
    querySelector: (selector) => (selector === '[data-avisos]' ? contenedor : anuncio),
    createElement: () => aviso
  };
  const { avisar } = await import('../../main/webapp/assets/js/compartido/avisos.js');
  avisar('Producto agregado');
  assert.equal(anuncio.textContent, 'Producto agregado');
  assert.equal(mensaje.textContent, 'Producto agregado');

  document.activeElement = boton;
  boton.dispatchEvent(Object.assign(new Event('focus'), { relatedTarget: anterior }));
  t.mock.timers.tick(4000);
  assert.equal(aviso.dataset.saliendo, undefined);

  boton.dispatchEvent(new Event('click'));
  t.mock.timers.tick(600);
  assert.equal(aviso.isConnected, false);
  assert.equal(document.activeElement, anterior);
});
