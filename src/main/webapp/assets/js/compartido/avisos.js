/** Avisos efímeros con mensaje anunciado y cierre manual. */

const contenedor = document.querySelector('[data-avisos]');
const anuncio = document.querySelector('[data-aviso-anuncio]');
const DURACION = 3200;

export function avisar(mensaje, tono = 'ok') {
  if (!contenedor) return;

  const aviso = document.createElement('div');
  aviso.className = 'aviso';
  aviso.dataset.tono = tono;
  aviso.innerHTML = `
    <span class="aviso__punto" aria-hidden="true"></span>
    <span class="aviso__mensaje"></span>
    <button class="aviso__cerrar" type="button" aria-label="Cerrar notificación">×</button>`;
  const texto = aviso.querySelector('.aviso__mensaje');
  const boton = aviso.querySelector('.aviso__cerrar');
  contenedor.append(aviso);
  texto.textContent = mensaje;
  if (anuncio) anuncio.textContent = mensaje;

  let temporizador;
  let focoAnterior;
  let punteroDentro = false;

  const cerrar = () => {
    if (!aviso.isConnected || aviso.dataset.saliendo === 'true') return;
    aviso.dataset.saliendo = 'true';
    clearTimeout(temporizador);
    if (document.activeElement === boton && focoAnterior?.isConnected) {
      focoAnterior.focus({ preventScroll: true });
    }
    const respaldo = setTimeout(() => aviso.remove(), 600);
    aviso.addEventListener('animationend', () => {
      clearTimeout(respaldo);
      aviso.remove();
    }, { once: true });
  };

  const programarCierre = () => {
    clearTimeout(temporizador);
    if (!punteroDentro && document.activeElement !== boton && aviso.dataset.saliendo !== 'true') {
      temporizador = setTimeout(cerrar, DURACION);
    }
  };
  boton.addEventListener('click', cerrar);
  boton.addEventListener('focus', (evento) => {
    focoAnterior = evento.relatedTarget;
    programarCierre();
  });
  boton.addEventListener('blur', programarCierre);
  aviso.addEventListener('pointerenter', () => { punteroDentro = true; programarCierre(); });
  aviso.addEventListener('pointerleave', () => { punteroDentro = false; programarCierre(); });
  programarCierre();

  /* Nunca dejamos más de tres avisos apilados. */
  if (contenedor.children.length > 3) {
    [...contenedor.children].find((nodo) => !nodo.contains(document.activeElement))?.remove();
  }
}
