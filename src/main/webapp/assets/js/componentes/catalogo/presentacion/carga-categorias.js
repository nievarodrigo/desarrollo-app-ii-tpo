/** Actualiza el estado de la carga y admite un solo reintento a la vez. */
export function iniciarCargaCategorias({ lista, estado, mensaje, reintentar, cargar }) {
  let enCurso = false;

  async function intentar(desdeBoton = false) {
    if (enCurso) return;
    enCurso = true;
    lista.setAttribute('aria-busy', 'true');
    if (desdeBoton) {
      mensaje.textContent = 'Cargando categorías…';
      reintentar.setAttribute('aria-disabled', 'true');
    }

    try {
      await cargar();
      if (desdeBoton && reintentar === reintentar.ownerDocument.activeElement) {
        (lista.querySelector('[data-categoria]') ?? lista).focus({ preventScroll: true });
      }
      estado.hidden = true;
    } catch {
      lista.replaceChildren();
      estado.hidden = false;
      mensaje.textContent = 'No pudimos cargar las categorías.';
    } finally {
      lista.removeAttribute('aria-busy');
      reintentar.removeAttribute('aria-disabled');
      enCurso = false;
    }
  }

  reintentar.addEventListener('click', () => intentar(true));
  return intentar();
}
