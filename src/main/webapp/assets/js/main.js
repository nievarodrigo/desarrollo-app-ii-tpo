/**
 * GOMARKET — orquestación de la landing.
 *
 * ARQUITECTURA (ver `docs/ARQUITECTURA_FRONT.md`)
 *
 * El código está organizado POR COMPONENTE y, dentro de cada uno, POR CAPA:
 *
 *     componentes/<componente>/presentacion/   ← qué se muestra
 *     componentes/<componente>/negocio/        ← qué reglas se aplican
 *     componentes/<componente>/datos/          ← de dónde salen los datos
 *
 * Espeja la estructura del backend (`src/main/java/.../catalogo/{controller,
 * service,repository}`) a propósito: es la misma arquitectura de las dos manos
 * del cable.
 *
 * Este archivo NO es una capa: es el ARRANQUE de la landing. Monta los tres
 * componentes, les pasa las dependencias que necesitan y maneja lo que no
 * pertenece a ninguno (tema, menú móvil, contador de la promo, newsletter).
 *
 * No contiene reglas de negocio ni datos de productos, y sólo habla con las
 * FACHADAS de los componentes — nunca con un repositorio ni con una
 * implementación concreta.
 */

import * as catalogo from './componentes/catalogo/negocio/catalogo-service.js';
import * as carrito from './componentes/carrito/negocio/carrito-service.js';
import { tarjetaProducto, detalleProducto } from './componentes/catalogo/presentacion/catalogo-view.js';
import { iniciarCarritoView } from './componentes/carrito/presentacion/carrito-view.js';
import { exigirSesion, iniciarUsuarioView } from './componentes/usuario/presentacion/usuario-view.js';
import * as usuario from './componentes/usuario/negocio/usuario-service.js';
import { TEMAS, temaActual, aplicarTema } from './compartido/tema.js';
import { precio, numero } from './compartido/formato.js';
import { arteProducto } from './compartido/arte.js';
import { abrirCapa, cerrarCapa } from './compartido/capa-modal.js';
import { avisar } from './compartido/avisos.js';

/* ------------------------------------------------------------------ */
/* Estado de la vista de catálogo                                      */
/* ------------------------------------------------------------------ */

/**
 * Rango de precios del catálogo. Ya no es sincrónico: lo resuelve la API
 * (`/productos/facetas` con el backend, o el JSON del catálogo sin él), así que
 * se completa durante el arranque, antes de armar el panel de filtros.
 */
let rango = { min: 0, max: 0 };

const filtros = {
  q: '',
  categoria: 'todas',
  orden: 'relevancia',
  soloStock: false,
  precioMax: Infinity
};

let categorias = [];
let productosVisibles = [];
let peticionEnCurso = 0;

/* Cache de productos por id para poder abrir el detalle sin volver a pedir. */
const porId = new Map();

/* ------------------------------------------------------------------ */
/* Utilidades                                                          */
/* ------------------------------------------------------------------ */

const $ = (selector, raiz = document) => raiz.querySelector(selector);
const $$ = (selector, raiz = document) => [...raiz.querySelectorAll(selector)];

function esperar(fn, ms = 260) {
  let id;
  return (...args) => {
    clearTimeout(id);
    id = setTimeout(() => fn(...args), ms);
  };
}

/**
 * Filtros listos para viajar a la API: `precioMax` se omite mientras esté en el
 * tope, porque mandar `Infinity` o el máximo del catálogo no filtra nada y
 * ensucia la query.
 */
function filtrosEfectivos() {
  const { precioMax, ...resto } = filtros;
  return Number.isFinite(precioMax) && precioMax < rango.max
    ? { ...resto, precioMax }
    : resto;
}

function irAlCatalogo() {
  $('#catalogo')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/* ------------------------------------------------------------------ */
/* Tema                                                                */
/* ------------------------------------------------------------------ */

const ICONOS_TEMA = {
  normal: '<svg viewBox="0 0 20 20" focusable="false"><path d="M10 2.5 15.5 8a5.5 5.5 0 1 1-11 0L10 2.5Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  claro: '<svg viewBox="0 0 20 20" focusable="false"><circle cx="10" cy="10" r="3.6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M10 1.6v2M10 16.4v2M1.6 10h2M16.4 10h2M4.1 4.1l1.4 1.4M14.5 14.5l1.4 1.4M15.9 4.1l-1.4 1.4M5.5 14.5l-1.4 1.4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  oscuro: '<svg viewBox="0 0 20 20" focusable="false"><path d="M16.5 12.4A7 7 0 0 1 7.6 3.5a7 7 0 1 0 8.9 8.9Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>'
};

/**
 * Construye TODOS los selectores de tema del documento (el de la cabecera y el
 * del menú móvil) y los mantiene sincronizados entre sí.
 */
function iniciarTemas() {
  const contenedores = $$('[data-temas]');
  if (contenedores.length === 0) return;

  aplicarTema(temaActual());

  contenedores.forEach((contenedor) => {
    contenedor.innerHTML = TEMAS.map(
      (tema) => `
      <button
        type="button"
        role="radio"
        data-tema-id="${tema.id}"
        aria-checked="false"
        tabindex="-1"
        title="Tema ${tema.nombre.toLowerCase()} — ${tema.descripcion}">
        ${ICONOS_TEMA[tema.id]}
        <span class="visualmente-oculto">Tema ${tema.nombre}: ${tema.descripcion}</span>
      </button>`
    ).join('');
  });

  /** Refleja el tema activo en todos los grupos, con tabindex móvil (roving). */
  function pintar(id) {
    $$('[data-tema-id]').forEach((boton) => {
      const activo = boton.dataset.temaId === id;
      boton.setAttribute('aria-checked', String(activo));
      boton.tabIndex = activo ? 0 : -1;
    });
  }

  function seleccionar(id, contenedor, moverFoco) {
    aplicarTema(id);
    pintar(id);
    if (moverFoco && contenedor) {
      $(`[data-tema-id="${id}"]`, contenedor)?.focus();
    }
  }

  pintar(temaActual());

  contenedores.forEach((contenedor) => {
    contenedor.addEventListener('click', (evento) => {
      const boton = evento.target.closest('[data-tema-id]');
      if (boton) seleccionar(boton.dataset.temaId, contenedor, false);
    });

    contenedor.addEventListener('keydown', (evento) => {
      if (!['ArrowRight', 'ArrowLeft', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(evento.key)) return;
      evento.preventDefault();

      const botones = $$('[data-tema-id]', contenedor);
      const indiceActual = botones.findIndex((b) => b.getAttribute('aria-checked') === 'true');
      const paso = ['ArrowRight', 'ArrowDown'].includes(evento.key) ? 1 : -1;

      let siguiente;
      if (evento.key === 'Home') siguiente = 0;
      else if (evento.key === 'End') siguiente = botones.length - 1;
      else siguiente = (indiceActual + paso + botones.length) % botones.length;

      seleccionar(botones[siguiente].dataset.temaId, contenedor, true);
    });
  });
}

/* ------------------------------------------------------------------ */
/* Cabecera                                                            */
/* ------------------------------------------------------------------ */

function iniciarCabecera() {
  const cabecera = $('#cabecera');
  if (cabecera) {
    const revisar = () => {
      cabecera.dataset.elevada = String(window.scrollY > 8);
    };
    revisar();
    window.addEventListener('scroll', revisar, { passive: true });
  }

  /* Menú de cuenta */
  const cuenta = $('[data-cuenta]');
  const botonCuenta = $('[data-cuenta-boton]');
  const panelCuenta = $('[data-cuenta-panel]');

  if (cuenta && botonCuenta && panelCuenta) {
    const cerrar = () => {
      panelCuenta.hidden = true;
      botonCuenta.setAttribute('aria-expanded', 'false');
    };

    botonCuenta.addEventListener('click', () => {
      const abierto = !panelCuenta.hidden;
      panelCuenta.hidden = abierto;
      botonCuenta.setAttribute('aria-expanded', String(!abierto));
      if (!abierto) $('button', panelCuenta)?.focus();
    });

    /* Cerramos el menú cuando se elige una acción. Qué HACE cada acción lo
       resuelve el componente Usuario: acá sólo se administra el desplegable. */
    panelCuenta.addEventListener('click', (evento) => {
      if (!evento.target.closest('[data-cuenta-accion]')) return;
      cerrar();
    });

    document.addEventListener('click', (evento) => {
      if (!panelCuenta.hidden && !cuenta.contains(evento.target)) cerrar();
    });

    document.addEventListener('keydown', (evento) => {
      if (evento.key === 'Escape' && !panelCuenta.hidden) {
        cerrar();
        botonCuenta.focus();
      }
    });
  }

  /* Menú móvil */
  const botonMenu = $('[data-abrir-menu]');
  const menuMovil = $('[data-menu-movil]');

  if (botonMenu && menuMovil) {
    botonMenu.addEventListener('click', () => {
      const abierto = !menuMovil.hidden;
      menuMovil.hidden = abierto;
      botonMenu.setAttribute('aria-expanded', String(!abierto));
    });

    menuMovil.addEventListener('click', (evento) => {
      if (evento.target.closest('a, [data-cuenta-accion]')) {
        menuMovil.hidden = true;
        botonMenu.setAttribute('aria-expanded', 'false');
      }
    });
  }

}

/* ------------------------------------------------------------------ */
/* Hero                                                                */
/* ------------------------------------------------------------------ */

async function iniciarHero() {
  const contenedores = $$('[data-hero-arte]');
  if (contenedores.length === 0) return;

  await Promise.all(
    contenedores.map(async (figura) => {
      try {
        const producto = await catalogo.obtener(Number(figura.dataset.heroArte));
        figura.insertAdjacentHTML('afterbegin', arteProducto(producto));
        const etiqueta = $(`[data-hero-precio="${producto.id}"]`);
        if (etiqueta) etiqueta.textContent = precio(producto.precio);
      } catch {
        /* Si falla, la tarjeta queda con su fondo: nunca rompe el hero. */
      }
    })
  );
}

/* ------------------------------------------------------------------ */
/* Categorías                                                          */
/* ------------------------------------------------------------------ */

async function iniciarCategorias() {
  const lista = $('[data-categorias]');
  if (!lista) return;

  try {
    categorias = await catalogo.listarCategorias();
  } catch {
    lista.innerHTML = '';
    return;
  }

  const { porCategoria: conteo } = await catalogo.contarPorCategoria({});

  lista.innerHTML = categorias
    .map(
      (categoria, indice) => `
        <li>
          <button class="categoria" type="button" data-categoria="${categoria.slug}" aria-pressed="false">
            <span class="categoria__indice">${String(indice + 1).padStart(2, '0')}</span>
            <span class="categoria__cuerpo">
              <span class="categoria__nombre">${categoria.nombre}</span>
              <span class="categoria__bajada">${categoria.bajada}</span>
              <span class="categoria__cantidad">${conteo[categoria.slug] ?? 0} productos</span>
            </span>
          </button>
        </li>`
    )
    .join('');

  lista.addEventListener('click', (evento) => {
    const boton = evento.target.closest('[data-categoria]');
    if (!boton) return;
    const slug = boton.dataset.categoria;
    filtros.categoria = filtros.categoria === slug ? 'todas' : slug;
    sincronizarFiltros();
    cargarCatalogo();
    irAlCatalogo();
  });

  pintarCategoriaActiva();
}

function pintarCategoriaActiva() {
  $$('[data-categoria]').forEach((boton) => {
    boton.setAttribute('aria-pressed', String(boton.dataset.categoria === filtros.categoria));
  });
}

/* ------------------------------------------------------------------ */
/* Filtros                                                             */
/* ------------------------------------------------------------------ */

function iniciarFiltros() {
  const formulario = $('[data-filtros]');
  if (!formulario) return;

  formulario.addEventListener('submit', (e) => e.preventDefault());

  /* Categorías del panel lateral */
  const opciones = $('[data-filtro-categorias]');
  if (opciones) {
    const construir = () => {
      const items = [{ slug: 'todas', nombre: 'Todas' }, ...categorias];
      opciones.innerHTML = items
        .map(
          (c) => `
            <button class="filtro-opcion" type="button" data-filtro-categoria="${c.slug}" aria-pressed="${c.slug === filtros.categoria}">
              <span>${c.nombre}</span>
              <span data-conteo="${c.slug}"></span>
            </button>`
        )
        .join('');
    };
    construir();

    /* Las categorías llegan asincrónicamente; volvemos a construir cuando estén. */
    document.addEventListener('categorias-listas', construir);

    opciones.addEventListener('click', (evento) => {
      const boton = evento.target.closest('[data-filtro-categoria]');
      if (!boton) return;
      filtros.categoria = boton.dataset.filtroCategoria;
      sincronizarFiltros();
      cargarCatalogo();
    });
  }

  /* Precio máximo */
  const deslizador = $('[data-filtro-precio]');
  const salida = $('[data-filtro-precio-valor]');
  if (deslizador && salida && rango.max > 0) {
    deslizador.min = String(Math.floor(rango.min / 1000) * 1000);
    deslizador.max = String(Math.ceil(rango.max / 1000) * 1000);
    deslizador.step = '1000';
    deslizador.value = deslizador.max;
    salida.textContent = precio(Number(deslizador.value));

    const actualizar = esperar(() => {
      filtros.precioMax = Number(deslizador.value);
      cargarCatalogo();
    }, 220);

    deslizador.addEventListener('input', () => {
      salida.textContent = precio(Number(deslizador.value));
      actualizar();
    });
  }

  /* Sólo con stock */
  $('[data-filtro-stock]')?.addEventListener('change', (evento) => {
    filtros.soloStock = evento.target.checked;
    sincronizarFiltros();
    cargarCatalogo();
  });

  /* Orden */
  $('[data-filtro-orden]')?.addEventListener('change', (evento) => {
    filtros.orden = evento.target.value;
    cargarCatalogo();
  });

  /* Limpiar */
  $('[data-limpiar-filtros]')?.addEventListener('click', () => {
    limpiarFiltros();
    avisar('Limpiamos los filtros.', 'ok');
  });

  /* Error simulado, para poder mostrar el estado de error en vivo */
  $('[data-simular-error]')?.addEventListener('click', () => {
    catalogo.forzarProximoError();
    cargarCatalogo();
  });

  /* Panel plegable en pantallas chicas */
  const abrirFiltros = $('[data-abrir-filtros]');
  if (abrirFiltros) {
    abrirFiltros.addEventListener('click', () => {
      const abierto = formulario.dataset.abierto === 'true';
      formulario.dataset.abierto = String(!abierto);
      abrirFiltros.setAttribute('aria-expanded', String(!abierto));
    });
  }

  /* Botón de la promo: ordena por descuento mostrando primero las ofertas */
  $('[data-ir-a-ofertas]')?.addEventListener('click', () => {
    filtros.q = '';
    filtros.categoria = 'oficina';
    filtros.orden = 'relevancia';
    filtros.precioMax = rango.max;
    filtros.soloStock = false;
    sincronizarFiltros();
    cargarCatalogo();
    irAlCatalogo();
  });
}

function limpiarFiltros() {
  filtros.q = '';
  filtros.categoria = 'todas';
  filtros.orden = 'relevancia';
  filtros.soloStock = false;
  filtros.precioMax = rango.max;
  sincronizarFiltros();
  cargarCatalogo();
}

/** Refleja el estado en todos los controles (hay dos buscadores y dos listas). */
function sincronizarFiltros() {
  $$('[data-buscador-input]').forEach((entrada) => {
    if (entrada.value !== filtros.q) entrada.value = filtros.q;
    const limpiar = entrada.parentElement?.querySelector('[data-buscador-limpiar]');
    if (limpiar) limpiar.hidden = filtros.q.length === 0;
  });

  $$('[data-filtro-categoria]').forEach((boton) => {
    boton.setAttribute('aria-pressed', String(boton.dataset.filtroCategoria === filtros.categoria));
  });

  pintarCategoriaActiva();

  const stock = $('[data-filtro-stock]');
  if (stock) stock.checked = filtros.soloStock;

  const orden = $('[data-filtro-orden]');
  if (orden) orden.value = filtros.orden;

  const deslizador = $('[data-filtro-precio]');
  const salida = $('[data-filtro-precio-valor]');
  if (deslizador && salida) {
    deslizador.value = String(filtros.precioMax);
    salida.textContent = precio(filtros.precioMax);
  }

  pintarFichas();
}

/* Fichas de filtros activos */
function pintarFichas() {
  const contenedor = $('[data-fichas]');
  if (!contenedor) return;

  const cruz =
    '<svg viewBox="0 0 12 12" aria-hidden="true" focusable="false"><path d="m3 3 6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>';

  const fichas = [];
  if (filtros.q) fichas.push({ tipo: 'q', texto: `“${filtros.q}”` });
  if (filtros.categoria !== 'todas') {
    const nombre = categorias.find((c) => c.slug === filtros.categoria)?.nombre ?? filtros.categoria;
    fichas.push({ tipo: 'categoria', texto: nombre });
  }
  if (filtros.soloStock) fichas.push({ tipo: 'stock', texto: 'Sólo con stock' });
  if (filtros.precioMax < rango.max) fichas.push({ tipo: 'precio', texto: `Hasta ${precio(filtros.precioMax)}` });

  contenedor.hidden = fichas.length === 0;
  contenedor.innerHTML = fichas
    .map(
      (f) => `<button class="ficha" type="button" data-quitar-ficha="${f.tipo}">
        ${f.texto} ${cruz}<span class="visualmente-oculto">Quitar filtro</span>
      </button>`
    )
    .join('');
}

/* ------------------------------------------------------------------ */
/* Buscador                                                            */
/* ------------------------------------------------------------------ */

function iniciarBuscadores() {
  const buscarConDemora = esperar((valor) => {
    filtros.q = valor;
    sincronizarFiltros();
    cargarCatalogo();
  }, 280);

  $$('[data-buscador-input]').forEach((entrada) => {
    entrada.addEventListener('input', () => {
      const limpiar = entrada.parentElement?.querySelector('[data-buscador-limpiar]');
      if (limpiar) limpiar.hidden = entrada.value.length === 0;
      buscarConDemora(entrada.value.trim());
    });

    entrada.addEventListener('keydown', (evento) => {
      if (evento.key === 'Enter') {
        evento.preventDefault();
        filtros.q = entrada.value.trim();
        sincronizarFiltros();
        cargarCatalogo();
        irAlCatalogo();
      }
      if (evento.key === 'Escape' && entrada.value) {
        evento.preventDefault();
        entrada.value = '';
        filtros.q = '';
        sincronizarFiltros();
        cargarCatalogo();
      }
    });
  });

  $$('[data-buscador-limpiar]').forEach((boton) => {
    boton.addEventListener('click', () => {
      filtros.q = '';
      sincronizarFiltros();
      cargarCatalogo();
      boton.parentElement?.querySelector('input')?.focus();
    });
  });

  $('[data-fichas]')?.addEventListener('click', (evento) => {
    const boton = evento.target.closest('[data-quitar-ficha]');
    if (!boton) return;

    const tipo = boton.dataset.quitarFicha;
    if (tipo === 'q') filtros.q = '';
    if (tipo === 'categoria') filtros.categoria = 'todas';
    if (tipo === 'stock') filtros.soloStock = false;
    if (tipo === 'precio') filtros.precioMax = rango.max;

    sincronizarFiltros();
    cargarCatalogo();
  });

  /* Atajo: "/" enfoca el buscador, como en las tiendas grandes. */
  document.addEventListener('keydown', (evento) => {
    if (evento.key !== '/' || evento.metaKey || evento.ctrlKey) return;
    const activo = document.activeElement;
    if (activo && ['INPUT', 'TEXTAREA', 'SELECT'].includes(activo.tagName)) return;
    evento.preventDefault();
    $('#q-cabecera')?.focus();
  });
}

/* ------------------------------------------------------------------ */
/* Catálogo                                                            */
/* ------------------------------------------------------------------ */

const grilla = $('[data-grilla]');
const estado = $('[data-estado]');
const nota = $('[data-resultado-nota]');

function mostrarEsqueletos(cantidad = 8) {
  if (!grilla) return;
  grilla.setAttribute('aria-busy', 'true');
  grilla.innerHTML = Array.from(
    { length: cantidad },
    () =>
      `<div class="tarjeta-esqueleto" aria-hidden="true">
        <div class="esqueleto"></div><div class="esqueleto"></div>
        <div class="esqueleto"></div><div class="esqueleto"></div><div class="esqueleto"></div>
      </div>`
  ).join('');
  if (estado) estado.hidden = true;
}

function mostrarEstado(tipo) {
  if (!estado || !grilla) return;
  grilla.innerHTML = '';
  estado.hidden = false;
  estado.className = `estado estado--${tipo}`;

  if (tipo === 'error') {
    estado.innerHTML = `
      <span class="estado__icono" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false"><path d="M12 8v5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/><circle cx="12" cy="16.5" r="1.3" fill="currentColor"/><path d="M12 3.2 22 20H2L12 3.2Z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>
      </span>
      <h3>No pudimos cargar el catálogo</h3>
      <p>Puede ser un problema momentáneo de conexión. Probá de nuevo en unos segundos.</p>
      <button class="boton boton--primario" type="button" data-reintentar>Reintentar</button>`;
    $('[data-reintentar]', estado)?.addEventListener('click', () => cargarCatalogo());
    $('[data-reintentar]', estado)?.focus({ preventScroll: true });
  } else {
    estado.innerHTML = `
      <span class="estado__icono" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false"><circle cx="10.5" cy="10.5" r="6.5" fill="none" stroke="currentColor" stroke-width="2"/><path d="m15.5 15.5 5 5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      </span>
      <h3>No encontramos nada con esos filtros</h3>
      <p>Probá con otras palabras, ampliá el precio máximo o limpiá los filtros para ver todo el catálogo.</p>
      <button class="boton boton--primario" type="button" data-limpiar-desde-vacio>Limpiar filtros</button>`;
    $('[data-limpiar-desde-vacio]', estado)?.addEventListener('click', limpiarFiltros);
  }
}

function pintarGrilla() {
  if (!grilla) return;
  grilla.setAttribute('aria-busy', 'false');
  if (estado) estado.hidden = true;

  grilla.innerHTML = productosVisibles
    .map((producto) => tarjetaProducto(producto, carrito.cantidadDe(producto.id)))
    .join('');

  /* Entrada escalonada, discreta. */
  $$('.producto', grilla).forEach((tarjeta, indice) => {
    tarjeta.style.animationDelay = `${Math.min(indice, 11) * 35}ms`;
  });
}

async function cargarCatalogo() {
  const identificador = ++peticionEnCurso;
  mostrarEsqueletos();
  if (nota) nota.textContent = 'Buscando…';

  try {
    const { items, total } = await catalogo.buscar(filtrosEfectivos());
    if (identificador !== peticionEnCurso) return;

    productosVisibles = items;
    items.forEach((p) => porId.set(p.id, p));

    if (nota) {
      nota.textContent =
        total === 0
          ? 'Sin resultados'
          : `${numero(total)} ${total === 1 ? 'producto' : 'productos'}${filtros.q ? ` para “${filtros.q}”` : ''}`;
    }

    actualizarConteos();

    if (total === 0) {
      mostrarEstado('vacio');
      return;
    }
    pintarGrilla();
  } catch (error) {
    if (identificador !== peticionEnCurso) return;
    if (nota) nota.textContent = 'Error al cargar';
    mostrarEstado('error');
    avisar('No pudimos cargar el catálogo.', 'error');
  }
}

/** Cantidad de productos por categoría con los filtros actuales (sin la categoría). */
async function actualizarConteos() {
  try {
    const { total, porCategoria } = await catalogo.contarPorCategoria(filtrosEfectivos());
    $$('[data-conteo]').forEach((nodo) => {
      const slug = nodo.dataset.conteo;
      nodo.textContent = slug === 'todas' ? total : (porCategoria[slug] ?? 0);
    });
  } catch {
    /* Los conteos son un adorno: si fallan, el catálogo sigue funcionando. */
  }
}

/* ------------------------------------------------------------------ */
/* Agregar al carrito y detalle                                        */
/* ------------------------------------------------------------------ */

const modal = $('[data-modal]');
const modalCaja = $('[data-modal-caja]');

async function abrirDetalle(id, disparador) {
  if (!modal || !modalCaja) return;

  let producto = porId.get(Number(id));
  if (!producto) {
    try {
      producto = await catalogo.obtener(id);
      porId.set(producto.id, producto);
    } catch {
      avisar('No pudimos abrir el detalle del producto.', 'error');
      return;
    }
  }

  modalCaja.innerHTML = detalleProducto(producto, carrito.cantidadDe(producto.id));
  abrirCapa(modal, { disparador, focoInicial: modalCaja });
  $('[data-cerrar-modal]', modalCaja)?.addEventListener('click', cerrarCapa);
}

function manejarAgregar(id, origen) {
  const producto = porId.get(Number(id));
  if (!producto) return;

  const resultado = carrito.agregar(producto);

  if (!resultado.ok) {
    const mensajes = {
      'sin-stock': 'Ese producto está sin stock.',
      'tope-stock': 'Ya tenés todo el stock disponible en el carrito.'
    };
    avisar(mensajes[resultado.motivo] ?? 'No pudimos agregarlo.', 'alerta');
  } else {
    avisar(`${producto.nombre} agregado al carrito.`, 'ok');
  }

  /* Refrescamos los botones para reflejar el tope de stock. */
  pintarGrilla();
  if (origen === 'modal' && modalCaja) {
    modalCaja.innerHTML = detalleProducto(producto, carrito.cantidadDe(producto.id));
    $('[data-cerrar-modal]', modalCaja)?.addEventListener('click', cerrarCapa);
  }
}

function iniciarInteraccionesCatalogo() {
  grilla?.addEventListener('click', (evento) => {
    const agregar = evento.target.closest('[data-agregar]');
    if (agregar) {
      manejarAgregar(agregar.dataset.agregar, 'grilla');
      return;
    }
    const ver = evento.target.closest('[data-ver]');
    if (ver) abrirDetalle(ver.dataset.ver, ver);
  });

  modalCaja?.addEventListener('click', (evento) => {
    const agregar = evento.target.closest('[data-agregar]');
    if (agregar) manejarAgregar(agregar.dataset.agregar, 'modal');
  });
}

/* ------------------------------------------------------------------ */
/* Promoción con cuenta regresiva                                      */
/* ------------------------------------------------------------------ */

function iniciarContador() {
  const contenedor = $('[data-contador]');
  if (!contenedor) return;

  const campos = {
    dias: $('[data-contador-dias]'),
    horas: $('[data-contador-horas]'),
    minutos: $('[data-contador-minutos]'),
    segundos: $('[data-contador-segundos]')
  };

  /* Siempre a cuatro días vista, para que la demo no muestre una promo vencida. */
  const fin = new Date();
  fin.setDate(fin.getDate() + 4);
  fin.setHours(23, 59, 59, 0);

  const dos = (n) => String(Math.max(0, n)).padStart(2, '0');

  const tic = () => {
    const restante = Math.max(0, fin - Date.now());
    const seg = Math.floor(restante / 1000);
    campos.dias.textContent = dos(Math.floor(seg / 86400));
    campos.horas.textContent = dos(Math.floor((seg % 86400) / 3600));
    campos.minutos.textContent = dos(Math.floor((seg % 3600) / 60));
    campos.segundos.textContent = dos(seg % 60);
  };

  tic();
  setInterval(tic, 1000);
}

/* ------------------------------------------------------------------ */
/* Newsletter                                                          */
/* ------------------------------------------------------------------ */

function iniciarNewsletter() {
  const formulario = $('[data-newsletter]');
  if (!formulario) return;

  const entrada = $('#email-newsletter', formulario);
  const mensaje = $('[data-newsletter-mensaje]', formulario);
  const boton = $('button[type="submit"]', formulario);
  const patron = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    const valor = entrada.value.trim();

    if (!patron.test(valor)) {
      entrada.setAttribute('aria-invalid', 'true');
      mensaje.dataset.tono = 'error';
      mensaje.textContent = 'Revisá el correo: parece que le falta algo.';
      entrada.focus();
      return;
    }

    entrada.removeAttribute('aria-invalid');
    boton.disabled = true;
    boton.textContent = 'Suscribiendo…';

    setTimeout(() => {
      mensaje.dataset.tono = 'ok';
      mensaje.textContent = '¡Listo! Te vamos a escribir una vez por semana.';
      entrada.value = '';
      boton.disabled = false;
      boton.textContent = 'Suscribirme';
      avisar('Te suscribimos al newsletter.', 'ok');
    }, 700);
  });

  entrada.addEventListener('input', () => {
    if (entrada.getAttribute('aria-invalid') === 'true' && patron.test(entrada.value.trim())) {
      entrada.removeAttribute('aria-invalid');
      mensaje.textContent = '';
      delete mensaje.dataset.tono;
    }
  });
}

/* ------------------------------------------------------------------ */
/* Arranque                                                            */
/* ------------------------------------------------------------------ */

async function iniciar() {
  const anio = $('[data-anio]');
  if (anio) anio.textContent = String(new Date().getFullYear());

  /* Todo lo que no depende de los datos se arma primero: la página queda
     utilizable aunque el catálogo tarde o falle. */
  iniciarTemas();
  iniciarCabecera();
  iniciarBuscadores();
  iniciarInteraccionesCatalogo();
  /* Los tres componentes se montan acá. Cada uno recibe por parámetro lo que
     necesita del afuera, así ninguno importa al otro. */
  iniciarUsuarioView();
  iniciarCarritoView({ alIrAlCatalogo: irAlCatalogo, alExigirSesion: exigirSesion });
  iniciarContador();
  iniciarNewsletter();

  mostrarEsqueletos();

  try {
    rango = await catalogo.rangoDePrecios();
  } catch {
    /* Sin rango el deslizante queda deshabilitado, pero el resto sigue. */
  }
  if (Number.isFinite(rango.max) && rango.max > 0) filtros.precioMax = rango.max;

  iniciarFiltros();

  iniciarHero();
  cargarCatalogo();
  iniciarCategorias().then(() => {
    document.dispatchEvent(new CustomEvent('categorias-listas'));
    sincronizarFiltros();
    actualizarConteos();
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', iniciar);
} else {
  iniciar();
}

/**
 * Expuesto sólo para inspección manual desde la consola.
 *
 * Son las FACHADAS de los tres componentes, no sus tripas: desde la consola se
 * puede ejercitar exactamente el mismo contrato que usa la UI. Sirve para
 * demostrar en vivo, por ejemplo, que `gomarket.carrito.confirmarCompra()` tira
 * 401 sin sesión aunque nunca se toque el botón.
 */
window.gomarket = { catalogo, carrito, usuario, filtros };
