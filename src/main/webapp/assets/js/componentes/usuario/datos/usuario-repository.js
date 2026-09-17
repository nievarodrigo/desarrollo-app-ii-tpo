/**
 * CAPA DE DATOS del componente Usuario — patrón Repository / DAO.
 *
 * Dos agregados, dos responsabilidades, un solo medio de persistencia:
 *
 *   · USUARIOS — el padrón de cuentas registradas.
 *   · SESIÓN   — quién está autenticado AHORA en esta pestaña.
 *
 * Van juntos en un repositorio porque son el mismo agregado desde el punto de
 * vista del negocio (una sesión no existe sin su usuario), y porque el día que
 * esto sea real los dos salen del mismo lugar: la tabla `usuario` y el
 * `HttpSession` que administra el contenedor.
 *
 * Este repositorio NO valida contraseñas ni decide si alguien puede entrar: eso
 * es negocio. Guarda y devuelve. Lo único que hace de más es normalizar el
 * email a minúsculas, porque eso es parte de la FORMA de la clave primaria, no
 * una regla de autorización.
 *
 * ⚠ Sobre las contraseñas: acá se guarda SIEMPRE un hash con salt
 * (`clave: {hash, salt}`), nunca el texto en claro. El hash lo calcula el
 * service; este módulo ni lo mira. Ver la nota de `usuario-service.js` sobre
 * por qué SHA-256 en el front es una demostración y no la solución final.
 *
 *     buscarPorEmail(email) → Usuario|null
 *     buscarTodos()         → Usuario[]
 *     guardar(usuario)      → Usuario
 *     obtenerSesion()       → Sesion|null
 *     guardarSesion(sesion) → void
 *     borrarSesion()        → void
 *
 * @typedef {object} Usuario
 * @property {string} email
 * @property {string} nombre
 * @property {string} rol            'CLIENTE' | 'ADMIN'
 * @property {{hash:string, salt:string}} clave
 * @property {string} creadoEn       ISO 8601
 *
 * @typedef {object} Sesion
 * @property {string} email
 * @property {string} nombre
 * @property {string} rol
 * @property {string} iniciadaEn     ISO 8601
 */

import { leer, escribir, borrar } from '../../../compartido/almacenamiento.js';

const CLAVE_USUARIOS = 'usuarios';
const CLAVE_SESION = 'sesion';

export class UsuarioRepositoryLocal {
  /* ------------------------------ Usuarios ------------------------------ */

  buscarTodos() {
    const crudo = leer(CLAVE_USUARIOS, []);
    if (!Array.isArray(crudo)) return [];

    return crudo.filter(
      (u) => u && typeof u.email === 'string' && u.clave && typeof u.clave.hash === 'string'
    );
  }

  /** @returns {Usuario|null} */
  buscarPorEmail(email) {
    const buscado = String(email ?? '').trim().toLowerCase();
    return this.buscarTodos().find((u) => u.email === buscado) ?? null;
  }

  /** Inserta o reemplaza por email (upsert). */
  guardar(usuario) {
    const normalizado = { ...usuario, email: usuario.email.trim().toLowerCase() };
    const resto = this.buscarTodos().filter((u) => u.email !== normalizado.email);
    escribir(CLAVE_USUARIOS, [...resto, normalizado]);
    return normalizado;
  }

  existeAlguno() {
    return this.buscarTodos().length > 0;
  }

  /* ------------------------------- Sesión ------------------------------- */

  /** @returns {Sesion|null} */
  obtenerSesion() {
    const sesion = leer(CLAVE_SESION, null);
    return sesion && typeof sesion.email === 'string' ? sesion : null;
  }

  guardarSesion(sesion) {
    escribir(CLAVE_SESION, sesion);
  }

  borrarSesion() {
    borrar(CLAVE_SESION);
  }
}
