/**
 * COMPONENTE USUARIO — capa de negocio y fachada del componente.
 *
 * Registro, autenticación, sesión y AUTORIZACIÓN POR ROL. Es el componente que
 * habilita el requisito de "seguridad declarativa" de la consigna, y el que le
 * da a los otros dos componentes la herramienta para protegerse.
 *
 * ══ SEGURIDAD DECLARATIVA: el equivalente de @RolesAllowed ═════════════════
 *
 * En Jakarta EE uno no escribe `if (usuario.rol == ADMIN)` dentro del método.
 * Anota:
 *
 *     @RolesAllowed({"ADMIN"})
 *     public List<Usuario> listarUsuarios() { ... }
 *
 * y el CONTENEDOR intercepta la llamada antes de que el método corra. La regla
 * de seguridad queda DECLARADA junto a la operación, separada de su lógica, y
 * no se puede olvidar de aplicarla en una rama del `if`.
 *
 * Acá se reproduce exactamente eso con `autorizar()`, que envuelve la operación
 * en el momento de declararla:
 *
 *     export const listarUsuarios = autorizar([ROLES.ADMIN], async () => { ... });
 *
 * La diferencia con un `if` desperdigado es la que importa: la regla se lee de
 * un vistazo en la firma, vale para TODAS las invocaciones, y la operación de
 * negocio queda limpia de código de seguridad. Cuando esto pase al backend,
 * `autorizar([ROLES.ADMIN], …)` se borra y se pone `@RolesAllowed({"ADMIN"})`:
 * el que cambia es el ejecutor, no el diseño.
 *
 * ══ QUÉ ES Y QUÉ NO ES ESTA CAPA ═══════════════════════════════════════════
 *
 * Un front NUNCA es una frontera de seguridad: cualquiera puede editar
 * `localStorage` y darse el rol ADMIN. Esto no es un descuido, es la naturaleza
 * del cliente, y hay que decirlo en la defensa: lo que se demuestra acá es el
 * DISEÑO de la autorización (dónde se declara, cómo se propaga, qué error
 * produce). La autoridad real vive en el servidor, que revalida todo — igual
 * que el carrito, que calcula totales para que el usuario vea algo, pero el
 * total que vale es el que recalcula el backend.
 *
 * Los errores que lanza son a propósito los mismos dos casos que distingue
 * HTTP, porque son problemas distintos:
 *
 *     NoAutenticadoError  → 401: no sé quién sos
 *     AccesoDenegadoError → 403: sé quién sos y no te alcanza
 */

import { UsuarioRepositoryLocal } from '../datos/usuario-repository.js';

/** Roles del sistema. Mismos nombres que usarán los `@RolesAllowed` del backend. */
export const ROLES = Object.freeze({
  CLIENTE: 'CLIENTE',
  ADMIN: 'ADMIN'
});

/** 401 — la operación exige una sesión y no hay ninguna. */
export class NoAutenticadoError extends Error {
  constructor(mensaje = 'Necesitás iniciar sesión para hacer esto.') {
    super(mensaje);
    this.name = 'NoAutenticadoError';
  }
}

/** 403 — hay sesión, pero el rol no alcanza. */
export class AccesoDenegadoError extends Error {
  constructor(rolesPermitidos = []) {
    super(`Esta operación es sólo para: ${rolesPermitidos.join(', ')}.`);
    this.name = 'AccesoDenegadoError';
    this.rolesPermitidos = rolesPermitidos;
  }
}

/** Error de validación del registro o del ingreso. */
export class DatosInvalidosError extends Error {
  constructor(mensaje, campo = null) {
    super(mensaje);
    this.name = 'DatosInvalidosError';
    this.campo = campo;
  }
}

/* ------------------------------------------------------------------ */
/* Hash de contraseñas                                                */
/* ------------------------------------------------------------------ */

/**
 * SHA-256 con salt por usuario.
 *
 * ¿Por qué no bcrypt o Argon2, que es lo correcto? Porque son deliberadamente
 * lentos y eso sólo tiene sentido del lado del servidor; en el navegador no hay
 * forma honesta de hacerlo bien. Lo que SÍ se sostiene y es el punto de esta
 * decisión: la contraseña **nunca** se persiste en claro, y dos usuarios con la
 * misma contraseña producen hashes distintos porque cada uno tiene su salt.
 * Cuando el registro pase a `POST /api/usuarios`, el hash lo calcula el backend
 * con Argon2 y este módulo deja de hashear.
 */
async function hashear(clave, salt) {
  const datos = new TextEncoder().encode(`${salt}:${clave}`);

  if (globalThis.crypto?.subtle) {
    const digest = await globalThis.crypto.subtle.digest('SHA-256', datos);
    return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
  }

  /* `crypto.subtle` sólo existe en contexto seguro (https o localhost). Si la
     página se abrió con file://, degradamos a un hash NO criptográfico para que
     la demo siga funcionando. Sirve para no guardar texto en claro; no sirve
     para nada más, y por eso queda marcado. */
  let h = 0x811c9dc5;
  for (const byte of datos) {
    h = ((h ^ byte) * 0x01000193) >>> 0;
  }
  return `fnv1a-inseguro:${h.toString(16)}`;
}

function nuevoSalt() {
  const bytes = new Uint8Array(16);
  if (globalThis.crypto?.getRandomValues) {
    globalThis.crypto.getRandomValues(bytes);
  } else {
    for (let i = 0; i < bytes.length; i += 1) bytes[i] = Math.floor(Math.random() * 256);
  }
  return [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/* ------------------------------------------------------------------ */
/* Estado de sesión                                                   */
/* ------------------------------------------------------------------ */

const repositorio = new UsuarioRepositoryLocal();
const oyentes = new Set();

/** @type {import('../datos/usuario-repository.js').Sesion|null} */
let sesion = repositorio.obtenerSesion();

function emitir() {
  const instantanea = sesionActual();
  oyentes.forEach((fn) => fn(instantanea));
}

/** Patrón OBSERVER, igual que en el carrito: el negocio no conoce la vista. */
export function alCambiarSesion(fn) {
  oyentes.add(fn);
  fn(sesionActual());
  return () => oyentes.delete(fn);
}

/** Sesión activa, sin exponer nunca el hash ni el salt. @returns {object|null} */
export function sesionActual() {
  return sesion ? { email: sesion.email, nombre: sesion.nombre, rol: sesion.rol } : null;
}

export function estaAutenticado() {
  return sesion !== null;
}

export function tieneRol(rol) {
  return sesion?.rol === rol;
}

/* ------------------------------------------------------------------ */
/* AUTORIZACIÓN DECLARATIVA                                           */
/* ------------------------------------------------------------------ */

/**
 * Envuelve una operación con su regla de autorización. El análogo de
 * `@RolesAllowed`: la regla se declara junto a la operación y se aplica antes
 * de que el cuerpo corra.
 *
 * Lo usa este componente para sus propias operaciones y lo usan los OTROS
 * componentes para proteger las suyas — la seguridad es un servicio
 * transversal, igual que en el contenedor.
 *
 * @template {(...args: any[]) => any} F
 * @param {string[]} rolesPermitidos roles admitidos; vacío = cualquier sesión válida
 * @param {F} operacion
 * @returns {F} la misma operación, con el control de acceso ya aplicado
 */
export function autorizar(rolesPermitidos, operacion) {
  return function operacionAutorizada(...args) {
    if (!sesion) throw new NoAutenticadoError();
    if (rolesPermitidos.length > 0 && !rolesPermitidos.includes(sesion.rol)) {
      throw new AccesoDenegadoError(rolesPermitidos);
    }
    return operacion.apply(this, args);
  };
}

/**
 * Verificación imperativa, para cuando la presentación necesita DECIDIR QUÉ
 * DIBUJAR antes de invocar nada (mostrar u ocultar el panel de administración,
 * por ejemplo). No reemplaza a `autorizar()`: la operación sigue protegida por
 * su propia declaración, esto sólo evita ofrecerle al usuario un botón que le
 * va a dar 403.
 */
export function puede(rolesPermitidos = []) {
  if (!sesion) return false;
  return rolesPermitidos.length === 0 || rolesPermitidos.includes(sesion.rol);
}

/* ------------------------------------------------------------------ */
/* Registro e ingreso                                                 */
/* ------------------------------------------------------------------ */

const PATRON_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const CLAVE_MINIMA = 8;

/**
 * Cuentas de demostración, sembradas la primera vez que se usa el componente.
 *
 * Existen para que la defensa oral pueda mostrar los dos caminos de
 * autorización en vivo sin registrar nada a mano. En un sistema real esto sería
 * un script de migración, no código de aplicación.
 */
const SEMILLA = [
  { nombre: 'Ana Gómez', email: 'cliente@gomarket.com.ar', clave: 'cliente123', rol: ROLES.CLIENTE },
  { nombre: 'Rodrigo Nieva', email: 'admin@gomarket.com.ar', clave: 'admin123', rol: ROLES.ADMIN }
];

/** @type {Promise<void>|null} */
let semillaLista = null;

export function asegurarSemilla() {
  if (semillaLista) return semillaLista;

  semillaLista = (async () => {
    if (repositorio.existeAlguno()) return;

    for (const { nombre, email, clave, rol } of SEMILLA) {
      const salt = nuevoSalt();
      repositorio.guardar({
        nombre,
        email,
        rol,
        clave: { hash: await hashear(clave, salt), salt },
        creadoEn: new Date().toISOString()
      });
    }
  })();

  return semillaLista;
}

/** Las credenciales de demo, para mostrarlas en el formulario de ingreso. */
export function credencialesDeDemo() {
  return SEMILLA.map(({ email, clave, rol }) => ({ email, clave, rol }));
}

/**
 * Registra una cuenta nueva. Siempre con rol CLIENTE: un usuario no se puede
 * auto-promover a ADMIN, eso lo hace un administrador.
 *
 * @throws {DatosInvalidosError}
 */
export async function registrar({ nombre, email, clave } = {}) {
  await asegurarSemilla();

  const nombreLimpio = String(nombre ?? '').trim();
  const emailLimpio = String(email ?? '').trim().toLowerCase();

  if (nombreLimpio.length < 2) {
    throw new DatosInvalidosError('Escribí tu nombre completo.', 'nombre');
  }
  if (!PATRON_EMAIL.test(emailLimpio)) {
    throw new DatosInvalidosError('Revisá el correo: parece que le falta algo.', 'email');
  }
  if (String(clave ?? '').length < CLAVE_MINIMA) {
    throw new DatosInvalidosError(`La contraseña necesita al menos ${CLAVE_MINIMA} caracteres.`, 'clave');
  }
  if (repositorio.buscarPorEmail(emailLimpio)) {
    throw new DatosInvalidosError('Ya existe una cuenta con ese correo.', 'email');
  }

  const salt = nuevoSalt();
  const usuario = repositorio.guardar({
    nombre: nombreLimpio,
    email: emailLimpio,
    rol: ROLES.CLIENTE,
    clave: { hash: await hashear(clave, salt), salt },
    creadoEn: new Date().toISOString()
  });

  abrirSesion(usuario);
  return sesionActual();
}

/**
 * Autentica y abre sesión.
 *
 * El mensaje de error es el MISMO para "no existe el usuario" y para
 * "contraseña incorrecta". Es deliberado: distinguirlos le confirma a un
 * atacante qué correos están registrados.
 *
 * @throws {DatosInvalidosError}
 */
export async function ingresar(email, clave) {
  await asegurarSemilla();

  const usuario = repositorio.buscarPorEmail(email);
  const generico = new DatosInvalidosError('Correo o contraseña incorrectos.', 'clave');

  if (!usuario) throw generico;

  const hash = await hashear(clave ?? '', usuario.clave.salt);
  if (hash !== usuario.clave.hash) throw generico;

  abrirSesion(usuario);
  return sesionActual();
}

function abrirSesion(usuario) {
  sesion = {
    email: usuario.email,
    nombre: usuario.nombre,
    rol: usuario.rol,
    iniciadaEn: new Date().toISOString()
  };
  repositorio.guardarSesion(sesion);
  emitir();
}

export function egresar() {
  sesion = null;
  repositorio.borrarSesion();
  emitir();
}

/* ------------------------------------------------------------------ */
/* Operaciones sensibles del componente                               */
/* ------------------------------------------------------------------ */

/**
 * OPERACIÓN SENSIBLE · sólo ADMIN.
 *
 * Padrón de cuentas. Nunca devuelve el hash ni el salt: que la operación esté
 * autorizada no significa que tenga que exponer el material de credenciales.
 * Autorización y mínimo privilegio son dos controles distintos.
 */
export const listarUsuarios = autorizar([ROLES.ADMIN], () =>
  repositorio.buscarTodos().map(({ nombre, email, rol, creadoEn }) => ({ nombre, email, rol, creadoEn }))
);

/**
 * OPERACIÓN SENSIBLE · sólo ADMIN.
 *
 * Cambia el rol de una cuenta. Es la operación más peligrosa del componente
 * (quien la ejecuta puede fabricar administradores), así que además del rol
 * valida que el ADMIN no se quite a sí mismo el privilegio y quede el sistema
 * sin nadie que pueda administrarlo.
 */
export const cambiarRol = autorizar([ROLES.ADMIN], (email, rolNuevo) => {
  if (!Object.values(ROLES).includes(rolNuevo)) {
    throw new DatosInvalidosError(`Rol desconocido: ${rolNuevo}`, 'rol');
  }

  const usuario = repositorio.buscarPorEmail(email);
  if (!usuario) throw new DatosInvalidosError('No existe esa cuenta.', 'email');

  if (usuario.email === sesion.email && rolNuevo !== ROLES.ADMIN) {
    throw new DatosInvalidosError('No podés quitarte a vos mismo el rol de administrador.', 'rol');
  }

  repositorio.guardar({ ...usuario, rol: rolNuevo });

  if (usuario.email === sesion.email) {
    abrirSesion({ ...usuario, rol: rolNuevo });
  }

  return { nombre: usuario.nombre, email: usuario.email, rol: rolNuevo };
});
