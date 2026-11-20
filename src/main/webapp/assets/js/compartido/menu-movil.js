/** Controla el desplegable de navegación y su foco de teclado. */
export function iniciarMenuMovil(boton, menu) {
  if (!boton || !menu) return;

  function cambiarApertura(abierto) {
    menu.hidden = !abierto;
    boton.setAttribute('aria-expanded', String(abierto));
  }

  function cerrarConEscape(evento) {
    if (evento.key !== 'Escape' || evento.defaultPrevented || menu.hidden) return;
    evento.preventDefault();
    cambiarApertura(false);
    boton.focus({ preventScroll: true });
  }

  boton.addEventListener('click', () => cambiarApertura(menu.hidden));
  boton.addEventListener('keydown', cerrarConEscape);
  menu.addEventListener('keydown', cerrarConEscape);
  menu.addEventListener('click', (evento) => {
    if (evento.target.closest('a, [data-cuenta-accion]')) cambiarApertura(false);
  });
}
