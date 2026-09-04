/**
 * Carrito del lado del cliente.
 *
 * Replica en el front las reglas que después validará `CarritoService`
 * (`TPO/MOCKUP_TPO.md` §10): la cantidad nunca baja de 1, nunca supera el stock
 * disponible y el total se recalcula siempre desde los ítems, jamás se guarda
 * un total "de confianza". Cuando exista el backend, este módulo pasa a ser un
 * caché optimista de `GET /api/carritos/{id}`.
 */

import { leer, escribir } from './storage.js';

const CLAVE = 'carrito';

/** Umbral y costo de envío. En el backend saldrán de una regla de negocio. */
export const ENVIO_GRATIS_DESDE = 150000;
export const COSTO_ENVIO = 8900;

const oyentes = new Set();

/** @type {Array<{id:number, sku:string, nombre:string, marca:string, precio:number, arte:string, stock:number, cantidad:number}>} */
let items = normalizarItems(leer(CLAVE, []));

function normalizarItems(crudo) {
  if (!Array.isArray(crudo)) return [];
  return crudo
    .filter((it) => it && typeof it.id === 'number' && typeof it.precio === 'number')
    .map((it) => ({
      ...it,
      cantidad: Math.max(1, Math.min(Number(it.cantidad) || 1, Number(it.stock) || 1))
    }));
}

function persistir() {
  escribir(CLAVE, items);
  const instantanea = resumen();
  oyentes.forEach((fn) => fn(instantanea));
}

/** Suscripción al estado del carrito. Devuelve la función para desuscribirse. */
export function alCambiar(fn) {
  oyentes.add(fn);
  fn(resumen());
  return () => oyentes.delete(fn);
}

export function obtenerItems() {
  return items.map((it) => ({ ...it }));
}

export function cantidadDe(idProducto) {
  return items.find((it) => it.id === idProducto)?.cantidad ?? 0;
}

/**
 * Agrega un producto. Devuelve el resultado para que la UI pueda avisar
 * cuando la operación fue rechazada por stock.
 * @returns {{ok:boolean, motivo?:string, cantidad?:number}}
 */
export function agregar(producto, cantidad = 1) {
  if (!producto || producto.stock <= 0) {
    return { ok: false, motivo: 'sin-stock' };
  }

  const pedida = Math.max(1, Math.trunc(cantidad));
  const existente = items.find((it) => it.id === producto.id);
  const yaEnCarrito = existente?.cantidad ?? 0;

  if (yaEnCarrito >= producto.stock) {
    return { ok: false, motivo: 'tope-stock', cantidad: yaEnCarrito };
  }

  const nuevaCantidad = Math.min(yaEnCarrito + pedida, producto.stock);

  if (existente) {
    existente.cantidad = nuevaCantidad;
    existente.stock = producto.stock;
    existente.precio = producto.precio;
  } else {
    items = [
      ...items,
      {
        id: producto.id,
        sku: producto.sku,
        nombre: producto.nombre,
        marca: producto.marca,
        precio: producto.precio,
        arte: producto.arte,
        stock: producto.stock,
        cantidad: nuevaCantidad
      }
    ];
  }

  persistir();
  return { ok: true, cantidad: nuevaCantidad };
}

/** Cambia la cantidad absoluta de un ítem. `0` o menos lo elimina. */
export function cambiarCantidad(idProducto, cantidad) {
  const item = items.find((it) => it.id === idProducto);
  if (!item) return { ok: false, motivo: 'inexistente' };

  const objetivo = Math.trunc(cantidad);

  if (objetivo <= 0) {
    return quitar(idProducto);
  }

  if (objetivo > item.stock) {
    item.cantidad = item.stock;
    persistir();
    return { ok: false, motivo: 'tope-stock', cantidad: item.stock };
  }

  item.cantidad = objetivo;
  persistir();
  return { ok: true, cantidad: objetivo };
}

export function quitar(idProducto) {
  const antes = items.length;
  items = items.filter((it) => it.id !== idProducto);
  if (items.length === antes) return { ok: false, motivo: 'inexistente' };
  persistir();
  return { ok: true, cantidad: 0 };
}

export function vaciar() {
  items = [];
  persistir();
  return { ok: true };
}

/** Totales recalculados desde los ítems, nunca almacenados. */
export function resumen() {
  const subtotal = items.reduce((acc, it) => acc + it.precio * it.cantidad, 0);
  const unidades = items.reduce((acc, it) => acc + it.cantidad, 0);
  const envio = unidades === 0 || subtotal >= ENVIO_GRATIS_DESDE ? 0 : COSTO_ENVIO;

  return {
    items: obtenerItems(),
    unidades,
    subtotal,
    envio,
    total: subtotal + envio,
    faltaParaEnvioGratis: Math.max(0, ENVIO_GRATIS_DESDE - subtotal)
  };
}
