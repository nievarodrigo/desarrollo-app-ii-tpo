/**
 * Pruebas del componente USUARIO — capa de negocio.
 *
 * El foco está en la SEGURIDAD, que es lo que este componente aporta al
 * sistema: autenticación, autorización por rol, y que la contraseña nunca se
 * guarde en claro.
 *
 * El hash se prueba de verdad: `crypto.subtle` es el de Node, no un doble. Lo
 * único sustituido es `localStorage`.
 *
 * Correr con:  node --test 'src/test/js/**' + '/*.test.mjs'
 */

import './stub-navegador.mjs';
import { crudoDe, vaciarAlmacen } from './stub-navegador.mjs';

import { test, describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';

import {
  ROLES,
  AccesoDenegadoError,
  DatosInvalidosError,
  NoAutenticadoError,
  alCambiarSesion,
  asegurarSemilla,
  autorizar,
  cambiarRol,
  egresar,
  estaAutenticado,
  ingresar,
  listarUsuarios,
  puede,
  registrar,
  sesionActual
} from '../../main/webapp/assets/js/componentes/usuario/negocio/usuario-service.js';

const CLIENTE = { email: 'cliente@fachada.com.ar', clave: 'cliente123' };
const ADMIN = { email: 'admin@fachada.com.ar', clave: 'admin123' };

beforeEach(async () => {
  egresar();
  await asegurarSemilla();
});

/* ------------------------------------------------------------------ */

describe('Autenticación', () => {
  test('sin sesión no hay nadie autenticado', () => {
    assert.equal(estaAutenticado(), false);
    assert.equal(sesionActual(), null);
  });

  test('las credenciales correctas abren sesión con su rol', async () => {
    const sesion = await ingresar(CLIENTE.email, CLIENTE.clave);
    assert.equal(sesion.rol, ROLES.CLIENTE);
    assert.equal(estaAutenticado(), true);

    await ingresar(ADMIN.email, ADMIN.clave);
    assert.equal(sesionActual().rol, ROLES.ADMIN);
  });

  test('el email no distingue mayúsculas', async () => {
    const sesion = await ingresar('CLIENTE@Fachada.COM.AR', CLIENTE.clave);
    assert.equal(sesion.email, CLIENTE.email);
  });

  test('la contraseña SÍ distingue mayúsculas', async () => {
    await assert.rejects(() => ingresar(CLIENTE.email, 'Cliente123'), DatosInvalidosError);
  });

  test('clave incorrecta y usuario inexistente dan el MISMO mensaje', async () => {
    /* Distinguirlos le confirmaría a un atacante qué correos están registrados. */
    const claveMal = await ingresar(CLIENTE.email, 'incorrecta').catch((e) => e.message);
    const noExiste = await ingresar('fantasma@fachada.com.ar', 'incorrecta').catch((e) => e.message);
    assert.equal(claveMal, noExiste);
  });

  test('un ingreso fallido no abre sesión', async () => {
    await ingresar(CLIENTE.email, 'incorrecta').catch(() => {});
    assert.equal(estaAutenticado(), false);
  });

  test('egresar cierra la sesión', async () => {
    await ingresar(CLIENTE.email, CLIENTE.clave);
    egresar();
    assert.equal(estaAutenticado(), false);
  });

  test('la sesión nunca expone el hash ni el salt', async () => {
    await ingresar(ADMIN.email, ADMIN.clave);
    assert.deepEqual(Object.keys(sesionActual()).sort(), ['email', 'nombre', 'rol']);
  });
});

describe('Contraseñas', () => {
  test('nunca se persiste el texto en claro', async () => {
    await registrar({ nombre: 'Test Uno', email: 'uno@test.com', clave: 'secretaza123' });
    const crudo = crudoDe('usuarios');
    assert.ok(!crudo.includes('secretaza123'), 'la contraseña no aparece en el almacenamiento');
    assert.ok(crudo.includes('hash'), 'sí aparece el hash');
  });

  test('dos usuarios con la misma contraseña tienen hashes distintos', async () => {
    await registrar({ nombre: 'Test Dos', email: 'dos@test.com', clave: 'lamismaclave' });
    await registrar({ nombre: 'Test Tres', email: 'tres@test.com', clave: 'lamismaclave' });

    await ingresar(ADMIN.email, ADMIN.clave);
    const crudo = JSON.parse(crudoDe('usuarios'));
    const dos = crudo.find((u) => u.email === 'dos@test.com');
    const tres = crudo.find((u) => u.email === 'tres@test.com');

    assert.notEqual(dos.clave.salt, tres.clave.salt, 'cada usuario tiene su salt');
    assert.notEqual(dos.clave.hash, tres.clave.hash, 'y por eso distinto hash');
  });

  test('el hash es SHA-256 real, no el degradado', async () => {
    await registrar({ nombre: 'Test Hash', email: 'hash@test.com', clave: 'claveparahash' });
    const crudo = JSON.parse(crudoDe('usuarios'));
    const { hash } = crudo.find((u) => u.email === 'hash@test.com').clave;
    assert.match(hash, /^[0-9a-f]{64}$/, '64 caracteres hexadecimales');
  });
});

describe('Registro', () => {
  test('una cuenta nueva siempre nace CLIENTE', async () => {
    const sesion = await registrar({ nombre: 'Nadia Pérez', email: 'nadia@test.com', clave: 'clavelarga1' });
    assert.equal(sesion.rol, ROLES.CLIENTE, 'nadie se auto-promueve a ADMIN');
  });

  test('registrarse deja la sesión abierta', async () => {
    await registrar({ nombre: 'Leo Díaz', email: 'leo@test.com', clave: 'clavelarga1' });
    assert.equal(estaAutenticado(), true);
  });

  test('rechaza nombre, email y clave inválidos, señalando el campo', async () => {
    const casos = [
      [{ nombre: 'X', email: 'a@b.com', clave: 'clavelarga1' }, 'nombre'],
      [{ nombre: 'Nombre Ok', email: 'sin-arroba', clave: 'clavelarga1' }, 'email'],
      [{ nombre: 'Nombre Ok', email: 'a@b.com', clave: 'corta' }, 'clave']
    ];

    for (const [datos, campo] of casos) {
      const error = await registrar(datos).catch((e) => e);
      assert.ok(error instanceof DatosInvalidosError, `${campo} debía fallar`);
      assert.equal(error.campo, campo);
    }
  });

  test('no permite dos cuentas con el mismo correo', async () => {
    await registrar({ nombre: 'Primera Vez', email: 'repetido@test.com', clave: 'clavelarga1' });
    egresar();
    const error = await registrar({ nombre: 'Segunda Vez', email: 'repetido@test.com', clave: 'otraclave1' }).catch((e) => e);
    assert.ok(error instanceof DatosInvalidosError);
    assert.equal(error.campo, 'email');
  });

  test('un registro rechazado no crea la cuenta', async () => {
    await registrar({ nombre: 'Z', email: 'nocreada@test.com', clave: 'clavelarga1' }).catch(() => {});
    egresar();
    await assert.rejects(() => ingresar('nocreada@test.com', 'clavelarga1'), DatosInvalidosError);
  });
});

describe('Autorización declarativa', () => {
  test('sin sesión, una operación protegida lanza 401', () => {
    assert.throws(() => listarUsuarios(), NoAutenticadoError);
  });

  test('con el rol equivocado, lanza 403 y dice qué roles hacen falta', async () => {
    await ingresar(CLIENTE.email, CLIENTE.clave);

    let error;
    try {
      listarUsuarios();
    } catch (e) {
      error = e;
    }

    assert.ok(error instanceof AccesoDenegadoError, 'es un 403, no un 401');
    assert.deepEqual(error.rolesPermitidos, [ROLES.ADMIN]);
  });

  test('con el rol correcto, la operación corre', async () => {
    await ingresar(ADMIN.email, ADMIN.clave);
    const usuarios = listarUsuarios();
    assert.ok(usuarios.length >= 2);
  });

  test('la operación autorizada tampoco filtra material de credenciales', async () => {
    await ingresar(ADMIN.email, ADMIN.clave);

    /* Se comparan las CLAVES de cada objeto, no substrings del JSON: hay
       cuentas de prueba cuyo email contiene la palabra "hash". */
    for (const usuario of listarUsuarios()) {
      assert.deepEqual(
        Object.keys(usuario).sort(),
        ['creadoEn', 'email', 'nombre', 'rol'],
        'autorizar la operación no es lo mismo que exponer todo el registro'
      );
    }
  });

  test('roles vacíos = cualquier sesión válida alcanza', async () => {
    const operacion = autorizar([], () => 'listo');
    assert.throws(() => operacion(), NoAutenticadoError);

    await ingresar(CLIENTE.email, CLIENTE.clave);
    assert.equal(operacion(), 'listo');
  });

  test('la operación protegida recibe sus argumentos y devuelve su valor', async () => {
    const sumar = autorizar([ROLES.CLIENTE], (a, b) => a + b);
    await ingresar(CLIENTE.email, CLIENTE.clave);
    assert.equal(sumar(2, 3), 5);
  });

  test('el control se aplica en CADA invocación, no sólo en la primera', async () => {
    await ingresar(ADMIN.email, ADMIN.clave);
    assert.ok(listarUsuarios().length >= 2);
    egresar();
    assert.throws(() => listarUsuarios(), NoAutenticadoError, 'al cerrar sesión deja de estar permitida');
  });

  test('puede() responde sin ejecutar la operación', async () => {
    assert.equal(puede([ROLES.ADMIN]), false);
    await ingresar(CLIENTE.email, CLIENTE.clave);
    assert.equal(puede([ROLES.ADMIN]), false);
    assert.equal(puede([ROLES.CLIENTE]), true);
    assert.equal(puede([]), true);
  });
});

describe('Cambio de rol', () => {
  test('un ADMIN puede promover a otra cuenta', async () => {
    await registrar({ nombre: 'Futura Admin', email: 'futura@test.com', clave: 'clavelarga1' });
    egresar();
    await ingresar(ADMIN.email, ADMIN.clave);

    const actualizado = cambiarRol('futura@test.com', ROLES.ADMIN);
    assert.equal(actualizado.rol, ROLES.ADMIN);

    egresar();
    const sesion = await ingresar('futura@test.com', 'clavelarga1');
    assert.equal(sesion.rol, ROLES.ADMIN, 'el rol nuevo persiste');
  });

  test('un CLIENTE no puede cambiar roles', async () => {
    await ingresar(CLIENTE.email, CLIENTE.clave);
    assert.throws(() => cambiarRol(CLIENTE.email, ROLES.ADMIN), AccesoDenegadoError);
  });

  test('un ADMIN no puede degradarse a sí mismo', async () => {
    await ingresar(ADMIN.email, ADMIN.clave);
    assert.throws(() => cambiarRol(ADMIN.email, ROLES.CLIENTE), DatosInvalidosError);
    assert.equal(sesionActual().rol, ROLES.ADMIN, 'sigue siendo ADMIN');
  });

  test('rechaza roles que no existen y cuentas que no existen', async () => {
    await ingresar(ADMIN.email, ADMIN.clave);
    assert.throws(() => cambiarRol(CLIENTE.email, 'SUPERUSUARIO'), DatosInvalidosError);
    assert.throws(() => cambiarRol('fantasma@test.com', ROLES.ADMIN), DatosInvalidosError);
  });
});

describe('Observer de sesión', () => {
  test('notifica al suscribirse y en cada cambio', async () => {
    const vistas = [];
    const desuscribir = alCambiarSesion((s) => vistas.push(s?.rol ?? 'sin-sesion'));

    await ingresar(CLIENTE.email, CLIENTE.clave);
    egresar();
    desuscribir();
    await ingresar(ADMIN.email, ADMIN.clave);

    assert.deepEqual(vistas, ['sin-sesion', ROLES.CLIENTE, 'sin-sesion'], 'después de desuscribirse no llega nada');
  });
});

describe('Persistencia', () => {
  test('la sesión sobrevive en el almacenamiento, sin credenciales', async () => {
    await ingresar(CLIENTE.email, CLIENTE.clave);
    const crudo = crudoDe('sesion');

    assert.ok(crudo.includes(CLIENTE.email), 'se guardó quién está autenticado');
    assert.ok(!crudo.includes('hash'), 'pero no el material de credenciales');
    assert.ok(!crudo.includes(CLIENTE.clave));
  });

  test('egresar borra la sesión del almacenamiento', async () => {
    await ingresar(CLIENTE.email, CLIENTE.clave);
    egresar();
    assert.equal(crudoDe('sesion'), null);
  });
});
