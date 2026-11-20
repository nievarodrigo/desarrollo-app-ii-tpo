/**
 * CAPA DE PRESENTACIÓN del componente Carrito — `CarritoView`.
 *
 * Panel lateral: render de la lista, controles de cantidad y confirmación de
 * compra. No calcula totales ni valida stock — todo eso sale ya resuelto de
 * `carrito-service.js`. Lo que sí hace, y es su trabajo, es traducir el
 * resultado del negocio a algo que una persona entienda: un tope de stock no es
 * un error, es un aviso; una sesión faltante no es un error, es un formulario.
 */

import * as carrito from '../negocio/carrito-service.js';
import { estadoDePedido, vistaConfirmacionPedido } from './pedido-vista.js';
import { precio, cuota } from '../../../compartido/formato.js';
import { arteProducto } from '../../../compartido/arte.js';
import { abrirCapa, cerrarCapa, capaAbierta } from '../../../compartido/capa-modal.js';
import { avisar } from '../../../compartido/avisos.js';
import { SesionVencidaError, hayBackend, obtener, publicar } from '../../../compartido/cliente-http.js';

const panel = document.querySelector('[data-carrito]');
const cuerpo = document.querySelector('[data-carrito-cuerpo]');
const pie = document.querySelector('[data-carrito-pie]');
const progreso = document.querySelector('[data-envio-progreso]');
const progresoTexto = document.querySelector('[data-envio-texto]');
const progresoBarra = document.querySelector('[data-envio-barra]');
const contadorCabecera = document.querySelector('[data-carrito-contador]');
const contadorTitulo = document.querySelector('[data-carrito-titulo-contador]');
const botonAbrir = document.querySelector('[data-abrir-carrito]');

const totales = {
  subtotal: document.querySelector('[data-total-subtotal]'),
  envio: document.querySelector('[data-total-envio]'),
  final: document.querySelector('[data-total-final]'),
  cuotas: document.querySelector('[data-total-cuotas]')
};

const ICONO_MENOS = '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M3 8h10" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>';
const ICONO_MAS = '<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>';

/** Estado de confirmación: al confirmar mostramos el comprobante hasta reabrir. */
let pedidoConfirmado = null;

function vacio() {
  return `
    <div class="carrito__vacio">
      <span class="estado__icono" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false"><path d="M3 4h2.2l2.3 11.2a1.6 1.6 0 0 0 1.6 1.3h8.3a1.6 1.6 0 0 0 1.6-1.2L21 8H6.2" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="20" r="1.5" fill="currentColor"/><circle cx="17.5" cy="20" r="1.5" fill="currentColor"/></svg>
      </span>
      <h3>Tu carrito está vacío</h3>
      <p>Todavía no agregaste nada. Empezá por el catálogo: hay más de 150 productos esperando.</p>
      <button class="boton boton--primario" type="button" data-ir-al-catalogo>Ver el catálogo</button>
    </div>`;
}

function fila(item) {
  const enTope = item.cantidad >= item.stock;
  return `
    <li class="item" data-item="${item.id}">
      <div class="item__medio">${arteProducto(item)}</div>
      <div class="item__cuerpo">
        <p class="item__marca">${item.marca}</p>
        <div class="item__fila">
          <p class="item__nombre">${item.nombre}</p>
          <p class="item__precio">${precio(item.precio * item.cantidad)}</p>
        </div>
        <p class="item__unitario">${precio(item.precio)} c/u</p>
        ${enTope ? `<p class="item__tope">Llegaste al stock disponible (${item.stock})</p>` : ''}
        <div class="item__controles">
          <div class="contador">
            <button type="button" data-menos="${item.id}" aria-label="Quitar una unidad de ${item.nombre}">${ICONO_MENOS}</button>
            <output aria-label="Cantidad de ${item.nombre}">${item.cantidad}</output>
            <button type="button" data-mas="${item.id}" ${enTope ? 'disabled' : ''} aria-label="Agregar una unidad de ${item.nombre}">${ICONO_MAS}</button>
          </div>
          <button class="item__quitar" type="button" data-quitar="${item.id}">Quitar</button>
        </div>
      </div>
    </li>`;
}

function render(resumen) {
  if (!cuerpo) return;

  /* Insignia de la cabecera */
  if (contadorCabecera) {
    contadorCabecera.textContent = resumen.unidades;
    contadorCabecera.hidden = resumen.unidades === 0;
    if (resumen.unidades > 0) {
      contadorCabecera.dataset.pulso = 'true';
      setTimeout(() => delete contadorCabecera.dataset.pulso, 450);
    }
  }
  if (botonAbrir) {
    botonAbrir.setAttribute(
      'aria-label',
      resumen.unidades === 0
        ? 'Abrir el carrito, está vacío'
        : `Abrir el carrito, ${resumen.unidades} ${resumen.unidades === 1 ? 'producto' : 'productos'}`
    );
  }
  if (contadorTitulo) {
    contadorTitulo.textContent = resumen.unidades ? `(${resumen.unidades})` : '';
  }

  if (pedidoConfirmado) {
    cuerpo.innerHTML = vistaConfirmacionPedido(pedidoConfirmado);
    if (pie) pie.hidden = true;
    if (progreso) progreso.hidden = true;
    return;
  }

  if (resumen.unidades === 0) {
    cuerpo.innerHTML = vacio();
    if (pie) pie.hidden = true;
    if (progreso) progreso.hidden = true;
    return;
  }

  cuerpo.innerHTML = `<ul class="carrito__lista">${resumen.items.map(fila).join('')}</ul>`;
  if (pie) pie.hidden = false;

  if (progreso && progresoTexto && progresoBarra) {
    progreso.hidden = false;
    const alcanzado = resumen.faltaParaEnvioGratis === 0;
    progresoTexto.innerHTML = alcanzado
      ? '🎉 <strong>Tenés envío gratis</strong> en este pedido.'
      : `Te faltan <strong>${precio(resumen.faltaParaEnvioGratis)}</strong> para el envío gratis.`;
    const avance = Math.min(100, (resumen.subtotal / carrito.ENVIO_GRATIS_DESDE) * 100);
    progresoBarra.style.width = `${avance}%`;
  }

  if (totales.subtotal) totales.subtotal.textContent = precio(resumen.subtotal);
  if (totales.envio) totales.envio.textContent = resumen.envio === 0 ? 'Gratis' : precio(resumen.envio);
  if (totales.final) totales.final.textContent = precio(resumen.total);
  if (totales.cuotas) totales.cuotas.textContent = `o 12 cuotas de ${cuota(resumen.total)} sin interés`;
}

export function abrirCarrito(disparador) {
  if (!panel) return;
  abrirCapa(panel, {
    disparador,
    focoInicial: panel.querySelector('[data-cerrar-carrito]')
  });
}

export function cerrarCarrito() {
  if (panel && capaAbierta(panel)) cerrarCapa();
}

/**
 * Conecta los eventos del panel.
 *
 * Las dos dependencias las inyecta `main.js` como funciones, no como módulos:
 * así esta vista no importa ni la navegación de la landing ni el componente
 * Usuario, y se puede montar sola.
 *
 * @param {object} opciones
 * @param {Function} [opciones.alIrAlCatalogo] llevar el foco al catálogo
 * @param {Function} [opciones.alExigirSesion] `(roles) => boolean`; si devuelve
 *   `false` ya se encargó de avisar y de ofrecer el ingreso
 */
export function iniciarCarritoView({ alIrAlCatalogo, alExigirSesion } = {}) {
  if (!panel || !cuerpo) return;

  carrito.alCambiar(render);

  botonAbrir?.addEventListener('click', (e) => {
    pedidoConfirmado = null;
    render(carrito.resumen());
    abrirCarrito(e.currentTarget);
  });

  panel.querySelector('[data-cerrar-carrito]')?.addEventListener('click', cerrarCarrito);

  cuerpo.addEventListener('click', (evento) => {
    const boton = evento.target.closest('button');
    if (!boton) return;

    const { menos, mas, quitar } = boton.dataset;

    if (menos) {
      const id = Number(menos);
      carrito.cambiarCantidad(id, carrito.cantidadDe(id) - 1);
      devolverFoco(id, 'menos');
    } else if (mas) {
      const id = Number(mas);
      const resultado = carrito.cambiarCantidad(id, carrito.cantidadDe(id) + 1);
      if (!resultado.ok && resultado.motivo === 'tope-stock') {
        avisar('No hay más stock disponible de ese producto.', 'alerta');
      }
      devolverFoco(id, 'mas');
    } else if (quitar) {
      const item = carrito.obtenerItems().find((it) => it.id === Number(quitar));
      carrito.quitar(Number(quitar));
      avisar(`Quitamos ${item?.nombre ?? 'el producto'} del carrito.`, 'alerta');
      cuerpo.focus({ preventScroll: true });
    } else if (boton.dataset.irAlCatalogo !== undefined) {
      cerrarCarrito();
      alIrAlCatalogo?.();
    } else if (boton.dataset.seguirComprando !== undefined) {
      pedidoConfirmado = null;
      render(carrito.resumen());
      cerrarCarrito();
      alIrAlCatalogo?.();
    } else if (boton.dataset.consultarPedido !== undefined
        && pedidoConfirmado && estadoDePedido(pedidoConfirmado.estado).consultable) {
      const consultado = pedidoConfirmado;
      boton.disabled = true;
      obtener(`pedidos/${consultado.codigo}`, {}, { requiereSesion: true })
        .then((actual) => {
          if (pedidoConfirmado !== consultado) return;
          consultado.estado = actual.estado;
          render(carrito.resumen());
          avisar(estadoDePedido(actual.estado).titulo, 'ok');
          if (capaAbierta(panel)) {
            (cuerpo.querySelector('[data-consultar-pedido]') ?? cuerpo).focus({ preventScroll: true });
          }
        })
        .catch((error) => avisar(error instanceof SesionVencidaError
          ? error.message
          : 'No pudimos consultar el estado. Intentá de nuevo.', 'alerta'))
        .finally(() => { boton.disabled = false; });
    }
  });

  /* Tras re-renderizar la lista el nodo original desaparece: devolvemos el foco
     al botón equivalente para no perder el lugar con teclado. */
  function devolverFoco(id, tipo) {
    requestAnimationFrame(() => {
      const destino =
        cuerpo.querySelector(`[data-${tipo}="${id}"]:not([disabled])`) ??
        cuerpo.querySelector(`[data-item="${id}"] button:not([disabled])`) ??
        cuerpo;
      destino.focus({ preventScroll: true });
    });
  }

  document.querySelector('[data-vaciar]')?.addEventListener('click', () => {
    carrito.vaciar();
    avisar('Vaciamos el carrito.', 'alerta');
    cuerpo.focus({ preventScroll: true });
  });

  /**
   * Confirmar compra — OPERACIÓN SENSIBLE.
   *
   * Dos controles, con propósitos distintos:
   *
   *   1. `alExigirSesion()` es USABILIDAD: si no hay sesión, en vez de un error
   *      seco le abrimos el formulario de ingreso. No autoriza nada.
   *   2. `carrito.confirmarCompra()` es SEGURIDAD: está declarada con
   *      `autorizar([CLIENTE, ADMIN], …)` en la capa de negocio y lanza igual
   *      si alguien la invoca desde la consola salteando este botón.
   *
   * Si borráramos el paso 1, la operación seguiría protegida. Si borráramos el
   * paso 2, no. Esa es la diferencia entre un chequeo de UI y un control de
   * acceso, y es la pregunta que conviene tener contestada.
   */
  /**
   * UUID v4 para la Idempotency-Key. `crypto.randomUUID` sólo existe en contextos
   * seguros (https o localhost); servido por http://IP:8080 se arma con getRandomValues.
   */
  function nuevaClave() {
    if (globalThis.crypto?.randomUUID) return crypto.randomUUID();
    const b = crypto.getRandomValues(new Uint8Array(16));
    b[6] = (b[6] & 0x0f) | 0x40;
    b[8] = (b[8] & 0x3f) | 0x80;
    const h = [...b].map((x) => x.toString(16).padStart(2, '0')).join('');
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
  }

  /** Idempotency-Key de la compra en curso (y el carrito al que corresponde); se conserva entre reintentos fallidos. */
  let claveDeCompra = null;

  document.querySelector('[data-checkout]')?.addEventListener('click', async (evento) => {
    const boton = evento.currentTarget;
    if (carrito.resumen().unidades === 0) return;

    if (alExigirSesion && !alExigirSesion(['CLIENTE', 'ADMIN'], boton)) return;

    boton.disabled = true;
    boton.textContent = 'Enviando pedido…';

    try {
      let pedido;
      if (await hayBackend()) {
        // REST acepta el comando; JMS procesa después. No se manda el precio
        // calculado por el navegador, sólo producto y cantidad.
        const items = carrito.obtenerItems().map((item) => ({
          idProducto: item.id, cantidad: item.cantidad
        }));
        // Una clave por compra: si la red falla y el usuario reintenta, el servidor
        // reconoce la misma clave y devuelve el pedido ya creado en vez de duplicarlo.
        // Si el carrito cambió entre intentos es otra compra: clave nueva.
        const contenido = JSON.stringify(items);
        if (claveDeCompra?.contenido !== contenido) {
          claveDeCompra = { clave: nuevaClave(), contenido };
        }
        const aceptado = await publicar('pedidos', { items }, { 'Idempotency-Key': claveDeCompra.clave }, { requiereSesion: true });
        claveDeCompra = null;
        pedido = {
          codigo: aceptado.id,
          total: Number(aceptado.total),
          unidades: items.reduce((suma, item) => suma + item.cantidad, 0),
          estado: aceptado.estado
        };
        carrito.vaciar();
      } else {
        // Despliegue estático: conserva la simulación local, rotulada como tal.
        const resultado = carrito.confirmarCompra();
        if (!resultado.ok) {
          avisar('El carrito quedó vacío.', 'alerta');
          return;
        }
        pedido = resultado.pedido;
      }
      pedidoConfirmado = pedido;
      render(carrito.resumen());
      const mensajes = {
        PENDIENTE: 'Pedido aceptado para procesar.',
        PROCESADO: 'Pedido aceptado y procesado.',
        ERROR_ENVIO: 'Pedido aceptado. Lo enviamos apenas se normalice el servicio: no hace falta comprar de nuevo.'
      };
      avisar(mensajes[pedido.estado] ?? 'Pedido simulado localmente.', 'ok');
      panel.querySelector('[data-cerrar-carrito]')?.focus({ preventScroll: true });
    } catch (error) {
      // Si falló REST no vaciamos el carrito ni simulamos una compra exitosa.
      avisar(error.message || 'No pudimos enviar el pedido.', 'alerta');
    } finally {
      boton.disabled = false;
      boton.textContent = 'Confirmar compra';
    }
  });

  render(carrito.resumen());
}
