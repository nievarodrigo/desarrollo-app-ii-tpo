# Fachada — landing del e-commerce

Tienda online del Trabajo Práctico Integrador de **Desarrollo de Aplicaciones II**.
Es la capa de presentación real del proyecto: catálogo, búsqueda, filtros y carrito
funcionando de punta a punta **contra la API Jakarta EE** cuando corre dentro del WAR,
y contra un catálogo estático cuando se publica sin backend.

> Este sitio **no** documenta el TP: es la tienda. La documentación académica vive en
> `TPO/MOCKUP_TPO.md` y el mockup anterior en `TPO/mockup-web/`.
>
> Los archivos viven en `TPO/src/main/webapp/`, dentro del WAR. La misma carpeta se
> publica como sitio estático en <https://landing-ecommerce-tau.vercel.app>.

---

## Ejecutar en local

La UI vive en `src/main/webapp/` y usa **módulos ES nativos**, así que necesita
servirse por HTTP (abrir `index.html` con `file://` falla por CORS).

```bash
# Sólo el front, en modo mock
cd TPO/src/main/webapp && python3 -m http.server 4321

# Con el backend Jakarta EE
cd TPO && ./mvnw clean package
cp target/ecommerce.war ../wildfly/wildfly-41-ee10/standalone/deployments/
../wildfly/wildfly-41-ee10/bin/standalone.sh          # → localhost:8080/ecommerce/
```

No hay build, ni `npm install`, ni dependencias en tiempo de ejecución.

---

## Estructura

```text
src/main/webapp/
├── index.html                 # una sola página, HTML semántico
├── WEB-INF/beans.xml
└── assets/
    ├── favicon.svg
    ├── data/
    │   ├── catalogo.json      # ← semilla ÚNICA: la lee el front Y el backend
    │   └── estado-backend.json
    ├── css/styles.css         # sistema visual (tokens → componentes → responsive)
    └── js/
        ├── main.js            # orquestación: conecta datos con UI
        ├── data/catalog.js    # carga del catálogo estático (sólo modo mock)
        ├── core/
        │   ├── api.js         # capa de acceso a datos (http | mock, autodetectado)
        │   ├── cart.js        # estado y reglas del carrito
        │   ├── theme.js       # temas normal / claro / oscuro
        │   ├── format.js      # precios en ARS, cuotas, descuentos
        │   └── storage.js     # localStorage tolerante a fallos
        └── ui/
            ├── producto.js    # tarjeta y ficha de producto
            ├── carrito-ui.js  # panel lateral del carrito
            ├── art.js         # ilustraciones SVG de producto
            ├── overlay.js     # velo, foco atrapado, Escape
            └── avisos.js      # notificaciones efímeras
```

---

## Correspondencia con el stack elegido

| Área del stack | Tecnología declarada | Estado |
|---|---|---|
| Presentación | HTML/CSS/JS + JAX-RS | ✅ ambas: la UI consume `ProductoResource` y `CategoriaResource` |
| Entorno | VS Code + JDK 17+ | ✅ compila con `maven.compiler.release=17` |
| Construcción | Apache Maven | ✅ vía Maven Wrapper → `ecommerce.war` |
| Negocio | CDI + EJB + JTA | ✅ `@Stateless` + `@Inject` en el componente Catálogo |
| Datos | JPA / Hibernate + SQL | ⏳ hoy repositorio en memoria detrás de la interfaz |
| Mensajería | JMS + MDB | ⏳ pendiente |
| Servidor | WildFly | ✅ desplegado en `wildfly-41-ee10` |
| Empaquetado | WAR | ✅ `ecommerce.war` |

### Por qué no hay framework ni bundler

No es minimalismo por gusto: es lo que hace que estos archivos entren en el WAR sin
fricción. Con React o Vite, `mvn package` tendría que ejecutar Node y compilar antes
de armar el WAR. Así, los archivos ya están donde tienen que estar.

---

## Decisiones técnicas

### Sin framework ni build

Es un sitio estático de una página. Un bundler agregaría un paso de compilación,
`node_modules` y configuración para resolver un problema que acá no existe. Los
módulos ES nativos ya dan separación real de responsabilidades, y el despliegue es
copiar archivos. **La complejidad se agrega cuando duele, no antes.**

### La UI nunca toca los datos directamente

`assets/js/core/api.js` es la única puerta. La interfaz importa `listarProductos`,
`listarCategorias`, `obtenerProducto` y `contarPorCategoria`, y **no sabe de dónde
salen**: puede ser la API Jakarta EE o el catálogo estático.

El modo se resuelve solo, una vez, al arrancar (`GET api/salud`). Se puede forzar
para probar:

```js
// assets/js/core/api.js
const MODO = 'auto';   // 'auto' | 'http' | 'mock'
```

Los contratos coinciden con los recursos JAX-RS del mockup (§11):

| Función del front | Recurso REST | Componente Jakarta EE |
|---|---|---|
| `listarProductos(filtros)` | `GET /api/productos` | `ProductoResource` → `CatalogoService` |
| `contarPorCategoria(filtros)` | `GET /api/productos/facetas` | `CatalogoService` |
| `obtenerProducto(id)` | `GET /api/productos/{id}` | `ProductoResource` |
| `listarCategorias()` | `GET /api/categorias` | `CategoriaRepository` |
| carrito (`core/cart.js`) | `POST/PATCH/DELETE /api/carritos/{id}/items` | ⏳ `CarritoResource` → `CarritoService` |
| checkout (`ui/carrito-ui.js`) | `POST /api/checkout` | ⏳ `CheckoutFacade` |

Las cuatro primeras ya existen y funcionan. Las dos últimas siguen simuladas en el
front hasta que se implementen los componentes Carrito y Pedido.

El modo mock **simula latencia** (220–480 ms). No es un adorno: obliga a que los
estados de carga existan de verdad y no sean una pantalla que nunca se ve.

### El carrito replica las reglas del backend, no las reemplaza

`core/cart.js` aplica las mismas reglas que después validará `CarritoService`:

- la cantidad nunca baja de 1 ni supera el stock;
- el total **se recalcula siempre** desde los ítems, nunca se persiste;
- el envío es gratis desde $150.000, si no cuesta $8.900.

Es una copia optimista para que la UI responda al instante. **El servidor sigue siendo
la autoridad**: por eso el panel dice explícitamente que el total definitivo se
recalcula al confirmar el pedido.

### Una sola semilla de datos

`assets/data/catalogo.json` es el único archivo con productos del proyecto. El front
lo sirve como archivo estático; el backend lo lee del classpath porque el `pom.xml`
lo agrega desde `src/main/webapp/assets/data`. Si tuviéramos una copia en JS y otra
en Java, se desincronizarían a la primera corrección.

### Ilustraciones propias en SVG

Cero fotos de stock, cero CDN de imágenes, cero licencias dudosas. `ui/art.js` dibuja
17 objetos geométricos que toman su color de las variables CSS del tema, así que la
misma ilustración funciona en los tres temas. El fondo de cada pieza se elige con un
hash del SKU, para que un producto siempre se vea igual.

### Temas: `normal`, `claro`, `oscuro`

`normal` es la identidad de la marca (papel cálido, tinta profunda, bermellón);
`claro` es la versión de máximo brillo; `oscuro` está pensado para la noche. La
preferencia se guarda en `localStorage` y **se aplica en un script bloqueante en el
`<head>`** para que no haya parpadeo de tema al cargar.

Hay dos selectores en el documento (cabecera y menú móvil) y se mantienen
sincronizados: son `role="radiogroup"` con tabindex móvil y navegación por flechas.

### Accesibilidad

- HTML semántico: un solo `<h1>`, secciones con `aria-labelledby`, listas reales.
- Enlace de salto al contenido como primer elemento enfocable.
- Foco visible en todo (`:focus-visible`, 2,5 px, sin `outline: none` en ningún lado).
- Carrito y ficha de producto son diálogos modales con foco atrapado, cierre con
  `Escape` y devolución del foco al botón que los abrió.
- Contraste verificado ≥ 4,5:1 en los tres temas para todo el texto, incluido el
  terciario.
- `prefers-reduced-motion` desactiva la marquesina y todas las animaciones.
- Los SVG informativos llevan `role="img"` y `aria-label`; los decorativos,
  `aria-hidden`.

### Sin secretos

No hay claves, tokens ni credenciales. `BASE_URL` es una ruta relativa.

---

## Estados que se pueden ver funcionando

| Estado | Cómo llegar |
|---|---|
| Carga | Se ve en cada búsqueda o cambio de filtro (esqueletos) |
| Sin resultados | Buscar cualquier cosa que no exista |
| Error de red | Botón **“Simular error de red”** al pie del panel de filtros |
| Carrito vacío | Abrir el carrito sin productos |
| Sin stock | Parlante Portátil Cobre y Smartwatch Deportivo GPS |
| Tope de stock | Agregar 3 veces la Notebook Pro 14" (stock 3) |
| Compra confirmada | Confirmar compra con el carrito cargado |

---

## Verificación realizada

Suite de 46 pruebas automatizadas con Playwright sobre Chromium, corrida contra los
**tres** escenarios: WildFly (`/ecommerce/`), estático local y Vercel.

- **Catálogo**: render, precios en ARS, ilustraciones, categorías, conteos.
- **Búsqueda**: filtrado, estado vacío, limpieza de filtros, atajo `/`.
- **Filtros**: categoría, precio máximo, sólo con stock, orden, fichas activas, error
  y reintento.
- **Carrito**: agregar, sumar, restar, quitar, vaciar, tope de stock, envío gratis,
  total recalculado, checkout.
- **Temas**: los tres se aplican, persisten y sobreviven a la recarga; los dos
  selectores quedan sincronizados.
- **Accesibilidad**: enlace de salto, navegación por flechas, foco atrapado y
  devuelto, `aria-label` en SVG, un solo `h1`, contraste AA en los tres temas.
- **Responsive**: sin overflow horizontal a 1440, 1180, 834, 390 y 320 px; menú móvil
  y panel de filtros plegable.
- **Portabilidad**: la misma UI servida desde la raíz y desde `/ecommerce/` resuelve
  la API contra su propio context root, sin errores de consola.
- **Origen de los datos**: se verifica que en WildFly el catálogo venga de la API y
  **no** caiga al mock por accidente, y que en el sitio estático no se le pegue a la
  API. Sin esto, una suite verde no probaría nada.
- **Consola**: cero errores de JavaScript y cero peticiones fallidas.

---

## Qué falta

1. Componentes **Carrito** y **Pedido** con sus recursos REST.
2. Reemplazar el checkout simulado de `ui/carrito-ui.js` por `POST /api/checkout` y
   mostrar el `PedidoResponse` real.
3. Sumar autenticación: hoy el menú de cuenta es demostrativo.
5. Cuando el backend cubra todo, `MODO = 'http'` fijo y el catálogo estático queda
   sólo como respaldo del sitio público.
