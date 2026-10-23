# Figma — Checkpoint 1 · Catálogo

Prototipo visual del **primer checkpoint** del Trabajo Práctico Integrador de
Desarrollo de Aplicaciones II. Documenta, en formato de diseño, lo único que el
checkpoint 1 entrega funcionando: el **componente Catálogo** de TPO Store.

- **Fecha de creación:** 4 de septiembre de 2026
- **Página de Figma:** `Checkpoint 1 — Catálogo`
- **Enlace de lectura pública:** _(pendiente — se completa al publicar el archivo; ver
  [Cómo armar el archivo de Figma](#cómo-armar-el-archivo-de-figma))_
- **Implementación de referencia:** <https://landing-ecommerce-tau.vercel.app>

> El prototipo no se generó dentro de Figma: este entorno no tiene acceso a la API
> ni a una sesión de Figma. Se entregan en su lugar **SVG editables**, uno por frame,
> que se importan a Figma como capas vectoriales y de texto reales — no como imagen —
> más la receta exacta para reconstruir la página y cablear las interacciones.

---

## Alcance del prototipo

Se prototipa **sólo el componente Catálogo**, que es lo implementado en el checkpoint 1:

| Capa | Qué representa el prototipo |
|---|---|
| Presentación | `index.html` + `assets/js/ui/*` consumiendo la API JAX-RS |
| Controller | `CatalogoResource` / `ProductoResource` — `GET /api/productos`, `/categorias`, `/productos/{id}` |
| Negocio | `CatalogoService` stateless: qué se ve, cómo se busca, cómo se ordena |
| Datos | `ProductoRepository` y `CategoriaRepository`, hoy resueltos por `CatalogoEnMemoria` |

**Fuera de alcance en esta entrega** — y por eso no hay ni un frame de esto:
login y registro, checkout completo, administración, pagos, pedidos, mensajería e
integraciones SOAP/REST externas. Quedan para las entregas siguientes. El carrito
aparece **únicamente como elemento de navegación** en la cabecera: no hay drawer,
no hay línea de pedido, no hay total.

---

## Frames creados

Cuatro frames, sin relleno y sin pantallas de más.

| # | Frame | Medidas | Contenido |
|---|---|---|---|
| 1 | **Catálogo Desktop** | 1440 × 1024 | Cabecera TPO Store, buscador, título del catálogo, panel lateral de filtros (búsqueda, categorías con conteo, precio máximo, sólo con stock, ordenamiento, limpiar), fichas de filtro activas y grilla de productos con precios en pesos argentinos, estado de stock y acciones **Agregar al carrito** / **Ver detalle** |
| 2 | **Detalle de producto Desktop** | 1440 × 1024 | Modal sobre el catálogo atenuado: ilustración, marca, categoría, SKU, precio actual y anterior con porcentaje de descuento, cuotas, stock, descripción, características destacadas, **Agregar al carrito**, **Seguir viendo** y cierre (×) |
| 3 | **Catálogo Mobile** | 390 × 844 | Cabecera móvil con carrito como navegación, buscador, botón **Filtros**, grilla de dos columnas adaptada, sin overflow horizontal |
| 4 | **Filtros Mobile** | 390 × 844 | Bottom sheet abierto sobre el catálogo: categoría en chips, precio máximo, sólo con stock, ordenamiento, **Limpiar filtros** y **Aplicar filtros** |

Un quinto archivo, `05-flujo.svg`, **no es un frame del prototipo**: es la captura
general del flujo, para pegar en el informe del checkpoint.

### Archivos

```text
figma-checkpoint-1/
├── README.md
├── generar_frames.py          # genera los SVG desde catalogo.json y los tokens del CSS
├── exportar_png.py            # renderiza cada SVG a PNG 2x con Chromium headless
├── generar_script_figma.py    # arma el script de Plugin API embebiendo los SVG
├── figma-checkpoint-1.js      # script: crea los 4 frames y cablea el prototipo
├── 01-catalogo-desktop.svg    # 1440 × 1024
├── 02-detalle-desktop.svg     # 1440 × 1024
├── 03-catalogo-mobile.svg     #  390 × 844
├── 04-filtros-mobile.svg      #  390 × 844
├── 05-flujo.svg               # lienzo de flujo (no es un frame)
└── exports/
    ├── 01-catalogo-desktop.png   2880 × 2048
    ├── 02-detalle-desktop.png    2880 × 2048
    ├── 03-catalogo-mobile.png     780 × 1688
    ├── 04-filtros-mobile.png      780 × 1688
    └── 05-flujo.png              3946 × 1526   ← captura general del flujo
```

Los SVG y los PNG son **derivados**: se regeneran con

```bash
cd TPO/figma-checkpoint-1
python3 generar_frames.py     # SVG
python3 exportar_png.py       # PNG 2x
```

Si mañana cambia un precio o un stock en `catalogo.json`, el prototipo se vuelve a
generar y sigue diciendo la verdad. Ese es el punto: **una sola semilla de datos**,
la misma decisión de arquitectura que ya toma el backend al leer ese JSON del classpath.

---

## Flujo navegable

Punto de inicio del prototipo: **Catálogo Desktop**.

```text
┌──────────────────┐   Ver detalle    ┌─────────────────────┐
│ Catálogo Desktop │ ───────────────► │ Detalle de producto │
│    (inicio)      │ ◄─────────────── │      Desktop        │
└──────────────────┘    Cerrar (×)    └─────────────────────┘

┌──────────────────┐     Filtros      ┌─────────────────────┐
│ Catálogo Mobile  │ ───────────────► │   Filtros Mobile    │
│                  │ ◄─────────────── │                     │
└──────────────────┘ Aplicar filtros  └─────────────────────┘
```

| Origen | Disparador | Destino | Animación sugerida |
|---|---|---|---|
| Catálogo Desktop | tarjeta de producto (o su botón **Ver detalle**) | Detalle de producto Desktop | Dissolve · 200 ms |
| Detalle de producto Desktop | botón **×** (y **Seguir viendo**) | Catálogo Desktop | Dissolve · 200 ms |
| Catálogo Mobile | botón **Filtros** | Filtros Mobile | Move in · Bottom · 250 ms |
| Filtros Mobile | **Aplicar filtros** (y **×**, y el scrim) | Catálogo Mobile | Move out · Bottom · 250 ms |

---

## Relación con el componente Catálogo

Cada zona del diseño existe porque existe una pieza del componente. Nada es decorativo.

| Elemento del prototipo | Qué ejercita del componente |
|---|---|
| Campo de búsqueda (cabecera y panel) | parámetro `q` de `GET /api/productos` — busca por nombre, marca o SKU |
| Control de categoría con conteo | `GET /api/categorias` + `GET /api/productos/facetas` (`porCategoria`) |
| Precio máximo | parámetro `precioMax`; los extremos $ 38.900 – $ 1.849.000 salen de `facetas` |
| Sólo con stock | parámetro `soloStock` |
| Ordenar por | parámetro `orden`: relevancia, menor precio, mayor precio, mejor puntuados, nombre |
| Chips de filtro activas + **Limpiar filtros** | estado de `CriteriosDeBusqueda` reflejado en la UI |
| Product Card | un `ProductoResponse` — la entidad `Producto` nunca se expone |
| Modal de producto | `GET /api/productos/{id}` (y su `404` con cuerpo JSON, vía `RecursoNoEncontradoMapper`) |
| Botón **Agregar al carrito** | sólo estado visual: habilitado, `Sin stock`, tope de stock. El carrito es de otra entrega |

Los 16 productos, sus precios, marcas, SKU, stock, puntajes y características salen
tal cual de `src/main/webapp/assets/data/catalogo.json`. No se inventó ninguno.

---

## Identidad visual

Tomada de `src/main/webapp/assets/css/styles.css`, tema `normal`.

| Token | Valor | Uso en el prototipo |
|---|---|---|
| `--fondo` | `#F2EDE4` | fondo de página |
| `--fondo-alt` | `#E9E0D2` | fondo del lienzo de flujo |
| `--superficie` | `#FBF8F2` | tarjetas, panel de filtros, cabecera, modal |
| `--tinta` | `#1A150F` | texto principal |
| `--tinta-2` | `#574C3D` | texto secundario |
| `--acento` | `#BD3A1E` | botón primario, precios en oferta, rótulos de sección |
| `--profundo` | `#1E332C` | barra de anuncios, marbete «Destacado», chip activa |
| `--mostaza` | `#C78B26` | estrellas de puntaje, detalles de las ilustraciones |

**Tipografía: `Archivo` (interfaz) y `Fraunces` (títulos).** El pedido original decía
`Inter`, pero la aplicación **no usa Inter**: `index.html` carga Archivo y Fraunces
desde Google Fonts y `styles.css` las declara como el sistema tipográfico. Se priorizó
la consistencia con la landing existente, que era el criterio de fondo del pedido.
Las dos están en la librería de Google Fonts de Figma, así que se activan sin instalar nada.

**Ilustraciones.** Cero imágenes de stock. Los dibujos son el puerto literal de las
recetas de `assets/js/ui/art.js`: mismo lienzo de 320 × 240, mismas formas, mismo hash
determinístico que asigna la trama de fondo según el SKU. Sólo cambian dos cosas, y por
una razón concreta:

1. Las `var(--art-*)` quedan resueltas a hexadecimales — Figma no interpreta variables CSS.
2. Se descarta el patrón `#grano` (`feTurbulence`) — Figma no importa filtros SVG.

El encuadre respeta el `aspect-ratio: 4 / 3` que `styles.css` fija en `.producto__medio`,
así que las ilustraciones se ven exactamente igual que en la tienda.

---

## Cómo armar el archivo de Figma

Diez minutos, y queda editable y navegable.

### 1. Crear la página

Archivo nuevo → renombrar la página a **`Checkpoint 1 — Catálogo`**.

### 2. Importar los cuatro SVG

Arrastrar `01`…`04` al canvas, **de a uno**. Figma los importa como grupos vectoriales
con los textos como capas de texto reales: se editan, se recolorean, se mueven.

> No arrastres `05-flujo.svg`: es la captura del flujo para el informe, no un frame.

### 3. Convertir cada grupo en Frame

Seleccionar el grupo importado → `⌘/Ctrl + Alt + G` (**Frame selection**) → renombrar:

| Grupo importado | Nombre del frame | Medidas a fijar |
|---|---|---|
| `01-catalogo-desktop` | `Catálogo Desktop` | 1440 × 1024 |
| `02-detalle-desktop` | `Detalle de producto Desktop` | 1440 × 1024 |
| `03-catalogo-mobile` | `Catálogo Mobile` | 390 × 844 |
| `04-filtros-mobile` | `Filtros Mobile` | 390 × 844 |

En cada frame, activar **Clip content** en el panel derecho.

### 4. Publicar los componentes

Las capas ya vienen con el nombre del componente (`Componente / Product Card — …`,
`Componente / Panel de filtros`, `Componente / Chip de filtro — …`). Para cada uno:
seleccionarlo → **Create component** (`⌘/Ctrl + Alt + K`).

Componentes a publicar — estos, y ni uno más:

`Header` · `Campo de búsqueda` · `Botón primario` · `Botón secundario` ·
`Product Card` · `Chip de filtro` · `Control de categoría` · `Control de stock` ·
`Panel de filtros` · `Modal de producto`

En el `Product Card` y en el `Panel de filtros` conviene aplicar **Auto Layout**
(`Shift + A`) sobre la columna de textos: el resto ya está posicionado y no lo necesita.
No construyas un design system más grande que esto — no es lo que pide el checkpoint.

### Atajo: armarlo con un script

Los pasos 3, 4 y 5 se pueden hacer de una con `figma-checkpoint-1.js`, que usa
`figma.createNodeFromSvg()` — devuelve un `FrameNode` ya armado — y setea las
`reactions` del prototipo. Se ejecuta con **Scripter**, un plugin de Figma Community
que corre en el navegador:

1. `Plugins` → `Scripter` (buscarlo en Community la primera vez)
2. Pegar el contenido de `figma-checkpoint-1.js`
3. `▶`

Los cuatro disparadores son capas con nombre propio en los SVG —
`Hotspot / Cerrar detalle`, `Hotspot / Abrir filtros`, `Hotspot / Aplicar filtros`
y la tarjeta `Componente / Product Card — Auriculares Cancelación Activa` — así que
el script los encuentra por nombre y no por coordenadas.

Queda pendiente sólo el paso 6: publicar el enlace de lectura.

### 5. Cablear el prototipo

Pestaña **Prototype**:

1. `Catálogo Desktop` → tarjeta de producto (o su botón **Ver detalle**) → `On click` →
   `Navigate to` → **Detalle de producto Desktop** · `Dissolve` 200 ms
2. `Detalle de producto Desktop` → botón **×** → `On click` → `Navigate to` →
   **Catálogo Desktop** · `Dissolve` 200 ms
3. `Catálogo Mobile` → botón **Filtros** → `On click` → `Navigate to` →
   **Filtros Mobile** · `Move in` · `Bottom` 250 ms
4. `Filtros Mobile` → **Aplicar filtros** → `On click` → `Navigate to` →
   **Catálogo Mobile** · `Move out` · `Bottom` 250 ms

Seleccionar **Catálogo Desktop** y marcar el **Flow starting point** (`+` en la pestaña
Prototype). Renombrar el flujo a `Checkpoint 1 — Catálogo`.

### 6. Publicar el enlace de lectura

**Share** → `Anyone with the link` → **can view** → copiar el enlace y pegarlo arriba,
en «Enlace de lectura pública».

---

## Aclaración de alcance

Este prototipo cubre **exclusivamente el componente Catálogo del checkpoint 1**.
**Checkout, usuarios (login y registro) y administración quedan para futuras entregas**,
igual que pagos, pedidos, mensajería e integraciones SOAP/REST externas. Si en el Figma
no aparecen, no es un olvido: es el alcance.
