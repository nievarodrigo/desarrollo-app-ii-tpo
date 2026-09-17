/** Avisos efímeros (toasts). El contenedor ya es `aria-live="polite"`. */

const contenedor = document.querySelector('[data-avisos]');
const DURACION = 3200;

export function avisar(mensaje, tono = 'ok') {
  if (!contenedor) return;

  const aviso = document.createElement('div');
  aviso.className = 'aviso';
  aviso.dataset.tono = tono;
  aviso.innerHTML = `<span class="aviso__punto" aria-hidden="true"></span><span></span>`;
  aviso.lastElementChild.textContent = mensaje;
  contenedor.append(aviso);

  const cerrar = () => {
    aviso.dataset.saliendo = 'true';
    aviso.addEventListener('animationend', () => aviso.remove(), { once: true });
    setTimeout(() => aviso.remove(), 600);
  };

  const temporizador = setTimeout(cerrar, DURACION);
  aviso.addEventListener('click', () => {
    clearTimeout(temporizador);
    cerrar();
  });

  /* Nunca dejamos más de tres avisos apilados. */
  while (contenedor.children.length > 3) contenedor.firstElementChild.remove();
}
