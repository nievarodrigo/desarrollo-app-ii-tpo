/** Formatos locales: todo el sitio es es-AR y los precios están en pesos. */

const pesos = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 0,
  maximumFractionDigits: 0
});

const pesosConCentavos = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 2
});

export function precio(valor) {
  return pesos.format(valor ?? 0);
}

export function precioExacto(valor) {
  return pesosConCentavos.format(valor ?? 0);
}

export function numero(valor) {
  return new Intl.NumberFormat('es-AR').format(valor ?? 0);
}

/** 12 cuotas sin interés es el gancho comercial más común en Argentina. */
export function cuota(valor, cuotas = 12) {
  return precio(Math.round(valor / cuotas));
}

export function porcentajeDescuento(precioActual, precioAnterior) {
  if (!precioAnterior || precioAnterior <= precioActual) return 0;
  return Math.round((1 - precioActual / precioAnterior) * 100);
}
