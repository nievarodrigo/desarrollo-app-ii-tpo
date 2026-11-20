import { test } from 'node:test';
import assert from 'node:assert/strict';
import { vistaConfirmacionPedido } from '../../main/webapp/assets/js/componentes/carrito/presentacion/pedido-vista.js';

const vista = (estado) => vistaConfirmacionPedido({ estado, codigo: 'GM-12345', total: 9000, unidades: 1 });

test('un pedido recibido para reintento y uno pendiente explican la espera', () => {
  const recibido = vista('ERROR_ENVIO');
  assert.match(recibido, /Pedido recibido/);
  assert.match(recibido, /automáticamente/);
  assert.match(recibido, /data-consultar-pedido/);

  const pendiente = vista('PENDIENTE');
  assert.match(pendiente, /Procesamiento pendiente/);
  assert.match(pendiente, /data-consultar-pedido/);
});

test('procesado muestra fin del procesamiento y conserva la distinción con el pago', () => {
  const html = vista('PROCESADO');
  assert.match(html, /Pedido procesado/);
  assert.match(html, /no se cobró/);
  assert.doesNotMatch(html, /data-consultar-pedido/);
});

test('sólo la confirmación local aparece como compra simulada', () => {
  assert.match(vista(), /Compra simulada/);
  assert.match(vista('CONFIRMADO'), /Pedido confirmado/);
  assert.doesNotMatch(vista('ESTADO_DESCONOCIDO'), /Compra simulada/);
});
