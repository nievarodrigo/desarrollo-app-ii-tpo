/**
 * CAPA DE PRESENTACIÓN del componente Usuario — `UsuarioView`.
 *
 * Menú de cuenta, formularios de ingreso y registro, y panel de administración.
 * Captura datos, los valida a nivel de FORMATO y delega toda decisión a
 * `usuario-service.js`. Acá no hay ni una comparación de rol para autorizar:
 * lo único que hace con los roles es decidir qué botón OFRECER, y eso es
 * usabilidad, no seguridad — la operación sigue protegida por su propia
 * declaración `autorizar([...])` en el negocio.
 *
 * Dos cosas que esta capa NO hace, y conviene tenerlas claras para la defensa:
 *   · no hashea contraseñas (lo hace el service);
 *   · no lee ni escribe `localStorage` (lo hace el repositorio).
 */

import {
  ROLES,
  AccesoDenegadoError,
  DatosInvalidosError,
  NoAutenticadoError,
  alCambiarSesion,
  asegurarSemilla,
  cambiarRol,
  credencialesDeDemo,
  egresar,
  ingresar,
  listarUsuarios,
  puede,
  registrar,
  sesionActual
} from '../negocio/usuario-service.js';
import { abrirCapa, cerrarCapa } from '../../../compartido/capa-modal.js';
import { avisar } from '../../../compartido/avisos.js';
import { ingresarEnBackend, usarToken } from '../../../compartido/cliente-http.js';

const panelCuenta = document.querySelector('[data-cuenta-panel]');
const botonCuenta = document.querySelector('[data-cuenta-boton]');
const modal = document.querySelector('[data-cuenta-modal]');
const caja = document.querySelector('[data-cuenta-caja]');

const ICONO_CERRAR =
  '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m7 7 10 10M17 7 7 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>';

/**
 * Traduce un error del negocio a un mensaje para la persona.
 *
 * La distinción 401 / 403 que hace el service llega hasta acá: no es lo mismo
 * "no sé quién sos" que "sé quién sos y no te alcanza", y el usuario merece
 * saber cuál de las dos le pasó.
 */
function mensajeDeError(error) {
  if (error instanceof NoAutenticadoError) return { texto: error.message, tono: 'alerta' };
  if (error instanceof AccesoDenegadoError) return { texto: error.message, tono: 'error' };
  if (error instanceof DatosInvalidosError) return { texto: error.message, tono: 'alerta' };
  return { texto: 'Algo salió mal. Probá de nuevo.', tono: 'error' };
}

/* ------------------------------------------------------------------ */
/* Menú de cuenta                                                     */
/* ------------------------------------------------------------------ */

function menuInvitado() {
  return `
    <p class="menu-cuenta__saludo">Hola, invitado 👋</p>
    <button role="menuitem" type="button" data-cuenta-accion="ingresar">Ingresar</button>
    <button role="menuitem" type="button" data-cuenta-accion="registro">Crear cuenta</button>`;
}

/** Datos del usuario dentro de HTML: escapar también comillas usadas en atributos. */
function escaparHtml(valor) {
  return String(valor).replace(/[&<>"']/g, (caracter) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[caracter]);
}

function menuAutenticado(sesion) {
  const esAdmin = sesion.rol === ROLES.ADMIN;
  return `
    <p class="menu-cuenta__saludo">
      Hola, <strong>${escaparHtml(sesion.nombre.split(' ')[0])}</strong>
      <span class="chip-rol" data-rol="${escaparHtml(sesion.rol)}">${escaparHtml(sesion.rol)}</span>
    </p>
    <button role="menuitem" type="button" data-cuenta-accion="pedidos">Mis pedidos</button>
    ${esAdmin ? '<button role="menuitem" type="button" data-cuenta-accion="admin">Panel de administración</button>' : ''}
    <button role="menuitem" type="button" data-cuenta-accion="salir">Cerrar sesión</button>`;
}

/**
 * Refleja la sesión en toda la cabecera. Es el suscriptor del OBSERVER del
 * service: cuando alguien entra o sale, esto se redibuja solo.
 */
function pintarMenu(sesion) {
  if (panelCuenta) {
    panelCuenta.innerHTML = sesion ? menuAutenticado(sesion) : menuInvitado();
  }

  if (botonCuenta) {
    const etiqueta = botonCuenta.querySelector('.accion__texto');
    if (etiqueta) etiqueta.textContent = sesion ? sesion.nombre.split(' ')[0] : 'Cuenta';
    botonCuenta.setAttribute(
      'aria-label',
      sesion ? `Cuenta de ${sesion.nombre}, rol ${sesion.rol}` : 'Cuenta, no iniciaste sesión'
    );
    botonCuenta.dataset.autenticado = String(Boolean(sesion));
  }

  /* El acceso del menú móvil cambia de texto y de acción con la sesión. */
  document.querySelectorAll('[data-cuenta-movil]').forEach((boton) => {
    boton.textContent = sesion ? 'Cerrar sesión' : 'Ingresar o crear cuenta';
    boton.dataset.cuentaAccion = sesion ? 'salir' : 'ingresar';
  });
}

/* ------------------------------------------------------------------ */
/* Modal de ingreso y registro                                        */
/* ------------------------------------------------------------------ */

function encabezado(titulo, subtitulo) {
  return `
    <header class="cuenta__cabecera">
      <div>
        <h2 class="cuenta__titulo" id="cuenta-titulo">${titulo}</h2>
        <p class="cuenta__bajada">${subtitulo}</p>
      </div>
      <button class="icono-boton" type="button" data-cuenta-cerrar aria-label="Cerrar">${ICONO_CERRAR}</button>
    </header>`;
}

function campo({ id, etiqueta, tipo = 'text', autocomplete, ayuda = '' }) {
  return `
    <div class="campo">
      <label for="${id}">${etiqueta}</label>
      <input id="${id}" name="${id}" type="${tipo}" ${autocomplete ? `autocomplete="${autocomplete}"` : ''} required>
      <p class="campo__ayuda" data-ayuda="${id}">${ayuda}</p>
    </div>`;
}

function pestanias(activa) {
  const opciones = [
    { id: 'ingresar', texto: 'Ingresar' },
    { id: 'registro', texto: 'Crear cuenta' }
  ];
  return `
    <div class="cuenta__pestanias" role="tablist" aria-label="Ingresar o crear cuenta">
      ${opciones
        .map(
          (o) => `
        <button
          role="tab"
          type="button"
          data-cuenta-pestania="${o.id}"
          aria-selected="${o.id === activa}"
          tabindex="${o.id === activa ? '0' : '-1'}">${o.texto}</button>`
        )
        .join('')}
    </div>`;
}

function bloqueDemo() {
  return `
    <div class="cuenta__demo">
      <p class="cuenta__demo-titulo">Cuentas de prueba</p>
      ${credencialesDeDemo()
        .map(
          (c) => `
        <button type="button" class="cuenta__demo-fila" data-cuenta-demo="${c.email}" data-cuenta-demo-clave="${c.clave}">
          <span class="chip-rol" data-rol="${c.rol}">${c.rol}</span>
          <code>${c.email}</code>
          <span class="cuenta__demo-usar">Usar</span>
        </button>`
        )
        .join('')}
    </div>`;
}

function vistaIngresar() {
  return `
    <div class="cuenta">
      ${encabezado('Ingresá a tu cuenta', 'Necesitás una sesión activa para confirmar una compra.')}
      ${pestanias('ingresar')}
      <form class="cuenta__formulario" data-cuenta-formulario="ingresar" novalidate>
        ${campo({ id: 'ingreso-email', etiqueta: 'Correo electrónico', tipo: 'email', autocomplete: 'email' })}
        ${campo({ id: 'ingreso-clave', etiqueta: 'Contraseña', tipo: 'password', autocomplete: 'current-password' })}
        <p class="campo__ayuda" data-ayuda="general"></p>
        <button class="boton boton--primario boton--ancho boton--grande" type="submit">Ingresar</button>
      </form>
      ${bloqueDemo()}
    </div>`;
}

function vistaRegistro() {
  return `
    <div class="cuenta">
      ${encabezado('Creá tu cuenta', 'Las cuentas nuevas se registran con rol CLIENTE.')}
      ${pestanias('registro')}
      <form class="cuenta__formulario" data-cuenta-formulario="registro" novalidate>
        ${campo({ id: 'registro-nombre', etiqueta: 'Nombre y apellido', autocomplete: 'name' })}
        ${campo({ id: 'registro-email', etiqueta: 'Correo electrónico', tipo: 'email', autocomplete: 'email' })}
        ${campo({
          id: 'registro-clave',
          etiqueta: 'Contraseña',
          tipo: 'password',
          autocomplete: 'new-password',
          ayuda: 'Mínimo 8 caracteres.'
        })}
        <p class="campo__ayuda" data-ayuda="general"></p>
        <button class="boton boton--primario boton--ancho boton--grande" type="submit">Crear mi cuenta</button>
      </form>
    </div>`;
}

/* ------------------------------------------------------------------ */
/* Panel de administración — operación sensible                       */
/* ------------------------------------------------------------------ */

function fechaCorta(iso) {
  try {
    return new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: '2-digit' }).format(
      new Date(iso)
    );
  } catch {
    return '—';
  }
}

/**
 * Renderiza el padrón de cuentas.
 *
 * `listarUsuarios()` está declarado con `autorizar([ROLES.ADMIN], …)`: si esto
 * se invocara sin ser ADMIN, lanzaría antes de devolver un solo dato. No hay un
 * `if (rol === ADMIN)` en esta función — el control está en la declaración de
 * la operación, no en quien la llama.
 */
function vistaAdmin() {
  const usuarios = listarUsuarios();
  const yo = sesionActual();

  return `
    <div class="cuenta">
      ${encabezado('Panel de administración', `Operación restringida al rol ADMIN · ${usuarios.length} cuentas`)}
      <div class="cuenta__tabla-marco">
        <table class="cuenta__tabla">
          <caption class="visualmente-oculto">Cuentas registradas y su rol</caption>
          <thead>
            <tr><th scope="col">Cuenta</th><th scope="col">Alta</th><th scope="col">Rol</th></tr>
          </thead>
          <tbody>
            ${usuarios
              .map(
                (u) => `
              <tr>
                <td>
                  <strong>${escaparHtml(u.nombre)}</strong>
                  <span class="cuenta__email">${escaparHtml(u.email)}${u.email === yo?.email ? ' · vos' : ''}</span>
                </td>
                <td>${fechaCorta(u.creadoEn)}</td>
                <td>
                  <label class="visualmente-oculto" for="rol-${escaparHtml(u.email)}">Rol de ${escaparHtml(u.nombre)}</label>
                  <select id="rol-${escaparHtml(u.email)}" class="cuenta__select" data-cuenta-rol="${escaparHtml(u.email)}">
                    ${Object.values(ROLES)
                      .map((r) => `<option value="${r}" ${r === u.rol ? 'selected' : ''}>${r}</option>`)
                      .join('')}
                  </select>
                </td>
              </tr>`
              )
              .join('')}
          </tbody>
        </table>
      </div>
      <p class="cuenta__nota">
        Esta vista y el cambio de rol están protegidos con
        <code>autorizar([ROLES.ADMIN], …)</code>, el equivalente en el front de
        <code>@RolesAllowed({"ADMIN"})</code>. Ingresá como CLIENTE y el botón que
        abre este panel no aparece; invocar la operación igual devuelve 403.
      </p>
    </div>`;
}

/* ------------------------------------------------------------------ */
/* Orquestación de la capa                                            */
/* ------------------------------------------------------------------ */

let vistaActual = 'ingresar';

/**
 * @param {string} html
 * @param {Element|null} disparador
 * @param {'cuenta'|'admin'} vista el CSS usa esto para elegir el ancho del modal
 */
function mostrar(html, disparador, vista = 'cuenta') {
  if (!modal || !caja) return;
  modal.dataset.vista = vista;
  caja.innerHTML = html;
  abrirCapa(modal, { disparador, focoInicial: caja });
}

export function abrirIngreso(disparador) {
  vistaActual = 'ingresar';
  mostrar(vistaIngresar(), disparador);
}

export function abrirRegistro(disparador) {
  vistaActual = 'registro';
  mostrar(vistaRegistro(), disparador);
}

export function abrirAdmin(disparador) {
  try {
    mostrar(vistaAdmin(), disparador, 'admin');
  } catch (error) {
    /* `listarUsuarios()` lanzó: la operación está protegida en el negocio, así
       que ni siquiera llegamos a tener datos que dibujar. */
    const { texto, tono } = mensajeDeError(error);
    avisar(texto, tono);
  }
}

function ayuda(formulario, campoId, texto, tono) {
  const nodo = formulario.querySelector(`[data-ayuda="${campoId}"]`);
  if (!nodo) return;
  nodo.textContent = texto;
  if (tono) nodo.dataset.tono = tono;
  else delete nodo.dataset.tono;
}

function limpiarAyudas(formulario) {
  formulario.querySelectorAll('[data-ayuda]').forEach((nodo) => {
    if (nodo.dataset.ayuda !== 'registro-clave') nodo.textContent = '';
    delete nodo.dataset.tono;
  });
  formulario.querySelectorAll('[aria-invalid]').forEach((i) => i.removeAttribute('aria-invalid'));
}

async function enviar(formulario) {
  const tipo = formulario.dataset.cuentaFormulario;
  const boton = formulario.querySelector('button[type="submit"]');
  const textoOriginal = boton.textContent;

  limpiarAyudas(formulario);
  boton.disabled = true;
  boton.textContent = tipo === 'ingresar' ? 'Verificando…' : 'Creando la cuenta…';

  try {
    const sesion =
      tipo === 'ingresar'
        ? await ingresar(
            formulario.querySelector('#ingreso-email').value,
            formulario.querySelector('#ingreso-clave').value
          )
        : await registrar({
            nombre: formulario.querySelector('#registro-nombre').value,
            email: formulario.querySelector('#registro-email').value,
            clave: formulario.querySelector('#registro-clave').value
          });

    // Con WildFly, la misma sesión pide su JWT para las operaciones protegidas (comprar).
    // Las cuentas creadas sólo en el navegador no existen en el backend: ahí no hay token.
    const email = formulario.querySelector(tipo === 'ingresar' ? '#ingreso-email' : '#registro-email').value;
    const clave = formulario.querySelector(tipo === 'ingresar' ? '#ingreso-clave' : '#registro-clave').value;
    await ingresarEnBackend(email, clave);

    cerrarCapa();
    avisar(
      tipo === 'ingresar'
        ? `Hola de nuevo, ${sesion.nombre.split(' ')[0]}. Rol ${sesion.rol}.`
        : `Cuenta creada. Ya estás dentro con rol ${sesion.rol}.`,
      'ok'
    );
  } catch (error) {
    const { texto, tono } = mensajeDeError(error);
    const campoId =
      error instanceof DatosInvalidosError && error.campo
        ? { nombre: 'registro-nombre', email: `${tipo === 'ingresar' ? 'ingreso' : 'registro'}-email`, clave: `${tipo === 'ingresar' ? 'ingreso' : 'registro'}-clave` }[error.campo]
        : null;

    ayuda(formulario, campoId ?? 'general', texto, tono);

    const entrada = campoId ? formulario.querySelector(`#${campoId}`) : null;
    if (entrada) {
      entrada.setAttribute('aria-invalid', 'true');
      entrada.focus();
    }
  } finally {
    boton.disabled = false;
    boton.textContent = textoOriginal;
  }
}

/**
 * Conecta la capa. `alPedirIngreso` deja que otros componentes (el carrito, al
 * confirmar una compra) abran el formulario sin importar este módulo entero.
 */
export function iniciarUsuarioView() {
  /* La semilla se prepara al arrancar: si alguien abre el ingreso, las cuentas
     de demostración ya tienen que existir. */
  asegurarSemilla();

  /* Suscripción al OBSERVER del negocio: pinta el menú ahora y en cada cambio. */
  alCambiarSesion(pintarMenu);

  /* Un solo manejador para el menú de escritorio, el móvil y cualquier otro
     disparador que aparezca: delegación de eventos sobre el documento. */
  document.addEventListener('click', (evento) => {
    const disparador = evento.target.closest('[data-cuenta-accion]');
    if (!disparador) return;

    const accion = disparador.dataset.cuentaAccion;

    if (accion === 'ingresar') abrirIngreso(disparador);
    else if (accion === 'registro') abrirRegistro(disparador);
    else if (accion === 'admin') abrirAdmin(disparador);
    else if (accion === 'salir') {
      const nombre = sesionActual()?.nombre.split(' ')[0] ?? '';
      egresar();
      usarToken(null);
      avisar(`Cerraste la sesión${nombre ? `, ${nombre}` : ''}. Tu carrito queda guardado.`, 'alerta');
    } else if (accion === 'pedidos') {
      avisar('El historial de pedidos llega con el componente Pedido.', 'alerta');
    }
  });

  if (!caja) return;

  /* Delegación dentro del modal: pestañas, cierre, credenciales de demo. */
  caja.addEventListener('click', (evento) => {
    if (evento.target.closest('[data-cuenta-cerrar]')) {
      cerrarCapa();
      return;
    }

    const pestania = evento.target.closest('[data-cuenta-pestania]');
    if (pestania) {
      const destino = pestania.dataset.cuentaPestania;
      if (destino === vistaActual) return;
      vistaActual = destino;
      caja.innerHTML = destino === 'ingresar' ? vistaIngresar() : vistaRegistro();
      caja.querySelector('input')?.focus();
      return;
    }

    const demo = evento.target.closest('[data-cuenta-demo]');
    if (demo) {
      const formulario = caja.querySelector('[data-cuenta-formulario="ingresar"]');
      if (!formulario) return;
      formulario.querySelector('#ingreso-email').value = demo.dataset.cuentaDemo;
      formulario.querySelector('#ingreso-clave').value = demo.dataset.cuentaDemoClave;
      limpiarAyudas(formulario);
      formulario.querySelector('button[type="submit"]').focus();
    }
  });

  caja.addEventListener('submit', (evento) => {
    const formulario = evento.target.closest('[data-cuenta-formulario]');
    if (!formulario) return;
    evento.preventDefault();
    enviar(formulario);
  });

  /* Cambio de rol desde el panel de administración. */
  caja.addEventListener('change', (evento) => {
    const select = evento.target.closest('[data-cuenta-rol]');
    if (!select) return;

    try {
      const actualizado = cambiarRol(select.dataset.cuentaRol, select.value);
      avisar(`${actualizado.nombre} ahora es ${actualizado.rol}.`, 'ok');
      caja.innerHTML = vistaAdmin();
    } catch (error) {
      const { texto, tono } = mensajeDeError(error);
      avisar(texto, tono);
      caja.innerHTML = vistaAdmin();
    }
  });
}

/**
 * Puerta que usan los OTROS componentes para exigir una sesión antes de una
 * operación sensible propia. Devuelve `true` si puede seguir; si no, abre el
 * formulario y devuelve `false`.
 *
 * No reemplaza la autorización: el componente que llama sigue declarando su
 * propia regla con `autorizar([...])`. Esto sólo evita el callejón sin salida
 * de mostrar un 401 sin ofrecer la forma de resolverlo.
 */
export function exigirSesion(rolesPermitidos = [], disparador = null) {
  if (puede(rolesPermitidos)) return true;

  if (!sesionActual()) {
    avisar('Necesitás iniciar sesión para continuar.', 'alerta');
    abrirIngreso(disparador);
  } else {
    avisar(`Esta operación es sólo para: ${rolesPermitidos.join(', ')}.`, 'error');
  }

  return false;
}
