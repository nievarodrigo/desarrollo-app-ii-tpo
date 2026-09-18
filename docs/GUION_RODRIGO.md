---
title: "Guion — Rodrigo"
subtitle: "Apertura, estructura y capas · 5 min · GOMARKET"
date: "Desarrollo de Aplicaciones II -- Entrega Obligatoria N.º 1"
lang: es-AR
mainfont: "Inter"
sansfont: "Inter"
monofont: "JetBrains Mono"
fontsize: 10.5pt
linkcolor: "black"
geometry:
  - a4paper
  - margin=2cm
numbersections: false
header-includes: |
  \setlength{\parskip}{0.4em}
  \setlength{\parindent}{0pt}
...

# Antes de entrar

```bash
cd ~/Materias/DesarrolloAppII/TPO
./scripts/demo.sh --todo
```

Levanta el front, reconstruye el WAR, arranca WildFly y abre el navegador.

## Las ventanas

| # | Ventana | Para |
|---|---|---|
| 1 | **Navegador** `localhost:4321` | abrir, y para Fol / Ivan / Tomás |
| 2 | **DevTools** -> pestaña **Console** | Tomás |
| 3 | **VS Code** | vos y los tres |
| 4 | **Terminal A** en `TPO/` | `capas.py` y los tests |
| 5 | **Terminal B** en `wildfly-41-ee10/` | Nicolás |

## Las pestañas del editor, en pantalla dividida

| Capa | Panel izquierdo (JS) | Panel derecho (Java) |
|---|---|---|
| **datos** | `producto-repository.js` línea **46** | `ProductoRepository.java` línea **18** |
| **negocio** | `catalogo-service-local.js` línea **81** | `CatalogoServiceImpl.java` línea **52** |

Dividís con `Ctrl+\` y saltás a la línea con `Ctrl+G`.

> Para el negocio va `catalogo-service-local.js`, **no** `catalogo-service.js`. Ese
> último es la fachada y la factory, no tiene reglas adentro, y además es el que abre
> Fol.

## Lo que se olvida

1. En la consola del navegador: `localStorage.clear(); location.reload();`
   **Sin esto a Tomás no le sale el 401.**
2. Fuentes: editor **20 pt** · terminal **17 pt** · **consola de DevTools, que va aparte**.
3. Cerrá el cartel de VS Code que ofrece instalar el *Extension Pack for Java*.
4. Cerrá notificaciones, WhatsApp y pestañas de más.

\newpage

# El guion, paso a paso

## 1 · Abrís con la tienda a la vista · 30 s

**Pantalla:** el navegador

> Buenas. Nosotros somos el grupo de **GOMARKET**, un e-commerce. Hoy les traemos la
> primera entrega obligatoria y vamos a mostrar el cliente: tres componentes completos,
> cada uno con su arquitectura en capas.

## 2 · La tesis · 40 s

**Pantalla:** el navegador, sin tocar nada. Decilo despacio y **hacé una pausa**.

> Antes de abrir nada quiero decir la idea que ordena todo lo que sigue.
>
> **Un componente no es una pantalla.**
>
> En la clase tres, cuando nos devolvió el TP inicial, usted dijo que algunos grupos
> estaban mezclando esas dos cosas. **Nosotros éramos uno de esos grupos.** Teníamos
> una landing muy linda con un catálogo, un carrito y un checkout, y les decíamos
> componentes. No lo eran: eran tres pantallas y un archivo de orquestación de treinta
> kilobytes.

## 3 · Qué van a ver y en qué orden · 40 s

**Pantalla:** el navegador todavía

> Lo que hicimos fue reorganizar el cliente **por componente y, dentro de cada
> componente, por capa**. Van a ver tres **componentes**: Catálogo, Carrito y Usuario
> —tres de los cuatro que usted mismo identificó para el caso de la tienda online—. Y
> cada uno tiene adentro sus **tres capas**: presentación, negocio y datos.
>
> El orden va a ser así: yo les muestro la estructura; después Fol toma el Catálogo,
> que es nuestro componente *stateless*; Ivan el Carrito, que es el *stateful*; Tomás
> el componente Usuario, que es el que nos da la seguridad declarativa; y Nicolás
> cierra con las pruebas y con una cosa del servidor.
>
> Una aclaración: dijimos que presentábamos solo el front, pero el componente Catálogo
> **también está implementado en Jakarta EE 10 sobre Java 17**, empaquetado como un WAR
> y desplegado en WildFly 41. Al final Nicolás lo muestra, porque hay un requisito que
> sólo se puede ver ahí.
>
> Empiezo por la estructura, que es lo que ordena todo lo demás.

## 4 · Las carpetas, enfrentadas · 50 s

**Hacés:** Terminal A -> `./scripts/capas.py`

> A la izquierda el **frontend**, lo que corre en el navegador. A la derecha el
> **backend**, lo que corre en el servidor: Jakarta EE sobre WildFly.
>
> Tres componentes, y adentro de cada uno las mismas tres capas. **Es la misma forma de
> los dos lados del cable.**
>
> Lo agrupamos así por una razón concreta, y la vivimos en esta misma entrega. Cuando
> agregamos el componente **Usuario** —el del login y los roles—, **nació entero en una
> sola carpeta**: su vista, su servicio y su repositorio. Lo único que tocamos afuera
> fue el arranque, para cablearlo.
>
> Si hubiéramos agrupado primero por capa, ese mismo componente quedaba **repartido en
> tres ramas del árbol**, con un pedazo de Usuario en cada una. Y el día que haya que
> sacarlo, hay que ir a buscar los restos a tres lugares.
>
> **Las piezas que cambian juntas tienen que estar juntas.** Por eso: primero el
> componente, después la capa.

\newpage

## 5 · La capa de DATOS · 50 s

**Hacés:** `producto-repository.js` línea **46** | `ProductoRepository.java` línea **18**

> Bajemos a las capas. Esto es la capa de **datos**, del lado del cliente y del lado del
> servidor. Y lean el comentario, que es el mismo de los dos lados:
>
> ***«Todos los productos, sin filtrar. El filtrado es decisión del negocio.»***
>
> El repositorio devuelve **todo**, sin filtrar. Y es **a propósito**: quién es visible,
> cómo se busca y cómo se ordena son decisiones del **negocio**, no de la base. Lo
> dejamos tonto a propósito.
>
> Porque si el repositorio filtrara, estaría tomando decisiones de negocio — que es
> justo lo que su lámina 20 marca como **no permitido**: *«datos "subiendo" para tomar
> decisiones de negocio»*.

> **Si preguntan si lo copiaron del front al back:** el comentario de arriba del archivo
> JS dice *"Espeja `catalogo/repository/ProductoRepository.java`"*. Está declarado en el
> código.

> **Si preguntan de dónde salieron los nombres:** de la lámina 35 de la Clase 3, donde
> usted bajó la tienda online a tres capas — `CatálogoView`, `CarritoView`,
> `CarritoService`, `UsuarioService`, `ProductoRepository`, `UsuarioRepository`. Son
> exactamente los nombres de nuestros archivos: *View* en presentación, *Service* en
> negocio, *Repository* en datos.

## 6 · La capa de NEGOCIO · 50 s

**Hacés:** `catalogo-service-local.js` línea **81** | `CatalogoServiceImpl.java` línea **52**

> Y acá está la otra mitad: la capa de **negocio**. Fíjense cómo arranca —pidiéndole al
> repositorio `buscarTodos()`—. **Recién después decide**: filtra por activo, por
> categoría, por texto, por stock, por precio, y ordena.
>
> El repositorio entrega. El negocio decide. **Y es la misma cadena en los dos
> lenguajes.**

```js
// JavaScript — catalogo-service-local.js
const todos = await this.productos.buscarTodos();
todos.filter((p) => p.activo)
     .filter((p) => this.#cumpleCategoria(p, criterios))
     .filter((p) => this.#cumpleTexto(p, criterios))
     .filter((p) => this.#cumpleStock(p, criterios))
     .filter((p) => this.#cumplePrecio(p, criterios))
     .sort(ESTRATEGIAS_DE_ORDEN[criterios.orden]);
```

```java
// Java — CatalogoServiceImpl.java
productos.buscarTodos().stream()
         .filter(Producto::isActivo)
         .filter(p -> cumpleCategoria(p, criterios))
         .filter(p -> cumpleTexto(p, criterios))
         .filter(p -> cumpleStock(p, criterios))
         .filter(p -> cumplePrecio(p, criterios))
         .sorted(comparador(criterios.orden()));
```

## 7 · Lo que no es de nadie · 25 s

**Hacés:** abrís la carpeta `compartido/`

> Una última cosa. Esta carpeta **no es un componente**: son servicios técnicos
> transversales —el transporte HTTP, el acceso al almacenamiento, el manejo del foco en
> los modales, el formato de los precios—. El equivalente de lo que el contenedor le
> provee al backend sin que ninguno lo implemente.

## 8 · El puente y la palabra · 15 s

> Y una aclaración, para que se entienda qué estuve mostrando: **esto es la explicación
> técnica.** Vieron el contrato y las reglas, no la app corriendo. Fol ahora toma el
> Catálogo y les muestra esto mismo **ejecutándose** sobre el catálogo real.
>
> **Fol sigue con el Catálogo.**

\newpage

# Por las dudas: las palabras que nombrás

## stateless — "sin estado"

**No guarda nada entre una llamada y la siguiente.** Preguntale dos veces lo mismo y
contesta dos veces lo mismo.

El **Catálogo** es stateless: `buscar("cafe")` devuelve lo mismo siempre, sin importar
qué buscaste antes ni quién sos.

**Consecuencia:** cualquier instancia puede atender a cualquiera. Por eso el servidor
tiene un *pool* y reparte pedidos sin que importe cuál te toca. Eso es `@Stateless`.

## stateful — "con estado"

**Guarda estado conversacional:** información que pertenece a **un** cliente y se va
acumulando a lo largo de una secuencia.

El **Carrito** es stateful: `agregar(producto)` depende de todo lo que hiciste antes. Si
el producto tiene stock 2 y ya llevás 2, te lo rechaza. **La misma llamada, con el mismo
argumento, da distinto resultado.**

**Consecuencia:** cada cliente necesita su propia instancia. El servidor no puede darte
cualquiera del pool: tiene que darte *la tuya*. Eso es `@Stateful`.

> **El error a evitar:** no es "guarda cosas o no guarda". Un caché no te hace stateful.
> Lo que define es si el resultado **depende de la historia previa de ese cliente**.

## seguridad declarativa

**Declarás la regla junto a la operación, en vez de escribir un `if` adentro.**

```java
@RolesAllowed({"ADMIN"})              <- la regla, declarada
public List<Usuario> listarUsuarios() { ... }
```

El contenedor la intercepta y la hace cumplir **antes** de que el método corra. Se lee
en la firma, vale para **todas** las invocaciones, y la operación queda limpia de código
de seguridad.

Lo opuesto es *seguridad programática*: un `if (rol == ADMIN)` enterrado adentro, que hay
que acordarse de poner en cada rama.

## producto-repository.js

Es la capa de **datos** del Catálogo. Dos métodos: `buscarTodos()`, que devuelve todos
los productos del JSON **sin filtrar**, y `buscarPorId(id)`. **No decide nada.**

```
producto-repository.js      TRAE      <- no decide
catalogo-service-local.js   DECIDE    <- acá se filtra y se ordena
```

## El stack, si lo preguntan

| | |
|---|---|
| Java | **17** |
| Jakarta EE | **10.0.0** |
| Servidor | **WildFly 41**, perfil EE 10 Full Platform |
| Empaquetado | WAR único (`ecommerce.war`) |

> **Jakarta EE es la especificación, no el producto.** Define contratos; WildFly los
> implementa. Podría ser Payara u Open Liberty. Por eso la app es portable.

\newpage

# Si te preguntan a vos

| Pregunta | Respuesta |
|---|---|
| *¿Dónde está la capa de datos?* | En `componentes/<x>/datos/` del lado del cliente y en `catalogo/repository/` del lado del servidor. Son DAOs: entregan filas, no deciden. |
| *¿`compartido` no rompe la separación?* | No: no tiene reglas de negocio de ningún componente. Es infraestructura -- transporte HTTP, foco, formato. |
| *¿Qué pasa si un componente necesita a otro?* | Se inyecta desde `main.js` como función. El carrito recibe `alExigirSesion`; no importa el módulo de Usuario. |
| *¿No es mucha carpeta para tan poco código?* | Es el costo de la separación, y el beneficio ya se cobró: la capa de negocio se prueba sin navegador. |
| *¿Por qué no usaron un framework?* | Los módulos ES nativos ya dan separación real y el despliegue es copiar archivos. La complejidad se agrega cuando duele, no antes. |
| *¿Son componentes de verdad si no hay contenedor?* | Cumplen interfaz explícita e implementación oculta. La tercera condición -- que un contenedor gestione el ciclo de vida -- no se cumple en el navegador. Por eso al final mostramos WildFly. |
| *¿Qué diferencia hay entre un objeto y un componente?* | El objeto lo instancio yo y quien lo usa conoce su clase. El componente lo crea el contenedor y quien lo usa conoce sólo su interfaz. |
| *¿Por qué componente primero y capa después?* | Porque las piezas que cambian juntas tienen que estar juntas. Cuando agregamos Usuario, nació entero en una carpeta. |
| *¿Qué cambiarían si empezaran de nuevo?* | Habríamos organizado por componente y capa desde el día uno. Empezamos por pantallas y tuvimos que reestructurar. |

---

# Las tres frases que tenés que decir sí o sí

1. **"Un componente no es una pantalla."**
2. **"Es la misma forma de los dos lados del cable."**
3. **"El repositorio entrega. El negocio decide."**

---

# Y si algo falla en vivo

No lo pelees. Pasá al código y contá qué **debería** pasar. Los 84 tests te respaldan, y
Nicolás los corre al final.
