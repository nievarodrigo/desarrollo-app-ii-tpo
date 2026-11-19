# GOMARKET

**E-commerce en capas sobre Jakarta EE 10.** Catálogo con búsqueda, filtros y facetas;
carrito y checkout; API REST propia. Se empaqueta como un único WAR desplegable en
WildFly y la misma UI se publica como sitio estático sin tocar una línea de código.

> **Una entrada simple a un sistema complejo.** El cliente ve un botón "Comprar";
> atrás hay capas, servicios, repositorios y transacciones. Cada componente expone
> una sola puerta de entrada —el patrón *Facade*— y esconde todo lo demás. Esa es
> la promesa de este proyecto.

🔗 **[Ver la tienda en vivo](https://landing-ecommerce-tau.vercel.app)**

```
Java 17  ·  Jakarta EE 10  ·  JAX-RS  ·  CDI + EJB  ·  JUnit 5  ·  WAR único
```

---

## Qué hace

| | |
|---|---|
| 🔎 **Búsqueda** | Sin acentos y por "todas las palabras": `cafe` encuentra *Cafetera*, y `teclado cafetera` no devuelve nada porque exige ambos términos |
| 🎚️ **Filtros** | Categoría, sólo con stock, precio máximo, y 4 ordenamientos |
| 📊 **Facetas** | Conteo por categoría y rango real de precios, calculados sobre el catálogo vivo |
| 🛒 **Carrito** | Alta, baja, cantidades y total, persistido en el navegador |
| 👤 **Cuentas** | Registro, ingreso y sesión con roles `CLIENTE` / `ADMIN`; contraseñas con hash y salt |
| 🔒 **Autorización** | Confirmar una compra exige sesión; el panel de administración exige `ADMIN` |
| 💳 **Pedido de demostración** | Con WildFly: REST → JMS → MDB y consulta de estado. Sin backend: simulación local claramente rotulada. No hay cobro real. |
| 🎨 **3 temas** | Con persistencia por visitante |
| ♿ **Accesible** | Contraste AA, navegación por teclado, foco atrapado en modales |
| 📱 **Responsive** | De 320 a 1440 px |

**Catálogo actual:** 16 productos en 5 categorías — Tecnología, Audio, Hogar, Oficina y Deportes.

---

## Arranque rápido

No hace falta instalar Maven: el proyecto trae el **Maven Wrapper**.

### Compilar

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # o cualquier JDK 17+
./mvnw clean package
# → target/ecommerce.war
```

### Desplegar en WildFly

```bash
WF=../wildfly/wildfly-41-ee10
cp target/ecommerce.war "$WF/standalone/deployments/"
"$WF/bin/standalone.sh" -c standalone-full.xml  # habilita Artemis/JMS
```

Queda en **<http://localhost:8080/ecommerce/>**

### Sólo el front, sin backend

```bash
cd src/main/webapp && python3 -m http.server 4321
```

Levanta con `CatalogoServiceLocal` — ver [*Un mismo front, dos destinos*](#un-mismo-front-dos-destinos).

### Tests

```bash
./mvnw test          # catálogo y reglas del pedido
node --test src/test/js/*.test.mjs
```

---


### Seguridad y pedidos (2.ª entrega)

La API de pedidos exige un **JWT**. Cuentas de demostración: `cliente@gomarket.com.ar` / `cliente123` (rol CLIENTE) y `admin@gomarket.com.ar` / `admin123` (rol ADMIN). La clave de firma sale de `GOMARKET_JWT_SECRET`.

```bash
./scripts/levantar-wildfly.sh        # compila, despliega y levanta (JDK 17+, sin sudo)
./scripts/probar-entrega2.sh         # recorre login, roles, pedido, mensajería y el 409
```

| Método y ruta | Quién | Respuestas |
|---|---|---|
| `POST /api/v1/auth/login` · `POST /api/v1/auth/registro` | público | 200/201 · 401 · 429 (5 fallos) |
| `GET /api/v1/auth/yo` | CLIENTE, ADMIN | 200 · 401 |
| `GET /api/v1/auth/usuarios` | ADMIN | 200 · 401 · 403 |
| `POST /api/v1/pedidos` | CLIENTE | 202 · 400 · 401 · 403 |
| `GET /api/v1/pedidos/{id}` | dueño o ADMIN | 200 · 403 · 404 |
| `POST /api/v1/pedidos/{id}/cancelar` | dueño o ADMIN | 200 · **409** si ya está CONFIRMADO |
| `POST /api/v1/pedidos/{id}/confirmar` | ADMIN | 200 · **409** si no está PROCESADO |

Decisiones en `docs/adr/` (ADR-008 JWT y roles, ADR-009 REST y no SOAP, ADR-010 puertos).

## Arquitectura

Tres capas, con una regla que no se negocia: **las dependencias apuntan hacia adentro.**

```
┌─────────────────────────────────────────────────────────┐
│  PRESENTACIÓN                                           │
│  HTML/CSS/JS sin framework  ·  JAX-RS (@Path)           │
│  ProductoResource · CategoriaResource · SaludResource   │
└───────────────────────────┬─────────────────────────────┘
                            │ CriteriosDeBusqueda ↓   DTOs ↑
┌───────────────────────────▼─────────────────────────────┐
│  NEGOCIO                                                │
│  CDI + EJB @Stateless                                   │
│  CatalogoService ← CatalogoServiceImpl                  │
│  buscar() · obtener() · listarCategorias() · rango()    │
└───────────────────────────┬─────────────────────────────┘
                            │ interfaces ↓      entidades ↑
┌───────────────────────────▼─────────────────────────────┐
│  DATOS                                                  │
│  ProductoRepository · CategoriaRepository  (interfaces) │
│  └── CatalogoEnMemoria            (impl. actual)        │
│  └── CatalogoJpa                  (siguiente paso)      │
└─────────────────────────────────────────────────────────┘
```

El negocio **no importa nada de `jakarta.ws.rs`** y **no conoce la implementación**
del repositorio. Por eso los tests lo instancian a mano, sin levantar el contenedor.

### Estado por componente

| Componente | Presentación | Negocio | Datos | Estado |
|---|---|---|---|---|
| **Catálogo** | ✅ front + JAX-RS | ✅ front + EJB | ✅ en memoria · JPA pendiente | **stateless** |
| **Carrito** | ✅ front | ✅ front | ✅ front (localStorage) | **stateful** |
| **Usuario** | ✅ front | ✅ front | ✅ front (localStorage) | sesión + roles |
| Pago | ⏳ | ⏳ | ⏳ | — |
| Pedido | ⏳ | ⏳ | ⏳ | — |
| Notificación | — | ⏳ | — | — |

Los tres primeros están completos **del lado del cliente**, cada uno con sus tres
capas y su interfaz explícita — ver
[Arquitectura del front](docs/ARQUITECTURA_FRONT.md). El Catálogo además está
implementado en el backend; Carrito y Usuario todavía no, y por eso sus reglas
corren en el front y el servidor tendrá que revalidarlas.

### Estructura del código

Organizada **por componente** y, dentro de cada uno, **por capa**. Las piezas que
cambian juntas quedan juntas, sin perder el recorrido Controller → Service → Repository.

```text
TPO/
├── pom.xml                      # empaqueta ecommerce.war
├── mvnw, mvnw.cmd, .mvn/        # Maven Wrapper: no hay que instalar Maven
├── vercel.json                  # publicación estática de src/main/webapp
├── docs/LANDING.md              # documentación de la interfaz
└── src/
    ├── main/
    │   ├── java/ar/edu/desarrolloappii/ecommerce/
    │   │   ├── config/
    │   │   │   ├── JaxRsActivator.java       # @ApplicationPath("/api/v1")
    │   │   │   └── SaludResource.java        # GET /api/v1/salud
    │   │   ├── shared/
    │   │   │   ├── dto/                      # ProductoResponse, FacetasResponse, …
    │   │   │   └── exception/                # excepción de dominio + ExceptionMapper
    │   │   └── catalogo/                     # ← COMPONENTE
    │   │       ├── controller/               #   presentación
    │   │       ├── service/                  #   negocio
    │   │       ├── repository/               #   datos
    │   │       └── domain/
    │   └── webapp/
    │       ├── index.html                    # la tienda
    │       ├── assets/
    │       │   ├── css/styles.css
    │       │   ├── data/catalogo.json        # ← semilla única (front y backend)
    │       │   └── js/
    │       │       ├── componentes/          # ← MISMO criterio que el backend
    │       │       │   ├── catalogo/{presentacion,negocio,datos}/
    │       │       │   ├── carrito/{presentacion,negocio,datos}/
    │       │       │   └── usuario/{presentacion,negocio,datos}/
    │       │       ├── compartido/           # servicios técnicos transversales
    │       │       └── main.js               # arranque: monta los componentes
    │       └── WEB-INF/beans.xml
    └── test/
        ├── java/…/CatalogoServiceImplTest.java
        └── js/                               # 84 pruebas del front, sin navegador
```

El front está organizado **por componente y, dentro de cada uno, por capa** —
igual que el backend. No es simetría decorativa: es lo que permite decir con
precisión qué pieza del cliente se borra cuando su equivalente del servidor
entra en producción.

---

## API

Base: `/ecommerce/api/v1`

| Método | Ruta | Devuelve |
|---|---|---|
| `GET` | `/salud` | `{disponible, origen, version}` |
| `GET` | `/categorias` | `CategoriaResponse[]` |
| `GET` | `/productos` | `{items, total}` — acepta `q`, `categoria`, `orden`, `soloStock`, `precioMax` |
| `GET` | `/productos/facetas` | `{total, porCategoria, precioMin, precioMax}` |
| `GET` | `/productos/{id}` | `ProductoResponse`, o `404` con cuerpo JSON |
| `POST` | `/pedidos` | `202 Accepted` con `{id, estado, items, total}`; `400` si la solicitud es inválida |
| `GET` | `/pedidos/{id}` | Estado `PENDIENTE` o `PROCESADO`; `404` si no existe |

Los errores de negocio `400`/`404` de Pedido y el `404` de Catálogo se
devuelven como `application/problem+json` (`type`, `title`, `status`, `detail`,
`instance`). La [entrega de Clase 10](entrega-clase10/LEEME.md) incluye
catálogo de endpoints, OpenAPI, ejemplos y diagrama.

```bash
curl http://localhost:8080/ecommerce/api/v1/productos?q=cafe
curl http://localhost:8080/ecommerce/api/v1/productos/999   # → 404
curl -X POST http://localhost:8080/ecommerce/api/v1/pedidos \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"idProducto":1,"cantidad":1}]}'
# Luego GET /ecommerce/api/v1/pedidos/<id devuelto>
```

### Avance del 09/10: alcance real

`PedidoService` valida cantidades y stock contra el catálogo, recalcula el total
en el servidor y publica un mensaje persistente en `ProcesarPedidoQueue`.
`ProcesarPedidoMDB` consume ese mensaje y marca el pedido como `PROCESADO` en
`PedidoEnMemoria`. Esto demuestra mensajería real y consulta REST; **no** cobra,
reserva stock, persiste pedidos entre reinicios ni autentica el endpoint del
servidor. El `202` sólo significa aceptación para procesamiento. Se requiere
WildFly con `standalone-full.xml`; el despliegue estático mantiene una
simulación local diferente. Ver el
[guion de exposición](docs/ENTREGA_AVANCE_2026-10-09.md).

---

## Decisiones de arquitectura

### El controller no decide nada

`ProductoResource` traduce query params a un `CriteriosDeBusqueda` y arma el DTO de
salida. Qué producto es visible, cómo se busca y cómo se ordena lo decide
`CatalogoService`. El repositorio sólo entrega filas.

Que el service **no importe nada de JAX-RS** no es un detalle estético: por eso las
pruebas unitarias lo instancian a mano, sin levantar el contenedor.

### Se programa contra interfaces

`CatalogoServiceImpl` recibe `ProductoRepository` y `CategoriaRepository` —
interfaces, no clases. Hoy las cumple `CatalogoEnMemoria`; cuando entre JPA se
reemplaza esa implementación y **el negocio no se toca**. Eso es lo que hace que
`CatalogoServiceImplTest` pueda usar repositorios falsos en tres líneas.

### Las entidades no se exponen

`Producto` nunca sale por la API: sale `ProductoResponse`. Cuando `Producto` sea una
`@Entity` con relaciones y campos internos, el contrato de la API no cambia.

### El error de dominio no sabe de HTTP

`CatalogoService` lanza `RecursoNoEncontradoException`. Recién en
`RecursoNoEncontradoMapper` eso se convierte en un 404. Sin `try/catch` en los
controllers y sin `jakarta.ws.rs` en el negocio.

### Una sola semilla de datos

`assets/data/catalogo.json` es el único archivo con productos. El front lo sirve como
archivo estático y el backend lo lee del classpath — el `pom.xml` lo agrega desde
`src/main/webapp/assets/data` con un `<resource>`. Duplicarlo garantizaría que las
dos copias se desincronicen a la primera corrección.

### `@Stateless` aunque todavía no escriba nada

El catálogo hoy sólo lee. Se declara `@Stateless` igual para tener el pool de
instancias y las **transacciones declarativas** del contenedor ya resueltas cuando
entre JPA, sin tocar el código.

---

## Un mismo front, dos destinos

La UI se publica de dos formas y **el código fuente es exactamente el mismo**:

| Destino | Implementación activa de `CatalogoService` | Origen de los datos |
|---|---|---|
| `ecommerce.war` en WildFly | `CatalogoServiceRemoto` (Adapter) | API JAX-RS (`GET /ecommerce/api/v1/...`) |
| Sitio estático en Vercel | `CatalogoServiceLocal` | `assets/data/catalogo.json` |

Son **dos implementaciones del mismo contrato**, y una *factory*
(`implementacion()` en `catalogo-service.js`) elige una sola. La presentación no
se entera: habla con la fachada del componente.

Al arrancar, esa fábrica consulta **una sola vez** `GET api/salud`:

- en WildFly responde `SaludResource` con `disponible: true` → usa HTTP;
- en Vercel, un *rewrite* de `vercel.json` devuelve un JSON fijo con
  `disponible: false` → usa el catálogo estático.

Se hace así, y no provocando un 404 a propósito ni mirando el hostname, para que
**ninguno de los dos destinos ensucie la consola con errores de red**.

Dos detalles que lo hacen posible:

- todas las rutas del HTML son **relativas** (`assets/css/styles.css`, sin barra
  inicial), así que la página funciona bajo cualquier context root;
- la base de la API se deriva de `document.baseURI`. Fijarla en `/api/v1` funcionaba en
  la raíz y daba **404 en WildFly bajo `/ecommerce`** — el clásico bug que aparece
  recién el día del despliegue.

---

## Verificación

**Backend — 12 pruebas unitarias** (`./mvnw test`): visibilidad de inactivos, búsqueda
sin acentos, "todas las palabras", filtros de categoría/stock/precio, ordenamientos y
facetas.

**API en WildFly**: los 5 endpoints responden; `/productos/999` devuelve 404 con
cuerpo JSON.

**Front — 84 pruebas unitarias** (`node --test 'src/test/js/**/*.test.mjs'`), sin
navegador, sin red y sin dependencias externas:

| Archivo | Pruebas | Qué cubre |
|---|---|---|
| `catalogo-service-local.test.mjs` | 25 | espejo de `CatalogoServiceImplTest.java`: inactivos, búsqueda sin acentos, "todas las palabras", filtros, los 5 ordenamientos, facetas |
| `carrito-service.test.mjs` | 28 | topes de stock, totales recalculados, umbral de envío, **que es stateful**, observer, operación sensible |
| `usuario-service.test.mjs` | 31 | autenticación, hash con salt, registro, **autorización por rol**, cambio de rol |

Que el test del catálogo no necesite ningún doble del navegador no es
casualidad: es stateless y recibe sus repositorios por constructor. Si las capas
no estuvieran separadas de verdad, ese archivo no podría existir.

**Portabilidad**: se verificó que la misma UI resuelva la API contra el context
root correcto en cada destino, y que en WildFly **no** caiga al modo local por
accidente.

> **Pendiente:** las pruebas de interfaz de punta a punta (navegación real,
> accesibilidad, responsive) todavía no están automatizadas en este repo. Se
> verificaron a mano en los tres escenarios; automatizarlas con Playwright está
> en el roadmap.

---

## Roadmap

1. Reemplazar `CatalogoEnMemoria` por una implementación con **JPA/Hibernate**.
   Los contratos ya están: sólo cambia esa clase.
2. Componentes **Carrito** y **Pedido** con sus recursos REST.
3. **Checkout** como Facade, y **Pago** detrás de un Adapter.
4. Evento `PedidoConfirmado` por **JMS** con su MDB.
5. Llevar **Usuario** al backend: `POST /api/v1/usuarios`, hash con Argon2 y
   `@RolesAllowed` real. El diseño de la autorización ya está hecho en el front
   (`autorizar([...])`); lo que cambia es quién lo hace cumplir.
6. Automatizar las pruebas de interfaz de punta a punta con **Playwright**.

---

## Cómo contribuir

El repo es privado y se trabaja con colaboradores. Flujo:

```bash
git clone https://github.com/nievarodrigo/desarrollo-app-ii-tpo.git
cd desarrollo-app-ii-tpo
git switch -c feat/mi-cambio
./mvnw test                      # que pase antes de pushear
git push -u origin feat/mi-cambio
```

Después se abre un Pull Request. **Nadie commitea directo a `main`.**

Convenciones: [Conventional Commits](https://www.conventionalcommits.org/)
(`feat:`, `fix:`, `docs:`, `refactor:`, `test:`).

> Si el clone te devuelve **404**, no es que el link esté mal: es que todavía no
> aceptaste la invitación de colaborador. Revisá tu mail y corré `gh auth login`.

---

## Documentación técnica

- [**Arquitectura del front — 3 componentes en capas**](docs/ARQUITECTURA_FRONT.md) — componentes, capas, patrones, stateful/stateless y seguridad declarativa
- **Guion de la defensa** — [fuente en Markdown](docs/GUION_DEFENSA.md) · [capturas](docs/capturas/). Dos PDF desde el mismo fuente:
  - [**GUION_EQUIPO.pdf**](docs/GUION_EQUIPO.pdf) (25 pág) — sólo el guion. Un bloque por integrante, en orden de presentación, con lo que dice, sus capturas y sus preguntas. **Este es el que se reparte.**
  - [**GUION_DEFENSA.pdf**](docs/GUION_DEFENSA.pdf) (26 pág) — el anterior + la preparación previa y el montaje de WildFly, para quien arma la demo.

Para levantar todo el día de la presentación: `./scripts/demo.sh --todo`
- [Documentación de la interfaz](docs/LANDING.md)
- [Mockup integral del proyecto](MOCKUP_TPO.md)
- [Arquitectura del e-commerce (PDF)](TP_Primera_Parte_Ecommerce.pdf) · [fuente `.tex`](TP_Primera_Parte_Ecommerce.tex)

Para recompilar los PDF hace falta **XeLaTeX** y las tipografías Inter, Inter Display
y JetBrains Mono:

```bash
xelatex TP_Primera_Parte_Ecommerce.tex   # dos pasadas
```

---

## Contexto académico

Este proyecto es el **Trabajo Práctico Integrador de Desarrollo de Aplicaciones II**.
Todo lo de arriba es el producto; acá quedan los artefactos de la cursada.

### Entrega — Checkpoint 1

- [Documento de 4 páginas](Checkpoint_1_Ecommerce.pdf)
- [Versión técnica de respaldo](Checkpoint_1_Ecommerce_Tecnico.pdf)
- [Enlaces, evidencia y checklist reproducible](ENTREGA_CHECKPOINT_1.md)
- [Prototipo editable del Catálogo](figma-checkpoint-1/README.md)

### Seguimiento

- [Tablero Trello](https://trello.com/b/gbrISl9W/desarrollo-de-aplicaciones-2-tpo)
- [Plan de tareas](PLAN_TRELLO.md)
- [Mockup web anterior](https://mockup-web-cyan.vercel.app)
