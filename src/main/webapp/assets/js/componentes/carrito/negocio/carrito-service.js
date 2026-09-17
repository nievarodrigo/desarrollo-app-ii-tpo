/**
 * COMPONENTE CARRITO — capa de negocio y fachada del componente.
 *
 * ══ POR QUÉ ES STATEFUL ════════════════════════════════════════════════════
 *
 * Este es el componente STATEFUL del sistema, y el contraste deliberado con
 * `CatalogoService`, que es stateless. La diferencia no es "guarda cosas en
 * algún lado": es que mantiene **estado conversacional**.
 *
 *   · El resultado de `agregar(producto)` DEPENDE de todo lo que el cliente
 *     hizo antes: si ya tenía 3 unidades y el stock es 3, la operación se
 *     rechaza. La misma llamada, con el mismo argumento, da distinto resultado
 *     según la historia previa.
 *   · El estado pertenece a UN cliente y no se comparte. El carrito de uno no
 *     es el carrito de otro.
 *   · La secuencia importa: agregar → cambiar cantidad → confirmar es una
 *     conversación, no cuatro consultas independientes.
 *
 * En el backend esto se declara `@Stateful` y el contenedor le garantiza que
 * cada cliente hable siempre con SU instancia, pasivándola cuando hace falta.
 * Acá el rol de "contenedor" lo cumple el navegador: la instancia vive mientras
 * viva la pestaña y el repositorio la rehidrata al volver.
 *
 * `CatalogoService`, en cambio, es `@Stateless`: cualquier instancia del pool
 * puede atender cualquier búsqueda porque no hay nada que recordar.
 *
 * ══ PATRÓN OBSERVER ════════════════════════════════════════════════════════
 *
 * `alCambiar()` deja que la presentación se suscriba a los cambios de estado.
 * El negocio NO conoce a la vista: emite y sigue. Por eso la insignia de la
 * cabecera, el panel lateral y los botones de la grilla se actualizan solos sin
 * que este archivo importe una sola línea de DOM.
 *
 * ══ LO QUE ESTE ARCHIVO NO HACE ════════════════════════════════════════════
 *
 * No toca `localStorage` (eso es `CarritoRepositoryLocal`) y no genera HTML
 * (eso es `carrito-view.js`). Las reglas que valida acá son las mismas que
 * después revalidará el `CarritoService` de Jakarta EE: el cliente es cómodo,
 * no es autoridad.
 */

import { CarritoRepositoryLocal } from '../datos/carrito-repository.js';
import { ROLES, autorizar } from '../../usuario/negocio/usuario-service.js';

/**
 * Umbral y costo de envío.
 *
 * Son reglas de negocio, por eso viven en esta capa y no en la vista. En el
 * backend van a salir de una tabla de parámetros; el contrato hacia afuera
 * (`resumen().envio`) no cambia.
 */
export const ENVIO_GRATIS_DESDE = 150000;
export const COSTO_ENVIO = 8900;

/**
 * Reglas del carrito sobre un repositorio inyectado.
 *
 * La dependencia entra por constructor y es un CONTRATO: este service no sabe
 * si atrás hay `localStorage`, una API o un mapa en memoria. Es la misma
 * inyección de dependencias que `@Inject` resuelve del otro lado.
 */
class CarritoService {
  /** @param {{obtener: Function, guardar: Function, limpiar: Function}} repositorio */
  constructor(repositorio) {
    this.repositorio = repositorio;
    this.oyentes = new Set();

    /* El estado conversacional se rehidrata desde el repositorio. Se corrige al
       entrar porque el medio es editable: una cantidad por encima del stock
       guardada a mano no debe entrar al carrito. */
    this.items = repositorio.obtener().map((it) => ({
      ...it,
      cantidad: Math.max(1, Math.min(it.cantidad, Number(it.stock) || 1))
    }));
  }

  /* ----------------------------- Observer ----------------------------- */

  /** Suscripción al estado. Devuelve la función para desuscribirse. */
  alCambiar(fn) {
    this.oyentes.add(fn);
    fn(this.resumen());
    return () => this.oyentes.delete(fn);
  }

  #persistir() {
    this.repositorio.guardar(this.items);
    const instantanea = this.resumen();
    this.oyentes.forEach((fn) => fn(instantanea));
  }

  /* ----------------------------- Consultas ---------------------------- */

  obtenerItems() {
    return this.items.map((it) => ({ ...it }));
  }

  cantidadDe(idProducto) {
    return this.items.find((it) => it.id === idProducto)?.cantidad ?? 0;
  }

  /**
   * Totales SIEMPRE recalculados desde los ítems, nunca almacenados.
   *
   * Guardar un total sería guardar una verdad que puede quedar desactualizada:
   * si cambia un precio o una cantidad, el total mentiría. Es la misma razón
   * por la que el backend recalcula el total en el checkout y no confía en el
   * que le manda el cliente.
   */
  resumen() {
    const subtotal = this.items.reduce((acc, it) => acc + it.precio * it.cantidad, 0);
    const unidades = this.items.reduce((acc, it) => acc + it.cantidad, 0);
    const envio = unidades === 0 || subtotal >= ENVIO_GRATIS_DESDE ? 0 : COSTO_ENVIO;

    return {
      items: this.obtenerItems(),
      unidades,
      subtotal,
      envio,
      total: subtotal + envio,
      faltaParaEnvioGratis: Math.max(0, ENVIO_GRATIS_DESDE - subtotal)
    };
  }

  /* ----------------------------- Comandos ----------------------------- */

  /**
   * Agrega un producto respetando el stock disponible.
   *
   * Devuelve un resultado en vez de lanzar: que el cliente haya llegado al tope
   * de stock no es un error del sistema, es una respuesta legítima del negocio,
   * y la vista necesita poder avisarlo con un tono distinto al de una falla.
   *
   * @returns {{ok:boolean, motivo?:string, cantidad?:number}}
   */
  agregar(producto, cantidad = 1) {
    if (!producto || producto.stock <= 0) {
      return { ok: false, motivo: 'sin-stock' };
    }

    const pedida = Math.max(1, Math.trunc(cantidad));
    const existente = this.items.find((it) => it.id === producto.id);
    const yaEnCarrito = existente?.cantidad ?? 0;

    if (yaEnCarrito >= producto.stock) {
      return { ok: false, motivo: 'tope-stock', cantidad: yaEnCarrito };
    }

    const nuevaCantidad = Math.min(yaEnCarrito + pedida, producto.stock);

    if (existente) {
      existente.cantidad = nuevaCantidad;
      /* Refrescamos precio y stock: el catálogo es la fuente de verdad y pudo
         haber cambiado desde que el ítem entró al carrito. */
      existente.stock = producto.stock;
      existente.precio = producto.precio;
    } else {
      this.items = [
        ...this.items,
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

    this.#persistir();
    return { ok: true, cantidad: nuevaCantidad };
  }

  /** Cambia la cantidad absoluta de un ítem. `0` o menos lo elimina. */
  cambiarCantidad(idProducto, cantidad) {
    const item = this.items.find((it) => it.id === idProducto);
    if (!item) return { ok: false, motivo: 'inexistente' };

    const objetivo = Math.trunc(cantidad);

    if (objetivo <= 0) return this.quitar(idProducto);

    if (objetivo > item.stock) {
      item.cantidad = item.stock;
      this.#persistir();
      return { ok: false, motivo: 'tope-stock', cantidad: item.stock };
    }

    item.cantidad = objetivo;
    this.#persistir();
    return { ok: true, cantidad: objetivo };
  }

  quitar(idProducto) {
    const antes = this.items.length;
    this.items = this.items.filter((it) => it.id !== idProducto);
    if (this.items.length === antes) return { ok: false, motivo: 'inexistente' };
    this.#persistir();
    return { ok: true, cantidad: 0 };
  }

  vaciar() {
    this.items = [];
    this.#persistir();
    return { ok: true };
  }

  /**
   * Cierra la conversación: convierte el carrito en un pedido y lo vacía.
   *
   * Es el único método que TERMINA el estado conversacional, y por eso es el
   * candidato natural a operación sensible del componente. El total se toma del
   * `resumen()` recalculado en el momento, jamás de uno que venga de la vista.
   *
   * Simula `POST /api/checkout`. Cuando exista el componente Pedido, esta
   * llamada se convierte en una transacción declarativa de varios pasos
   * (reservar stock → cobrar → confirmar) con `@TransactionAttribute`.
   */
  confirmar() {
    const resumen = this.resumen();
    if (resumen.unidades === 0) {
      return { ok: false, motivo: 'carrito-vacio' };
    }

    const pedido = {
      codigo: `FCH-${String(Math.floor(Math.random() * 90000) + 10000)}`,
      total: resumen.total,
      unidades: resumen.unidades,
      confirmadoEn: new Date().toISOString()
    };

    this.vaciar();
    return { ok: true, pedido };
  }
}

/* ------------------------------------------------------------------ */
/* Fachada del componente                                             */
/* ------------------------------------------------------------------ */

/**
 * Instancia única con su repositorio ya cableado.
 *
 * Es única A PROPÓSITO y no por comodidad: hay un solo cliente por pestaña, así
 * que hay un solo carrito. Del lado del servidor el contenedor hace lo mismo
 * pero al revés — una instancia `@Stateful` POR cliente — porque allá los
 * clientes son muchos.
 */
const instancia = new CarritoService(new CarritoRepositoryLocal());

export const alCambiar = (fn) => instancia.alCambiar(fn);
export const obtenerItems = () => instancia.obtenerItems();
export const cantidadDe = (id) => instancia.cantidadDe(id);
export const resumen = () => instancia.resumen();
export const agregar = (producto, cantidad) => instancia.agregar(producto, cantidad);
export const cambiarCantidad = (id, cantidad) => instancia.cambiarCantidad(id, cantidad);
export const quitar = (id) => instancia.quitar(id);
export const vaciar = () => instancia.vaciar();

/**
 * OPERACIÓN SENSIBLE · exige sesión activa (CLIENTE o ADMIN).
 *
 * Acá se ve por qué la autorización es un servicio TRANSVERSAL y no algo que
 * cada componente se escribe: la regla la declara el componente Carrito, sobre
 * SU operación, usando la primitiva que expone el componente Usuario. Ninguno
 * de los dos conoce la implementación del otro — Carrito no sabe cómo se
 * verifica una contraseña, Usuario no sabe qué es un ítem.
 *
 * Es el mismo reparto que en Jakarta EE: la anotación `@RolesAllowed` la pone
 * el componente dueño de la operación, y quien la hace cumplir es el contenedor.
 *
 * Al declararse con `autorizar([...])`, TODA invocación pasa por el control: no
 * hay forma de llamar a `confirmarCompra()` sin sesión, ni desde la consola.
 * Lanza `NoAutenticadoError` (401) o `AccesoDenegadoError` (403).
 */
export const confirmarCompra = autorizar([ROLES.CLIENTE, ROLES.ADMIN], () => instancia.confirmar());
