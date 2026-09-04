/**
 * Selector de tema: `normal` (identidad de la marca, papel cálido),
 * `claro` (alto brillo) y `oscuro`. La preferencia se persiste en
 * localStorage y se aplica antes del primer render para evitar el parpadeo.
 */

import { leer, escribir } from './storage.js';

export const TEMAS = [
  { id: 'normal', nombre: 'Normal', descripcion: 'Papel cálido, el look de la marca' },
  { id: 'claro', nombre: 'Claro', descripcion: 'Máximo brillo y contraste' },
  { id: 'oscuro', nombre: 'Oscuro', descripcion: 'Cómodo de noche' }
];

const CLAVE = 'tema';
const TEMA_POR_DEFECTO = 'normal';

const oyentes = new Set();

export function temaActual() {
  const guardado = leer(CLAVE, null);
  return TEMAS.some((t) => t.id === guardado) ? guardado : TEMA_POR_DEFECTO;
}

export function aplicarTema(id) {
  const elegido = TEMAS.some((t) => t.id === id) ? id : TEMA_POR_DEFECTO;
  document.documentElement.dataset.tema = elegido;

  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) {
    const fondo = getComputedStyle(document.documentElement).getPropertyValue('--fondo').trim();
    if (fondo) meta.setAttribute('content', fondo);
  }

  escribir(CLAVE, elegido);
  oyentes.forEach((fn) => fn(elegido));
  return elegido;
}

export function alCambiarTema(fn) {
  oyentes.add(fn);
  return () => oyentes.delete(fn);
}

export function iniciarTema() {
  return aplicarTema(temaActual());
}
