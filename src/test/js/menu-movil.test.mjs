import { test } from 'node:test';
import assert from 'node:assert/strict';
import { iniciarMenuMovil } from '../../main/webapp/assets/js/compartido/menu-movil.js';

function montarMenu() {
  const boton = new EventTarget();
  const menu = new EventTarget();
  const atributos = new Map([['aria-expanded', 'false']]);
  let devolucionesDeFoco = 0;
  boton.setAttribute = (nombre, valor) => atributos.set(nombre, valor);
  boton.focus = () => { devolucionesDeFoco += 1; };
  menu.hidden = true;
  iniciarMenuMovil(boton, menu);
  boton.dispatchEvent(new Event('click'));

  return { boton, menu, atributos, foco: () => devolucionesDeFoco };
}

function presionar(nodo, key, yaAtendido = false) {
  const evento = Object.assign(new Event('keydown', { cancelable: true }), { key });
  if (yaAtendido) evento.preventDefault();
  nodo.dispatchEvent(evento);
  return evento;
}

test('Escape desde el menú cierra el desplegable y vuelve al botón', () => {
  const { menu, atributos, foco } = montarMenu();
  assert.equal(atributos.get('aria-expanded'), 'true');
  assert.equal(menu.hidden, false);

  assert.equal(presionar(menu, 'Escape').defaultPrevented, true);
  assert.equal(menu.hidden, true);
  assert.equal(atributos.get('aria-expanded'), 'false');
  assert.equal(foco(), 1);
});

test('Escape también cierra si el foco sigue en el botón, una sola vez', () => {
  const { boton, menu, foco } = montarMenu();
  presionar(boton, 'Escape');
  assert.equal(menu.hidden, true);
  assert.equal(presionar(boton, 'Escape').defaultPrevented, false);
  assert.equal(foco(), 1);
});

test('el menú queda abierto con Tab o si otro control ya atendió Escape', () => {
  const { menu, foco } = montarMenu();
  assert.equal(presionar(menu, 'Tab').defaultPrevented, false);
  presionar(menu, 'Escape', true);
  assert.equal(menu.hidden, false);
  assert.equal(foco(), 0);
});
