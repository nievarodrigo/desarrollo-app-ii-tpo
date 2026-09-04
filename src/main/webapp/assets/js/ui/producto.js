/** Render de tarjetas de producto, puntajes y ficha de detalle. */

import { precio, cuota, numero, porcentajeDescuento } from '../core/format.js';
import { arteProducto } from './art.js';

const STOCK_BAJO = 5;

export function estrellas(rating) {
  const porcentaje = Math.max(0, Math.min(100, (rating / 5) * 100));
  return `<span class="puntaje__estrellas" aria-hidden="true">★★★★★<span style="width:${porcentaje}%">★★★★★</span></span>`;
}

function puntaje(producto) {
  return `
    <p class="puntaje">
      ${estrellas(producto.rating)}
      <strong>${producto.rating.toFixed(1)}</strong>
      <span class="visualmente-oculto">de 5 estrellas</span>
      <span>(${numero(producto.resenias)})</span>
    </p>`;
}

function stock(producto) {
  if (producto.stock === 0) {
    return '<p class="producto__stock stock--cero">Sin stock por ahora</p>';
  }
  if (producto.stock <= STOCK_BAJO) {
    return `<p class="producto__stock stock--bajo">¡Quedan ${producto.stock}!</p>`;
  }
  return `<p class="producto__stock stock--ok">${producto.stock} disponibles</p>`;
}

function marbetes(producto) {
  const marcas = [];
  if (producto.stock === 0) marcas.push('<span class="marbete marbete--agotado">Sin stock</span>');
  else if (producto.precioAnterior) {
    marcas.push(
      `<span class="marbete marbete--oferta">−${porcentajeDescuento(producto.precio, producto.precioAnterior)} %</span>`
    );
  }
  if (producto.etiquetas.includes('destacado')) {
    marcas.push('<span class="marbete marbete--destacado">Destacado</span>');
  }
  return marcas.length ? `<div class="producto__marcas">${marcas.join('')}</div>` : '';
}

function precios(producto) {
  const descuento = porcentajeDescuento(producto.precio, producto.precioAnterior);
  return `
    <div class="producto__precios">
      <span class="producto__precio">${precio(producto.precio)}</span>
      ${producto.precioAnterior ? `<span class="producto__precio-anterior">${precio(producto.precioAnterior)}</span>` : ''}
      ${descuento ? `<span class="producto__descuento">${descuento} % OFF</span>` : ''}
    </div>
    <p class="producto__cuotas">12x ${cuota(producto.precio)} sin interés</p>`;
}

const nombreCategorias = {
  tecnologia: 'Tecnología',
  audio: 'Audio',
  hogar: 'Hogar',
  oficina: 'Oficina',
  deportes: 'Deportes'
};

/**
 * Tarjeta del catálogo.
 * @param {object} producto
 * @param {number} enCarrito unidades ya agregadas, para reflejar el tope de stock
 */
export function tarjetaProducto(producto, enCarrito = 0) {
  const agotado = producto.stock === 0;
  const topeAlcanzado = !agotado && enCarrito >= producto.stock;

  const textoBoton = agotado
    ? 'Sin stock'
    : topeAlcanzado
      ? 'Máximo en carrito'
      : enCarrito > 0
        ? `Agregar otra (${enCarrito})`
        : 'Agregar al carrito';

  return `
    <article class="producto${agotado ? ' producto--agotado' : ''}" data-producto="${producto.id}">
      <div class="producto__medio">
        ${arteProducto(producto)}
        ${marbetes(producto)}
        <button class="producto__ver" type="button" data-ver="${producto.id}">
          <span>Ver detalle</span>
        </button>
      </div>
      <div class="producto__cuerpo">
        <p class="producto__meta">
          <span>${producto.marca}</span>
          <span>${nombreCategorias[producto.categoria] ?? producto.categoria}</span>
        </p>
        <h3 class="producto__nombre">${producto.nombre}</h3>
        ${puntaje(producto)}
        ${precios(producto)}
        ${stock(producto)}
        <div class="producto__accion">
          <button
            class="boton ${agotado || topeAlcanzado ? 'boton--fantasma' : 'boton--primario'} boton--ancho"
            type="button"
            data-agregar="${producto.id}"
            ${agotado || topeAlcanzado ? 'disabled' : ''}
            aria-label="${agotado ? `${producto.nombre} sin stock` : `Agregar ${producto.nombre} al carrito`}">
            ${textoBoton}
          </button>
        </div>
      </div>
    </article>`;
}

/** Contenido del modal de detalle. */
export function detalleProducto(producto, enCarrito = 0) {
  const agotado = producto.stock === 0;
  const topeAlcanzado = !agotado && enCarrito >= producto.stock;
  const descuento = porcentajeDescuento(producto.precio, producto.precioAnterior);

  return `
    <div class="detalle">
      <div class="detalle__medio">
        ${arteProducto(producto)}
        ${marbetes(producto)}
        <button class="icono-boton detalle__cerrar" type="button" data-cerrar-modal aria-label="Cerrar el detalle">
          <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m7 7 10 10M17 7 7 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </button>
      </div>
      <div class="detalle__cuerpo">
        <p class="producto__meta">
          <span>${producto.marca}</span>
          <span>${nombreCategorias[producto.categoria] ?? producto.categoria}</span>
        </p>
        <h2 class="detalle__titulo" id="modal-titulo">${producto.nombre}</h2>
        ${puntaje(producto)}
        <p class="detalle__precio">${precio(producto.precio)}</p>
        <p class="producto__cuotas">
          ${producto.precioAnterior ? `<s>${precio(producto.precioAnterior)}</s> · ${descuento} % OFF · ` : ''}
          12 cuotas de ${cuota(producto.precio)} sin interés
        </p>
        ${stock(producto)}
        <p class="detalle__descripcion">${producto.descripcion}</p>
        <ul class="detalle__destacados">
          ${producto.destacados.map((d) => `<li>${d}</li>`).join('')}
        </ul>
        <div class="producto__accion">
          <button
            class="boton ${agotado || topeAlcanzado ? 'boton--fantasma' : 'boton--primario'} boton--ancho boton--grande"
            type="button"
            data-agregar="${producto.id}"
            ${agotado || topeAlcanzado ? 'disabled' : ''}>
            ${agotado ? 'Sin stock' : topeAlcanzado ? 'Ya tenés el máximo disponible' : 'Agregar al carrito'}
          </button>
        </div>
        <p class="detalle__ficha">
          <span>SKU <strong>${producto.sku}</strong></span>
          <span>Garantía <strong>12 meses</strong></span>
          <span>Envío <strong>${producto.precio >= 150000 ? 'gratis' : 'a calcular'}</strong></span>
        </p>
      </div>
    </div>`;
}
