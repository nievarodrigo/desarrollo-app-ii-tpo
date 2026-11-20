/** Mantiene una única interpretación activa y limpia el indicador al terminar. */
export function crearProgresoBusquedaIA(interpretar, alCambiar) {
  let ocupado = false;

  return {
    get ocupado() { return ocupado; },
    async interpretar(consulta) {
      ocupado = true;
      alCambiar(true);
      try {
        return await interpretar(consulta);
      } finally {
        ocupado = false;
        alCambiar(false);
      }
    }
  };
}
