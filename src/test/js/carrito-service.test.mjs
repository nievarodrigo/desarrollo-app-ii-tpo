/**
 * Pruebas del componente CARRITO — capa de negocio.
 *
 * Dos cosas se prueban acá, y son las dos que hay que poder defender:
 *
 *   1. LAS REGLAS: tope de stock, totales recalculados, umbral de envío gratis.
 *   2. QUE ES STATEFUL: que el resultado de una operación depende de la
 *      historia previa. Es justo lo contrario del test del catálogo, donde se
 *      prueba que NO depende de nada.
 *
 * Y una tercera, que cruza componentes: que `confirmarCompra()` está realmente
 * protegida por la autorización del componente Usuario.
 */

import './stub-navegador.mjs';
import { crudoDe } from './stub-navegador.mjs';

import { test, describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';

import * as carrito from '../../main/webapp/assets/js/componentes/carrito/negocio/carrito-service.js';
import {
  ENVIO_GRATIS_DESDE,
  COSTO_ENVIO
} from '../../main/webapp/assets/js/componentes/carrito/negocio/carrito-service.js';
import {
  NoAutenticadoError,
  asegurarSemilla,
  egresar,
  ingresar
} from '../../main/webapp/assets/js/componentes/usuario/negocio/usuario-service.js';

/** Producto de prueba; cada caso pisa el stock o el precio que le importa. */
function producto(parcial = {}) {
  return {
    id: 1,
    sku: 'SKU-1',
    nombre: 'Mouse Inalámbrico',
    marca: 'Logi',
    precio: 45000,
    arte: 'generico',
    stock: 10,
    ...parcial
  };
}

beforeEach(() => {
  carrito.vaciar();
  egresar();
});

/* ------------------------------------------------------------------ */

describe('Reglas de stock', () => {
  test('agrega un producto disponible', () => {
    assert.deepEqual(carrito.agregar(producto()), { ok: true, cantidad: 1 });
    assert.equal(carrito.resumen().unidades, 1);
  });

  test('un producto sin stock se rechaza', () => {
    assert.deepEqual(carrito.agregar(producto({ stock: 0 })), { ok: false, motivo: 'sin-stock' });
    assert.equal(carrito.resumen().unidades, 0);
  });

  test('agregar el mismo producto acumula en una sola línea', () => {
    carrito.agregar(producto());
    carrito.agregar(producto());
    const { items, unidades } = carrito.resumen();
    assert.equal(items.length, 1, 'una línea');
    assert.equal(unidades, 2, 'dos unidades');
  });

  test('nunca se pasa del stock disponible', () => {
    carrito.agregar(producto({ stock: 3 }), 10);
    assert.equal(carrito.cantidadDe(1), 3, 'se recorta al stock');
  });

  test('llegado al tope, un agregado más se rechaza explicando por qué', () => {
    carrito.agregar(producto({ stock: 2 }), 2);
    assert.deepEqual(carrito.agregar(producto({ stock: 2 })), {
      ok: false,
      motivo: 'tope-stock',
      cantidad: 2
    });
  });

  test('cambiar la cantidad por encima del stock la deja en el tope', () => {
    carrito.agregar(producto({ stock: 4 }));
    assert.deepEqual(carrito.cambiarCantidad(1, 99), { ok: false, motivo: 'tope-stock', cantidad: 4 });
    assert.equal(carrito.cantidadDe(1), 4);
  });

  test('cantidad 0 o negativa elimina el ítem', () => {
    carrito.agregar(producto());
    assert.deepEqual(carrito.cambiarCantidad(1, 0), { ok: true, cantidad: 0 });
    assert.equal(carrito.resumen().items.length, 0);
  });

  test('operar sobre un ítem que no está devuelve "inexistente"', () => {
    assert.deepEqual(carrito.cambiarCantidad(404, 2), { ok: false, motivo: 'inexistente' });
    assert.deepEqual(carrito.quitar(404), { ok: false, motivo: 'inexistente' });
  });

  test('el precio y el stock se refrescan desde el catálogo al re-agregar', () => {
    carrito.agregar(producto({ precio: 45000, stock: 10 }));
    carrito.agregar(producto({ precio: 50000, stock: 6 }));
    const [item] = carrito.resumen().items;
    assert.equal(item.precio, 50000, 'el catálogo es la fuente de verdad');
    assert.equal(item.stock, 6);
  });
});

describe('Totales', () => {
  test('el subtotal es precio por cantidad, sumado', () => {
    carrito.agregar(producto({ id: 1, precio: 45000 }), 2);
    carrito.agregar(producto({ id: 2, precio: 30000 }));
    assert.equal(carrito.resumen().subtotal, 45000 * 2 + 30000);
  });

  test('por debajo del umbral se cobra el envío', () => {
    carrito.agregar(producto({ precio: 45000 }));
    const { envio, total, subtotal } = carrito.resumen();
    assert.equal(envio, COSTO_ENVIO);
    assert.equal(total, subtotal + COSTO_ENVIO);
  });

  test('el umbral de envío gratis es inclusivo', () => {
    carrito.agregar(producto({ precio: ENVIO_GRATIS_DESDE, stock: 1 }));
    assert.equal(carrito.resumen().envio, 0, 'exactamente el umbral ya es gratis');
  });

  test('informa cuánto falta para el envío gratis', () => {
    carrito.agregar(producto({ precio: 100000, stock: 1 }));
    assert.equal(carrito.resumen().faltaParaEnvioGratis, ENVIO_GRATIS_DESDE - 100000);
  });

  test('con el envío ya gratis, no falta nada', () => {
    carrito.agregar(producto({ precio: 500000, stock: 1 }));
    assert.equal(carrito.resumen().faltaParaEnvioGratis, 0);
  });

  test('un carrito vacío no cobra envío', () => {
    assert.deepEqual(
      { unidades: 0, subtotal: 0, envio: 0, total: 0 },
      (({ unidades, subtotal, envio, total }) => ({ unidades, subtotal, envio, total }))(carrito.resumen())
    );
  });

  test('el total se RECALCULA, nunca se guarda', () => {
    carrito.agregar(producto({ precio: 45000 }), 2);
    const antes = carrito.resumen().subtotal;
    carrito.cambiarCantidad(1, 1);
    assert.equal(carrito.resumen().subtotal, antes / 2, 'el total siguió al cambio');
  });

  test('mutar el resumen devuelto no afecta el carrito', () => {
    carrito.agregar(producto());
    const resumen = carrito.resumen();
    resumen.items[0].cantidad = 999;
    assert.equal(carrito.cantidadDe(1), 1, 'el resumen es una copia');
  });
});

describe('Es STATEFUL', () => {
  test('la MISMA llamada da distinto resultado según la historia previa', () => {
    const disponible = producto({ stock: 2 });

    /* Tres veces exactamente la misma invocación, con el mismo argumento. */
    assert.equal(carrito.agregar(disponible).ok, true, '1.ª vez: entra');
    assert.equal(carrito.agregar(disponible).ok, true, '2.ª vez: entra');
    assert.equal(carrito.agregar(disponible).ok, false, '3.ª vez: rechazada por el estado acumulado');

    /* Esto es lo que NO puede pasar en un componente stateless. El catálogo,
       llamado tres veces igual, contesta tres veces lo mismo. */
  });

  test('el orden de las operaciones importa', () => {
    const p = producto({ stock: 5 });

    carrito.agregar(p, 3);
    carrito.quitar(1);
    const despuesDeQuitar = carrito.agregar(p, 3);

    carrito.vaciar();
    carrito.agregar(p, 3);
    const sinQuitar = carrito.agregar(p, 3);

    assert.equal(despuesDeQuitar.cantidad, 3);
    assert.equal(sinQuitar.cantidad, 5, 'la misma secuencia sin el quitar llega al tope');
  });

  test('el estado sobrevive en el repositorio', () => {
    carrito.agregar(producto(), 2);
    const crudo = crudoDe('carrito');
    assert.ok(crudo.includes('SKU-1'), 'el carrito quedó persistido');
    assert.ok(!crudo.includes('"total"'), 'pero el total NO se persiste: se recalcula');
  });

  test('vaciar limpia el estado conversacional', () => {
    carrito.agregar(producto(), 3);
    assert.deepEqual(carrito.vaciar(), { ok: true });
    assert.equal(carrito.resumen().unidades, 0);
  });
});

describe('Observer', () => {
  test('notifica al suscribirse y en cada cambio de estado', () => {
    const unidadesVistas = [];
    const desuscribir = carrito.alCambiar((r) => unidadesVistas.push(r.unidades));

    carrito.agregar(producto());
    carrito.agregar(producto());
    carrito.quitar(1);
    desuscribir();
    carrito.agregar(producto());

    assert.deepEqual(unidadesVistas, [0, 1, 2, 0], 'después de desuscribirse no llega nada');
  });

  test('una operación rechazada no notifica', () => {
    let avisos = 0;
    const desuscribir = carrito.alCambiar(() => {
      avisos += 1;
    });

    avisos = 0;
    carrito.agregar(producto({ stock: 0 }));
    assert.equal(avisos, 0, 'nada cambió, nadie se enteró');

    desuscribir();
  });
});

describe('Confirmar compra — operación sensible', () => {
  test('sin sesión lanza 401, aunque el carrito tenga ítems', () => {
    carrito.agregar(producto());
    assert.throws(() => carrito.confirmarCompra(), NoAutenticadoError);
  });

  test('el rechazo por autorización NO vacía el carrito', () => {
    carrito.agregar(producto(), 2);
    assert.throws(() => carrito.confirmarCompra(), NoAutenticadoError);
    assert.equal(carrito.resumen().unidades, 2, 'la operación no dejó el estado a medias');
  });

  test('con sesión y con ítems, emite el pedido y vacía el carrito', async () => {
    await asegurarSemilla();
    await ingresar('cliente@fachada.com.ar', 'cliente123');

    carrito.agregar(producto({ precio: 45000 }), 2);
    const totalEsperado = carrito.resumen().total;

    const { ok, pedido } = carrito.confirmarCompra();

    assert.equal(ok, true);
    assert.match(pedido.codigo, /^FCH-\d{5}$/);
    assert.equal(pedido.total, totalEsperado, 'el total sale del resumen recalculado');
    assert.equal(pedido.unidades, 2);
    assert.equal(carrito.resumen().unidades, 0, 'la conversación se cerró');
  });

  test('con sesión pero carrito vacío, no emite pedido', async () => {
    await asegurarSemilla();
    await ingresar('cliente@fachada.com.ar', 'cliente123');

    assert.deepEqual(carrito.confirmarCompra(), { ok: false, motivo: 'carrito-vacio' });
  });

  test('un ADMIN también puede comprar', async () => {
    await asegurarSemilla();
    await ingresar('admin@fachada.com.ar', 'admin123');

    carrito.agregar(producto());
    assert.equal(carrito.confirmarCompra().ok, true);
  });
});
