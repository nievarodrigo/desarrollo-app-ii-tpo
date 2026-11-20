import { precio } from '../../../compartido/formato.js';

const aceptado = 'Todavía no se cobró ni se confirmó el pago.';
const estados = new Map([
  ['ERROR_ENVIO', { titulo: 'Pedido recibido', tono: 'pendiente', detalle: 'Tu pedido está guardado. Volveremos a enviarlo automáticamente. ' + aceptado, consultable: true }],
  ['PENDIENTE_VALIDACION', { titulo: 'Pedido recibido', tono: 'pendiente', detalle: 'Estamos validando el pedido. ' + aceptado, consultable: true }],
  ['PENDIENTE', { titulo: 'Procesamiento pendiente', tono: 'pendiente', detalle: 'El servidor aceptó el pedido y sigue procesándolo. ' + aceptado, consultable: true }],
  ['PROCESADO', { titulo: 'Pedido procesado', tono: 'ok', detalle: 'Terminó el procesamiento del pedido. ' + aceptado }],
  ['CONFIRMADO', { titulo: 'Pedido confirmado', tono: 'ok', detalle: 'El pedido pasó a confirmado.' }],
  ['RECHAZADO', { titulo: 'Pedido rechazado', tono: 'neutral', detalle: 'El servidor rechazó este pedido.' }],
  ['CANCELADO', { titulo: 'Pedido cancelado', tono: 'neutral', detalle: 'El pedido figura como cancelado.' }]
]);

export function estadoDePedido(estado) {
  if (estado == null) {
    return { titulo: 'Compra simulada', tono: 'neutral', detalle: 'Esta confirmación es local: no se cobró ni se envió un correo.' };
  }
  return estados.get(estado) ?? { titulo: 'Estado sin confirmar', tono: 'neutral', detalle: 'No pudimos identificar el estado del pedido.' };
}

export function vistaConfirmacionPedido(pedido) {
  const { titulo, tono, detalle, consultable } = estadoDePedido(pedido.estado);
  const codigo = String(pedido.codigo).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
  const icono = tono === 'ok'
    ? '<path d="m5 12.5 4.5 4.5L19 7.5"/>'
    : '<circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3 2"/>';
  return `
    <div class="carrito__confirmado" data-tono="${tono}">
      <span class="estado__icono" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${icono}</svg>
      </span>
      <p class="carrito__estado">Estado del pedido</p>
      <h3>${titulo}</h3>
      <p>${detalle}</p>
      <code>Pedido ${codigo}</code>
      <p><strong>${precio(pedido.total)}</strong> en ${pedido.unidades} ${pedido.unidades === 1 ? 'producto' : 'productos'}</p>
      ${consultable ? '<button class="boton boton--fantasma" type="button" data-consultar-pedido>Consultar estado</button>' : ''}
      <button class="boton boton--fantasma" type="button" data-seguir-comprando>Seguir comprando</button>
    </div>`;
}
