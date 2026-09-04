/**
 * Manejo compartido de capas modales (carrito y detalle de producto):
 * velo, bloqueo de scroll, cierre con Escape, trampa de foco y devolución
 * del foco al disparador.
 */

const velo = document.querySelector('[data-velo]');
const SELECTOR_FOCO =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/** @type {{panel: HTMLElement, disparador: Element|null, alCerrar: Function}|null} */
let activo = null;

function focoables(panel) {
  return [...panel.querySelectorAll(SELECTOR_FOCO)].filter(
    (el) => el.offsetParent !== null || el === document.activeElement
  );
}

function alPresionarTecla(evento) {
  if (!activo) return;

  if (evento.key === 'Escape') {
    evento.preventDefault();
    cerrarCapa();
    return;
  }

  if (evento.key !== 'Tab') return;

  const lista = focoables(activo.panel);
  if (lista.length === 0) {
    evento.preventDefault();
    return;
  }

  const primero = lista[0];
  const ultimo = lista[lista.length - 1];

  if (evento.shiftKey && document.activeElement === primero) {
    evento.preventDefault();
    ultimo.focus();
  } else if (!evento.shiftKey && document.activeElement === ultimo) {
    evento.preventDefault();
    primero.focus();
  }
}

/**
 * @param {HTMLElement} panel
 * @param {{disparador?: Element|null, focoInicial?: HTMLElement|null, alCerrar?: Function}} opciones
 */
export function abrirCapa(panel, opciones = {}) {
  if (activo) cerrarCapa();

  activo = {
    panel,
    disparador: opciones.disparador ?? document.activeElement,
    alCerrar: opciones.alCerrar ?? (() => {})
  };

  panel.hidden = false;
  if (velo) velo.hidden = false;
  document.body.classList.add('sin-scroll');
  /* Marca qué capa está abierta para que los avisos no tapen su CTA. */
  document.body.dataset.capa = panel.id || 'capa';

  const objetivo = opciones.focoInicial ?? focoables(panel)[0] ?? panel;
  requestAnimationFrame(() => objetivo.focus({ preventScroll: true }));

  document.addEventListener('keydown', alPresionarTecla);
}

export function cerrarCapa() {
  if (!activo) return;

  const { panel, disparador, alCerrar } = activo;
  activo = null;

  panel.hidden = true;
  if (velo) velo.hidden = true;
  document.body.classList.remove('sin-scroll');
  delete document.body.dataset.capa;
  document.removeEventListener('keydown', alPresionarTecla);

  alCerrar();

  if (disparador instanceof HTMLElement && document.contains(disparador)) {
    disparador.focus({ preventScroll: true });
  }
}

export function capaAbierta(panel) {
  return activo?.panel === panel;
}

velo?.addEventListener('click', cerrarCapa);
