# Arquitectura del front — 3 componentes en capas

> Documento de apoyo para la **Entrega Obligatoria N.º 1 (1.º Parcial)**.
> Unidades I – II – III · Comisión Lunes TM · 2.º Cuatrimestre 2026

Este documento explica cómo está construida la capa cliente de **Fachada** y por
qué está construida así. La tesis es una sola:

> **Un componente no es una pantalla.** El catálogo, el carrito y la cuenta no
> son tres vistas de una landing: son tres componentes con interfaz explícita y
> arquitectura en capas, que además se pueden ejercitar y probar por separado.

---

## 1. Los tres componentes

Son los mismos que la cátedra identificó para el caso "tienda online" en la
Clase 1 y retomó en la Clase 3 (slide 4). De los cuatro de esa lista,
implementamos tres; `PagoComponent` queda para la entrega siguiente.

| Componente | Responsabilidad | Estado | Fachada (única puerta de entrada) |
|---|---|---|---|
| **CatálogoComponent** | Mostrar productos, categorías y stock; buscar, filtrar, ordenar | **Stateless** | `componentes/catalogo/negocio/catalogo-service.js` |
| **CarritoComponent** | Agregar, quitar, calcular totales y confirmar la compra | **Stateful** | `componentes/carrito/negocio/carrito-service.js` |
| **UsuarioComponent** | Registro, autenticación, sesión y autorización por rol | Sesión | `componentes/usuario/negocio/usuario-service.js` |

Cada uno expone **una interfaz** y esconde todo lo demás. La presentación importa
la fachada y nada más: nunca un repositorio, nunca una implementación concreta.

---

## 2. Las capas

La estructura de carpetas ES la arquitectura. No hay que creernos el diagrama:
se abre la carpeta y las capas están ahí.

```text
assets/js/
├── componentes/
│   ├── catalogo/
│   │   ├── presentacion/  catalogo-view.js              ← qué se muestra
│   │   ├── negocio/       catalogo-service.js           ← contrato + FACTORY
│   │   │                  catalogo-service-local.js     ← reglas propias
│   │   │                  catalogo-service-remoto.js    ← ADAPTER al backend
│   │   │                  criterios-de-busqueda.js      ← objeto de valor
│   │   └── datos/         producto-repository.js        ← DAO
│   │                      categoria-repository.js       ← DAO
│   │                      catalogo-json.js              ← la fuente (DataSource)
│   ├── carrito/
│   │   ├── presentacion/  carrito-view.js
│   │   ├── negocio/       carrito-service.js            ← STATEFUL
│   │   └── datos/         carrito-repository.js         ← DAO
│   └── usuario/
│       ├── presentacion/  usuario-view.js
│       ├── negocio/       usuario-service.js            ← autorización declarativa
│       └── datos/         usuario-repository.js         ← DAO
│
├── compartido/            ← servicios técnicos transversales, de ningún componente
│   ├── cliente-http.js       transporte HTTP + detección de backend
│   ├── almacenamiento.js     acceso tolerante a localStorage
│   ├── capa-modal.js         velo, foco atrapado, Escape
│   ├── avisos.js             notificaciones efímeras
│   ├── formato.js            precios y números es-AR
│   ├── tema.js               claro / oscuro / normal
│   └── arte.js               ilustraciones SVG de producto
│
└── main.js                ← ARRANQUE. No es una capa: monta los componentes
```

### Por qué "por componente y después por capa", y no al revés

La alternativa era `presentacion/`, `negocio/`, `datos/` arriba y los componentes
adentro. Se descartó porque **las piezas que cambian juntas tienen que estar
juntas**: agregar un filtro al catálogo toca su vista, su service y sus
criterios. Con la carpeta por capa, ese cambio se reparte en tres ramas del
árbol; con la carpeta por componente, cae en una sola.

Y es exactamente lo que ya hacía el backend:

```text
src/main/java/ar/edu/desarrolloappii/ecommerce/catalogo/
├── controller/   ← presentación   (ProductoResource, CategoriaResource)
├── service/      ← negocio        (CatalogoService, CatalogoServiceImpl)
├── repository/   ← datos          (ProductoRepository, CatalogoEnMemoria)
└── domain/                        (Producto, Categoria)
```

**Las dos manos del cable tienen la misma forma.** Eso no es estética: es lo que
permite decir con precisión qué pieza del front se borra cuando su equivalente
del backend entra en producción.

### Reglas de comunicación (Clase 3, slide 20)

Se cumplen las tres, y se pueden verificar leyendo los `import`:

- **Presentación → Negocio → Datos**, en ese orden. Ninguna vista importa un
  repositorio.
- Ninguna capa de datos "sube" a tomar decisiones. `ProductoRepository` devuelve
  **todos** los productos sin filtrar; quién es visible lo decide el negocio.
- No se saltan capas "para ir más rápido".

---

## 3. Stateful vs. stateless

Es la comparación que la consigna pide justificar, y acá está con los dos casos
en el mismo sistema.

### CatálogoComponent — STATELESS

No guarda nada entre llamadas. Dos búsquedas iguales dan el mismo resultado sin
importar el orden ni quién las pidió. Cualquier instancia puede atender cualquier
consulta — que es por lo que del otro lado se declara `@Stateless` y el
contenedor puede tener un **pool** compartido.

Probado en `src/test/js/catalogo-service-local.test.mjs`:

```js
test('es STATELESS: la misma búsqueda repetida da el mismo resultado', async () => {
  const primera = await servicio.buscar(criterios({ q: 'teclado' }));
  await servicio.buscar(criterios({ categoria: 'hogar', soloStock: true }));
  const segunda = await servicio.buscar(criterios({ q: 'teclado' }));
  assert.deepEqual(ids(primera), ids(segunda));
});
```

### CarritoComponent — STATEFUL

Mantiene **estado conversacional**: el resultado de una operación depende de
todo lo que el cliente hizo antes.

Probado en `src/test/js/carrito-service.test.mjs`:

```js
test('la MISMA llamada da distinto resultado según la historia previa', () => {
  const disponible = producto({ stock: 2 });
  assert.equal(carrito.agregar(disponible).ok, true);   // 1.ª vez: entra
  assert.equal(carrito.agregar(disponible).ok, true);   // 2.ª vez: entra
  assert.equal(carrito.agregar(disponible).ok, false);  // 3.ª: rechazada por el estado
});
```

Tres invocaciones idénticas, con el mismo argumento, tres resultados distintos.
**Eso** es ser stateful — no "guardar cosas en algún lado".

Del lado del servidor esto se declara `@Stateful` y el contenedor garantiza que
cada cliente hable siempre con SU instancia. Acá el rol de contenedor lo cumple
el navegador: la instancia vive mientras viva la pestaña y el repositorio la
rehidrata al volver.

Detalle que conviene tener a mano: **el total nunca se persiste**. Se recalcula
desde los ítems en cada `resumen()`. Guardarlo sería guardar una verdad que
puede vencer.

---

## 4. Patrones de diseño aplicados

La consigna pide **3 como mínimo**, justificados, y el Anexo agrega que hay que
poder explicar *por qué se descartaron las alternativas*. Hay seis, y ninguno
está puesto para llenar la lista.

### 4.1 Facade

**Dónde:** las tres fachadas de componente — `catalogo-service.js`,
`carrito-service.js`, `usuario-service.js`.

**Qué problema resuelve:** la presentación necesita el catálogo, no sus tripas.
Sin fachada, `main.js` tendría que instanciar el repositorio, decidir qué
implementación usar y conocer el orden de las llamadas. Con fachada importa un
módulo y llama cinco funciones.

**Alternativa descartada:** exportar los repositorios y armar el service en el
arranque. Se descartó porque volvía a `main.js` responsable de cablear cada
componente: cualquier cambio interno del catálogo obligaba a tocar el arranque.

> Es, además, de dónde sale el nombre del proyecto.

### 4.2 Repository / DAO

**Dónde:** `producto-repository.js`, `categoria-repository.js`,
`carrito-repository.js`, `usuario-repository.js`.

**Qué problema resuelve:** desacoplar el acceso a datos de la lógica. Hoy el
carrito vive en `localStorage`; cuando exista `POST /api/carritos/{id}` se agrega
un `CarritoRepositoryRest` que cumple el mismo contrato y `CarritoService` **no
se toca**.

**Alternativa descartada:** llamar a `localStorage` directo desde el service. Se
descartó porque ataba las reglas del carrito a un medio de persistencia del
navegador — y esas reglas tienen que poder correr también en el servidor.

### 4.3 Adapter

**Dónde:** `catalogo-service-remoto.js`.

**Qué problema resuelve:** cuando hay backend, el componente Catálogo **ya está
implementado en Java**, con su negocio y sus datos. El front no debe duplicarlo:
necesita un enchufe que traduzca `CriteriosDeBusqueda` → query params de JAX-RS y
DTO JSON → objeto de dominio, y que delegue. Es el caso de "capas lógicas vs.
capas físicas" de la Clase 3 (slide 24): las tres capas siguen existiendo, sólo
que dos corren en otra máquina.

**Alternativa descartada:** que la vista hiciera `fetch` directo. Se descartó
porque filtra HTTP hacia la presentación: la vista tendría que saber de códigos
de estado, y la misma UI no podría funcionar en el despliegue estático.

Detalle fino: el adapter traduce el 404 del backend al **mismo** error de dominio
que lanza la implementación local. Si dejara escapar un `ErrorDeRed`, la
presentación se enteraría de que atrás hay HTTP.

### 4.4 Factory

**Dónde:** `implementacion()` en `catalogo-service.js`.

**Qué problema resuelve:** hay dos implementaciones del contrato `CatalogoService`
y alguien tiene que elegir una. La fábrica lo decide **una sola vez**, en el
arranque, consultando `GET api/salud`:

| Destino | `api/salud` | Implementación activa |
|---|---|---|
| `ecommerce.war` en WildFly | `disponible: true` | `CatalogoServiceRemoto` |
| Sitio estático (Vercel) | `disponible: false` | `CatalogoServiceLocal` |

Quien llama nunca instancia una implementación concreta. Es lo que hace `@Inject`
del otro lado: pedir el contrato y dejar que otro resuelva quién lo cumple.

**Alternativa descartada:** ramificar por hostname, o provocar un 404 a propósito
para detectar el entorno. Lo primero rompe en cuanto cambia el dominio; lo
segundo ensucia la consola con errores de red en uno de los dos destinos.

### 4.5 Strategy

**Dónde:** `ESTRATEGIAS_DE_ORDEN` en `catalogo-service-local.js`.

**Qué problema resuelve:** cinco criterios de ordenamiento intercambiables en
tiempo de ejecución, elegidos por clave. Agregar un orden nuevo es agregar una
entrada al mapa: no se toca `buscar()`.

**Alternativa descartada:** un `switch` dentro de `buscar()`. Se descartó porque
crece con cada orden nuevo y mete la decisión adentro del método que debería sólo
aplicarla. Es el mismo mapa de `Comparator` que arma `comparador(Orden)` en Java.

### 4.6 Observer

**Dónde:** `alCambiar()` en `carrito-service.js` y `alCambiarSesion()` en
`usuario-service.js`.

**Qué problema resuelve:** la insignia de la cabecera, el panel lateral, los
botones de la grilla y el menú de cuenta tienen que reflejar el estado. El
negocio emite y sigue; **no conoce la vista**. Por eso `carrito-service.js` no
importa una sola línea de DOM.

**Alternativa descartada:** que el negocio llamara a las funciones de render. Se
descartó porque invierte la dependencia: el negocio pasaría a depender de la
presentación, que es justo lo que las capas prohíben.

---

## 5. Seguridad declarativa

Requisito de la consigna: *"autenticación y autorización por rol en al menos una
operación sensible"*. Hay **dos** operaciones protegidas.

### El mecanismo

En Jakarta EE uno no escribe un `if` adentro del método: anota, y el contenedor
intercepta antes de que el cuerpo corra.

```java
@RolesAllowed({"ADMIN"})
public List<Usuario> listarUsuarios() { ... }
```

Acá se reproduce eso con `autorizar()`, aplicado **en el momento de declarar** la
operación:

```js
export const listarUsuarios = autorizar([ROLES.ADMIN], () =>
  repositorio.buscarTodos().map(({ nombre, email, rol, creadoEn }) => ({ nombre, email, rol, creadoEn }))
);
```

Lo que se gana frente a un `if` desperdigado:

1. la regla se lee **en la firma**, no enterrada en el cuerpo;
2. vale para **todas** las invocaciones, no para la rama que uno se acordó;
3. la operación de negocio queda **limpia** de código de seguridad.

Cuando esto pase al backend, `autorizar([ROLES.ADMIN], …)` se borra y se pone
`@RolesAllowed({"ADMIN"})`. **Cambia el ejecutor, no el diseño.**

### Las dos operaciones sensibles

| Operación | Declarada en | Exige | Error |
|---|---|---|---|
| `confirmarCompra()` | `carrito-service.js` | sesión CLIENTE o ADMIN | `NoAutenticadoError` (401) |
| `listarUsuarios()` / `cambiarRol()` | `usuario-service.js` | rol ADMIN | `AccesoDenegadoError` (403) |

Notá el reparto: la regla la declara **el componente dueño de la operación**,
usando la primitiva que expone el componente Usuario. Carrito no sabe cómo se
verifica una contraseña; Usuario no sabe qué es un ítem. **La seguridad es un
servicio transversal**, igual que en el contenedor.

### Los dos errores son distintos a propósito

- **401 `NoAutenticadoError`** — no sé quién sos.
- **403 `AccesoDenegadoError`** — sé quién sos y no te alcanza.

### Contraseñas

Nunca se persiste el texto en claro. Se guarda `{hash, salt}` con **SHA-256 y
salt por usuario**, así que dos personas con la misma contraseña producen hashes
distintos.

**Lo que hay que decir, y decirlo primero:** SHA-256 no es lo correcto para
contraseñas — lo correcto es bcrypt o Argon2, que son deliberadamente lentos. En
el navegador no hay forma honesta de hacerlo bien, y cuando el registro pase a
`POST /api/usuarios` el hash lo calculará el backend con Argon2. Lo que sí se
sostiene y es el punto de la decisión: **la contraseña no se guarda en claro**.

### Y la limitación, dicha de frente

**Un front nunca es una frontera de seguridad.** Cualquiera puede abrir el
devtools y escribir `rol: "ADMIN"` en `localStorage`. Eso no es un descuido
nuestro: es la naturaleza del cliente. Lo que se demuestra acá es el **diseño**
de la autorización — dónde se declara, cómo se propaga, qué error produce. La
autoridad real vive en el servidor, que revalida todo. Es la misma lógica del
carrito: calcula totales para que el usuario vea algo, pero el total que vale es
el que recalcula el backend.

---

## 6. Verificación

**84 pruebas unitarias**, sin navegador, sin red y sin dependencias externas:

```bash
cd TPO
node --test 'src/test/js/**/*.test.mjs'
```

| Archivo | Pruebas | Qué cubre |
|---|---|---|
| `catalogo-service-local.test.mjs` | 25 | visibilidad de inactivos, búsqueda sin acentos y "todas las palabras", filtros combinados, los 5 ordenamientos, facetas, rango, aislamiento |
| `carrito-service.test.mjs` | 28 | topes de stock, totales recalculados, umbral de envío, **que es stateful**, observer, operación sensible |
| `usuario-service.test.mjs` | 31 | autenticación, hash con salt, registro, **autorización declarativa**, cambio de rol, observer |

`catalogo-service-local.test.mjs` no importa ningún doble del navegador — **no lo
necesita**, porque es stateless y recibe sus repositorios por constructor:

```js
servicio = new CatalogoServiceLocal(
  new ProductoRepositoryFalso(CATALOGO),
  new CategoriaRepositoryFalso(CATEGORIAS)
);
```

Repositorios falsos en tres líneas. Eso es lo que la Clase 3 (slide 21) llama
*"testing aislado"*, y es la prueba concreta de que las capas están de verdad
separadas y no sólo dibujadas.

Los casos son el **espejo** de `CatalogoServiceImplTest.java`: misma
especificación, dos implementaciones. Si una se desvía, un test lo canta.

---

## 7. Guion de la demo

Unos 12 minutos. Cada paso muestra algo que la consigna pide.

### 1 · La estructura (2 min) — *componente ≠ pantalla*

Abrir `assets/js/componentes/` al lado de
`src/main/java/ar/edu/desarrolloappii/ecommerce/catalogo/`.
Misma forma. Decir la frase: **"un componente no es una pantalla"**.

### 2 · CatálogoComponent, stateless (2 min)

Buscar `cafe` → aparece *Cafetera de Filtro Signature* (sin acentos).
Buscar `teclado mecanico` → *Teclado Mecánico Compacto*; buscar `teclado cafetera`
→ **cero resultados**, porque exige TODAS las palabras y no es un `OR`.
Cambiar el orden → **Strategy**. Ver los conteos por categoría → facetas.

### 3 · CarritoComponent, stateful (2 min)

Agregar el *Escritorio Regulable en Altura* (stock 2) hasta el tope → el botón cambia a *"Máximo en
carrito"*. **Misma acción, distinto resultado: eso es stateful.**
Mostrar que el total sigue a las cantidades y que el envío se libera al cruzar
$150.000.

### 4 · UsuarioComponent y seguridad (3 min)

1. Sin sesión, tocar **Confirmar compra** → se abre el ingreso. *(Eso es el 401.)*
2. Entrar como `cliente@fachada.com.ar` / `cliente123` → chip **CLIENTE**. El
   menú **no ofrece** el panel de administración.
3. En la consola: `fachada.usuario.listarUsuarios()` → **`AccesoDenegadoError`**.
   *Ni desde la consola: el control está en la declaración de la operación.*
4. Confirmar la compra → ahora sí, código de pedido.
5. Salir, entrar como `admin@fachada.com.ar` / `admin123` → chip **ADMIN**,
   aparece el panel, se puede cambiar un rol.
6. Intentar degradarse a sí mismo → rechazado.

### 5 · Los tests (2 min)

```bash
node --test 'src/test/js/**/*.test.mjs'
```

84 en verde en menos de un segundo. Abrir el test del carrito y leer en voz alta
el caso *"la MISMA llamada da distinto resultado según la historia previa"*.

### 6 · Extras (1 min)

Los tres temas en vivo (Anexo §7) y el responsive de 320 a 1440 px (Anexo §8).

### Preguntas que van a venir — y la respuesta corta

| Pregunta | Respuesta |
|---|---|
| *¿Esto no es seguridad de verdad?* | No, y es a propósito. Un front no es frontera de seguridad. Demostramos el **diseño**; la autoridad la tiene el servidor, que revalida. |
| *¿Por qué duplicás las reglas del catálogo?* | No están duplicadas por descuido: son dos **implementaciones del mismo contrato**, y la fábrica elige UNA en el arranque. La local existe porque el despliegue estático no tiene backend. |
| *¿Por qué el carrito es stateful y el catálogo no?* | Porque el resultado de `agregar()` depende de la historia previa del cliente; el de `buscar()` no depende de nada. Hay un test de cada cosa. |
| *¿Dónde está la capa de datos del front?* | En `componentes/<x>/datos/`. Son DAOs sobre `localStorage` y sobre el JSON semilla. El día que haya API, se agrega una implementación del mismo contrato. |
| *¿Por qué SHA-256 y no bcrypt?* | Porque bcrypt tiene sentido en el servidor. Acá lo que se sostiene es que la contraseña no se guarda en claro. |

---

## 8. Checklist de la entrega

| Requisito (consigna §5, Entrega Obligatoria N.º 1) | Estado |
|---|---|
| Al menos 3 componentes implementados, cada uno en capas | ✅ Catálogo · Carrito · Usuario |
| Al menos 1 stateful y 1 stateless, justificados | ✅ Carrito / Catálogo, con un test cada uno |
| Al menos 3 patrones distintos, aplicados y justificados | ✅ 6: Facade, DAO, Adapter, Factory, Strategy, Observer |
| Seguridad declarativa por rol en una operación sensible | ✅ 2 operaciones: `confirmarCompra()` y `listarUsuarios()` |
| Demo funcional en vivo | ✅ guion en §7 |
| Evidencia del ciclo de vida gestionado | ⚠️ parcial — en el front lo administra el navegador; los `@PostConstruct` / `@PreDestroy` llegan con el backend |
| Documento técnico de 5 a 8 páginas | ⚠️ este documento es la base; falta maquetarlo a PDF |

### Lo que falta y lo sabemos

- **`PagoComponent`** — el cuarto componente del caso. El checkout hoy simula el
  pago dentro del carrito; separarlo pide un Adapter hacia la pasarela.
- **Persistencia real** — `CatalogoEnMemoria` sigue siendo el repositorio del
  backend; JPA es el próximo paso y los contratos ya están.
- **Componentes 4, 5 y 6** del checklist transversal (mínimo 6 identificados):
  están identificados en el documento del Checkpoint 1, sin implementar.

---

## 9. Bibliografía y referencias de clase

- Clase 1 — POC, componente vs. objeto, caso "tienda online"
- Clase 2 — contenedores, ciclo de vida, `@Stateless` / `@Stateful`, CDI, JTA
- Clase 3 — arquitectura de capas, reglas de comunicación, anti-patrones
  (slides 4, 16-21, 24, 35, 36)
- Bass, Clements & Kazman (2003), *Software Architecture in Practice*
- Fowler (2003), *Patterns of Enterprise Application Architecture*
