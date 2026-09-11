# Fachada

**E-commerce en capas sobre Jakarta EE 10.** Catálogo con búsqueda, filtros y facetas;
carrito y checkout; API REST propia. Se empaqueta como un único WAR desplegable en
WildFly y la misma UI se publica como sitio estático sin tocar una línea de código.

> **¿Por qué "Fachada"?** Porque es lo que el patrón *Facade* y una tienda tienen en
> común: **una entrada simple a un sistema complejo.** El cliente ve un botón "Comprar";
> atrás hay capas, servicios, repositorios y transacciones. Esa es exactamente la
> promesa de este proyecto — y el nombre nos obliga a cumplirla.

🔗 **[Ver la tienda en vivo](https://landing-ecommerce-tau.vercel.app)**

```
Java 17  ·  Jakarta EE 10  ·  JAX-RS  ·  CDI + EJB  ·  JUnit 5  ·  WAR único
```

---

## Qué hace

| | |
|---|---|
| 🔎 **Búsqueda** | Sin acentos y por "todas las palabras": `cafe` encuentra *Cafetera*, `mouse inalambrico` exige ambos términos |
| 🎚️ **Filtros** | Categoría, sólo con stock, precio máximo, y 4 ordenamientos |
| 📊 **Facetas** | Conteo por categoría y rango real de precios, calculados sobre el catálogo vivo |
| 🛒 **Carrito** | Alta, baja, cantidades y total, persistido en el navegador |
| 💳 **Checkout** | Flujo completo de confirmación en el front |
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
"$WF/bin/standalone.sh"
```

Queda en **<http://localhost:8080/ecommerce/>**

### Sólo el front, sin backend

```bash
cd src/main/webapp && python3 -m http.server 4321
```

Levanta en modo mock — ver [*Un mismo front, dos destinos*](#un-mismo-front-dos-destinos).

### Tests

```bash
./mvnw test          # 12 pruebas unitarias del CatalogoService
```

---

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

| Componente | Presentación | Negocio | Datos |
|---|---|---|---|
| **Catálogo** | ✅ | ✅ | ✅ en memoria · JPA pendiente |
| Carrito | ✅ front | ⏳ | ⏳ |
| Pedido | ⏳ | ⏳ | ⏳ |
| Pago | ⏳ | ⏳ | ⏳ |
| Checkout | ✅ front | ⏳ | — |
| Notificación | — | ⏳ | — |

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

## Verificación

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

## Roadmap

1. Reemplazar `CatalogoEnMemoria` por una implementación con **JPA/Hibernate**.
   Los contratos ya están: sólo cambia esa clase.
2. Componentes **Carrito** y **Pedido** con sus recursos REST.
3. **Checkout** como Facade, y **Pago** detrás de un Adapter.
4. Evento `PedidoConfirmado` por **JMS** con su MDB.
5. Autenticación de usuarios: hoy el menú de cuenta es demostrativo.

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
