# TPO — Desarrollo de Aplicaciones II

Trabajo Práctico Integrador: **e-commerce orientado a componentes y organizado en capas**,
empaquetado como un único WAR desplegable en WildFly.

## Entrega del Checkpoint 1

- [Documento técnico de 4 páginas](Checkpoint_1_Ecommerce.pdf)
- [Enlaces, evidencia y checklist reproducible](ENTREGA_CHECKPOINT_1.md)
- [Prototipo editable del Catálogo](figma-checkpoint-1/README.md)
- [Tablero Trello](https://trello.com/b/gbrISl9W/desarrollo-de-aplicaciones-2-tpo)
- [Landing pública](https://landing-ecommerce-tau.vercel.app)

## Estado actual

| Capa | Estado |
|---|---|
| Presentación (HTML/CSS/JS + JAX-RS) | ✅ implementada |
| Negocio (CDI + EJB) — componente **Catálogo** | ✅ implementado |
| Datos (Repository) — **en memoria** | ✅ implementado, JPA pendiente |
| Componentes Carrito, Pedido, Pago, Checkout, Notificación | ⏳ pendientes |

- **Landing e-commerce publicada:** <https://landing-ecommerce-tau.vercel.app>
- **Documentación de la UI:** [`docs/LANDING.md`](docs/LANDING.md)

---

## Ejecutar el proyecto

### Compilar y empaquetar

No hace falta instalar Maven: el proyecto trae el **Maven Wrapper**.

```bash
cd TPO
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # o cualquier JDK 17+
./mvnw clean package
# → target/ecommerce.war
```

### Desplegar en WildFly

```bash
WF=../wildfly/wildfly-41-ee10
cp target/ecommerce.war "$WF/standalone/deployments/"
"$WF/bin/standalone.sh"
```

Y queda en **<http://localhost:8080/ecommerce/>**.

### Sólo el front, sin backend

```bash
cd src/main/webapp && python3 -m http.server 4321
```

Levanta en modo mock (ver *Un mismo front, dos destinos* más abajo).

### Pruebas

```bash
./mvnw test          # 12 pruebas unitarias del CatalogoService
```

---

## Estructura

Organizada **por componente** y, dentro de cada uno, **por capa** — como recomienda
`MOCKUP_TPO.md` §7. Las piezas que cambian juntas quedan juntas, sin perder el
recorrido Controller → Service → Repository.

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
    │   │   │   ├── JaxRsActivator.java       # @ApplicationPath("/api")
    │   │   │   └── SaludResource.java        # GET /api/salud
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
    │       │   ├── css/ js/                  # UI sin framework ni build
    │       │   └── data/catalogo.json        # ← semilla única (front y backend)
    │       └── WEB-INF/beans.xml
    └── test/java/…/CatalogoServiceImplTest.java
```

---

## API

Base: `/ecommerce/api`

| Método | Ruta | Devuelve |
|---|---|---|
| `GET` | `/salud` | `{disponible, origen, version}` |
| `GET` | `/categorias` | `CategoriaResponse[]` |
| `GET` | `/productos` | `{items, total}` — acepta `q`, `categoria`, `orden`, `soloStock`, `precioMax` |
| `GET` | `/productos/facetas` | `{total, porCategoria, precioMin, precioMax}` |
| `GET` | `/productos/{id}` | `ProductoResponse`, o `404` con cuerpo JSON |

```bash
curl http://localhost:8080/ecommerce/api/productos?q=cafe
curl http://localhost:8080/ecommerce/api/productos/999   # → 404
```

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

| Destino | Origen de los datos |
|---|---|
| `ecommerce.war` en WildFly | API JAX-RS (`GET /ecommerce/api/...`) |
| Sitio estático en Vercel | `assets/data/catalogo.json` |

Al arrancar, el front consulta **una sola vez** `GET api/salud`:

- en WildFly responde `SaludResource` con `disponible: true` → usa HTTP;
- en Vercel, un *rewrite* de `vercel.json` devuelve un JSON fijo con
  `disponible: false` → usa el catálogo estático.

Se hace así, y no provocando un 404 a propósito ni mirando el hostname, para que
**ninguno de los dos destinos ensucie la consola con errores de red**.

Dos detalles que lo hacen posible:

- todas las rutas del HTML son **relativas** (`assets/css/styles.css`, sin barra
  inicial), así que la página funciona bajo cualquier context root;
- la base de la API se deriva de `document.baseURI`. Fijarla en `/api` funcionaba en
  la raíz y daba **404 en WildFly bajo `/ecommerce`** — el clásico bug que aparece
  recién el día del despliegue.

---

## Verificación realizada

**Backend — 12 pruebas unitarias** (`./mvnw test`): visibilidad de inactivos, búsqueda
sin acentos, "todas las palabras", filtros de categoría/stock/precio, ordenamientos y
facetas.

**API en WildFly**: los 5 endpoints responden; `/productos/999` devuelve 404 con
cuerpo JSON.

**Front — 46 pruebas automatizadas con Playwright**, corridas contra los **tres**
escenarios (WildFly, estático local y Vercel): catálogo, búsqueda, filtros, carrito
completo, checkout, detalle, los tres temas con persistencia, accesibilidad
(foco atrapado, contraste AA, navegación por teclado) y responsive de 320 a 1440 px.
Cero errores de consola en los tres.

**Portabilidad**: se verificó que la misma UI resuelva la API contra el context root
correcto en cada destino, y que en WildFly **no** caiga al mock por accidente.

---

## Qué sigue

1. Reemplazar `CatalogoEnMemoria` por una implementación con **JPA/Hibernate**.
   Los contratos ya están: sólo cambia esa clase.
2. Componentes **Carrito** y **Pedido** con sus recursos REST.
3. **Checkout** como Facade, y **Pago** detrás de un Adapter.
4. Evento `PedidoConfirmado` por **JMS** con su MDB.
5. Autenticación de usuarios: hoy el menú de cuenta es demostrativo.

---

## Documentación

- [Mockup integral del proyecto](MOCKUP_TPO.md)
- [Documentación de la landing](docs/LANDING.md)
- [PDF — Arquitectura del e-commerce (TP Primera Parte)](TP_Primera_Parte_Ecommerce.pdf)
- [Fuente editable del PDF](TP_Primera_Parte_Ecommerce.tex)
- [Plan de tareas para Trello](PLAN_TRELLO.md)
- [Mockup web anterior](https://mockup-web-cyan.vercel.app)

### Cómo recompilar el PDF

```bash
xelatex TP_Primera_Parte_Ecommerce.tex   # dos pasadas
```

Requiere **XeLaTeX** y las tipografías Inter, Inter Display y JetBrains Mono.
