---
title: "Guía de estudio — las bases"
subtitle: "Desarrollo de Aplicaciones II · Entrega Obligatoria N.º 1 · GOMARKET"
date: "Comisión Lunes TM -- 2.º Cuatrimestre 2026"
lang: es-AR
mainfont: "Inter"
sansfont: "Inter"
monofont: "JetBrains Mono"
fontsize: 10.5pt
linkcolor: "black"
geometry:
  - a4paper
  - margin=2cm
toc: true
toc-depth: 2
numbersections: false
header-includes: |
  \usepackage{float}
  \makeatletter
  \renewcommand*{\fps@figure}{H}
  \makeatother
  \setlength{\parskip}{0.4em}
  \setlength{\parindent}{0pt}
...

\newpage

# Para qué sirve esto

No es el guion. El guion dice **qué** decir; esto explica **por qué**.

Si entendés estos seis conceptos podés contestar cualquier pregunta, incluso una que
no esté en el guion. Si sólo memorizás el guion, la primera pregunta lateral te deja
sin nada.

La consigna evalúa *"defensa oral y comprensión individual"* con un 15 %, y ahí la
cátedra puede poner **notas distintas a integrantes del mismo grupo**. O sea: esto es
lo que separa tu nota de la del resto.

---

# 1 · ¿Qué es un componente?

Un componente es **una pieza de software que se puede reemplazar sin tocar lo que la
usa.**

Para que eso sea posible necesita tres cosas:

| | Qué significa | En GOMARKET |
|---|---|---|
| **Interfaz explícita** | un contrato público: qué operaciones ofrece | `CatalogoService` expone `buscar`, `obtener`, `listarCategorias`, `contarPorCategoria`, `rangoDePrecios` |
| **Implementación oculta** | nadie de afuera sabe cómo lo hace | nadie sabe si los datos vienen de JPA, de un JSON o de una API |
| **Vive en un contenedor** | otro lo crea, lo destruye y le inyecta lo que necesita | WildFly instancia `CatalogoServiceImpl` |

**La prueba de fuego** es una sola pregunta:

> ¿Puedo cambiar el adentro sin que se entere el afuera?

En nuestro proyecto la respuesta es sí, y tenemos el caso ideal: `CatalogoService` tiene
**dos implementaciones** —una que delega en el backend Java y otra con reglas propias— y
la presentación **no sabe cuál está activa**. Eso no es un truco de programación: es la
definición de componente, funcionando.

## Componente contra objeto

| | Objeto | Componente |
|---|---|---|
| ¿Quién lo crea? | vos, con `new` | **el contenedor** |
| ¿Qué conoce quien lo usa? | normalmente su clase | **sólo su interfaz** |
| ¿Cómo se configura? | en el código | **desde afuera**, por anotaciones o descriptores |
| Unidad de | lenguaje | **despliegue** |

La frase corta:

> **"Un objeto lo instancio yo. Un componente me lo da el contenedor."**

## Componente contra pantalla

Esta es **la** distinción de nuestra presentación, y la que la cátedra marcó en la
devolución del TP Inicial:

> *"Algunos grupos mezclan **componente** con **pantalla**."*

Una **pantalla** es lo que se ve. Un **componente** es una responsabilidad del negocio,
con su interfaz y sus tres capas.

El Carrito es un componente **aunque no se vea**: tiene reglas (tope de stock, cálculo
del total), tiene estado y tiene persistencia. El panel lateral que se abre es apenas
**una** de sus tres capas.

> **"Un componente no es una pantalla."**

---

# 2 · ¿Qué son las capas?

## La definición

> Una **capa** es un grupo de responsabilidades **del mismo tipo**.

No es una carpeta, ni un archivo, ni un paquete. Es un **criterio para decidir dónde va
cada cosa**.

La idea es cambiar la pregunta que te hacés al escribir código. En vez de:

> *"¿de qué trata esto? ¿de productos, del carrito, de usuarios?"*

te preguntás:

> *"¿qué **tipo de decisión** toma esto? ¿mostrar, decidir o persistir?"*

Todo el código que toma decisiones del mismo tipo va junto. Eso es una capa.

## El problema que resuelve

Sin capas, un método hace las tres cosas a la vez — arma el HTML, aplica la regla de
negocio y escribe en la base. Es el ejemplo que la cátedra mostró en la Clase 3:

```java
class CarritoComponent {
  // HTML mezclado con SQL y con la regla de negocio, todo en el mismo lugar
  void agregarProducto() { ... }
}
```

Cuando todo está mezclado, pasan cuatro cosas, y ninguna es teórica:

1. un cambio de **diseño visual** te obliga a tocar la lógica de negocio;
2. un cambio en la **base de datos** rompe pantallas que no deberían enterarse;
3. **escribir un test** es casi imposible sin levantar la aplicación entera;
4. **reemplazar una tecnología** implica reescribir el componente completo.

## La arquitectura en capas

Es un **patrón arquitectónico**: una forma acordada de estructurar un sistema. Dice
tres cosas.

**Primera: hay tres capas**, y cada una agrupa un tipo de responsabilidad.

```
PRESENTACIÓN   qué se MUESTRA         dibuja y captura clics. No decide nada.
      │
      ▼
NEGOCIO        qué reglas se APLICAN  decide. No sabe de pantallas ni de bases.
      │
      ▼
DATOS          de dónde SALEN         trae y guarda filas. No decide nada.
```

**Segunda: cada capa habla sólo con la inmediata siguiente.** La presentación le pide
al negocio; el negocio le pide a los datos. La presentación **nunca** toca la base.

**Tercera: las dependencias van en una sola dirección — hacia abajo.** El negocio no
sabe que existe una pantalla. Los datos no saben que existe una regla. Por eso el
negocio se puede probar sin navegador: no depende de nadie que esté arriba.

| Permitido | Prohibido |
|---|---|
| presentación → negocio → datos | presentación → datos (saltear el negocio) |
| una capa expone una interfaz para la de arriba | que los datos decidan reglas de negocio |
| cambiar una capa sin tocar las otras | saltear una capa "para ir más rápido" |

## Qué NO es una capa

- **No es una carpeta.** Podés tener las carpetas perfectas y las capas mezcladas si el
  repositorio filtra o la vista calcula el total.
- **No es un componente.** Un componente se parte *en* capas; las capas viven *adentro*
  de cada componente. Nosotros tenemos tres componentes × tres capas.
- **No es una pantalla.** La presentación es sólo **una** de las tres.

## Y el trade-off, porque no es gratis

Más capas significa más indirección: cada petición atraviesa más pasos, hay más piezas
que coordinar, y en una aplicación muy chica puede ser más estructura de la necesaria.
No existe la arquitectura perfecta: es una decisión según el tamaño y el contexto.

En este proyecto se justifica porque la consigna pide componentes reemplazables y
porque ya cobramos el beneficio: **la capa de negocio se prueba sin navegador.**

## Cómo se ve en nuestro código

Abrí `producto-repository.js` y leé el comentario del método:

> *"Todos los productos, **sin filtrar**. El filtrado es decisión del negocio."*

El repositorio devuelve **todo**. Quién es visible, cómo se busca y cómo se ordena lo
decide `CatalogoService`. Si el repositorio filtrara, estaría tomando una decisión que
no le corresponde — el anti-patrón exacto que la cátedra listó en la Clase 3.

Y el `ProductoRepository.java` del backend dice lo mismo, con las mismas palabras,
porque lo escribimos primero ahí.

## Por qué importa

Si mezclás las capas:

- un cambio de color te obliga a tocar la lógica de negocio;
- un cambio en la base rompe pantallas que no deberían enterarse;
- **no podés probar nada sin levantar todo.**

Nuestra prueba de que están separadas de verdad no es un diagrama: es que
`catalogo-service-local.test.mjs` **corre sin navegador, sin red y sin base de datos**.
Le inyectamos repositorios falsos en tres líneas.

> **"Si las capas estuvieran mezcladas, ese archivo no podría existir."**

---

\newpage

# 3 · Contenedor y servidor de aplicaciones

## Las dos definiciones

> Un **contenedor** es un entorno de ejecución que **administra el ciclo de vida de los
> componentes** y les provee servicios técnicos —inyección de dependencias,
> transacciones, seguridad— sin que ellos los tengan que programar.

> Un **servidor de aplicaciones** es el software que **aloja uno o más contenedores** y
> expone los protocolos de red hacia afuera: HTTP, RMI, JMS. También administra los
> recursos compartidos: pools de conexión, pools de hilos, colas de mensajes.

La relación entre los dos: **sin servidor, el contenedor no tiene dónde ejecutarse ni
con qué comunicarse hacia afuera.**

Y la diferencia en una línea:

> **El contenedor es el que trabaja. El servidor es la casa donde vive el contenedor.**

Acá se confunde casi todo el mundo, así que conviene tenerlo dibujado:

```
SERVIDOR DE APLICACIONES   (WildFly, Payara, TomEE…)
  │   aloja contenedores y expone los protocolos hacia afuera: HTTP, RMI, JMS
  │   administra recursos compartidos: pools de conexión, de hilos, colas
  │
  ├── CONTENEDOR WEB        Servlets, JAX-RS  (nuestros @Path)
  │
  └── CONTENEDOR EJB / CDI  los componentes de negocio
        │
        └── gestiona:  ciclo de vida · inyección · transacciones · seguridad
```

## Qué hace el contenedor por vos

Cuatro cosas que, si no, las escribís a mano:

1. **Ciclo de vida** — crea, agrupa en un pool, pasiva y destruye instancias. Vos
   declarás *cuándo* querés que pase algo; él lo ejecuta.
2. **Inyección de dependencias** — ponés `@Inject` y aparece la implementación. No
   hacés `new` nunca.
3. **Transacciones** — `@TransactionAttribute` y demarca solo, incluso entre varios
   recursos.
4. **Seguridad** — `@RolesAllowed` y bloquea **antes** de que tu método corra.

## Nuestra evidencia, en vivo

En `CatalogoEnMemoria.java` hay un método anotado:

```java
@ApplicationScoped
public class CatalogoEnMemoria implements ProductoRepository, CategoriaRepository {

    @PostConstruct
    void cargar() { ... }
```

Cuando arranca WildFly, en el log aparece:

```
Catálogo cargado: 16 productos, 5 categorías
```

**Nadie llamó a `cargar()`.** Lo llamó el contenedor CDI al crear el bean, antes del
primer uso. Esa línea es toda la demostración del ciclo de vida gestionado.

> **"Nadie llamó a `cargar()`. Lo llamó el contenedor."**

## Por qué `@ApplicationScoped` y no `@Stateless` ahí

Porque queremos **una sola instancia por despliegue**, para leer el catálogo una única
vez. `@Stateless` daría un pool de instancias y cada una leería el archivo de nuevo.

---

# 4 · Jakarta EE: la especificación, no el producto

Es el título literal de la clase, y es lo que más suma si lo decís bien.

**Jakarta EE es un conjunto de contratos** — interfaces y anotaciones. No es un programa
que se instala. Define *qué significa* `@Stateless`, pero no lo implementa.

**Quien lo implementa es un servidor**: WildFly, Payara, Open Liberty, TomEE, WebLogic.

## La analogía que funciona

Jakarta EE es **la norma del enchufe**. WildFly es **la marca del tomacorriente** que
compraste. Tu aplicación es el aparato: si respeta la norma, entra en cualquier
tomacorriente.

## Por qué importa

**Portabilidad.** Como programás contra el estándar y no contra un producto, podés
cambiar de servidor sin reescribir el código de negocio.

## Nuestros números

| | |
|---|---|
| **Java** | 17 |
| **Jakarta EE** | 10.0.0 |
| **Servidor** | WildFly 41.0.0.Final, perfil EE 10 Full Platform |
| **Empaquetado** | WAR único (`ecommerce.war`) |
| **Namespace** | 30 imports, todos `jakarta.*`, cero `javax.*` |

Ese último dato es fino y suma: el namespace cambió de `javax.*` a `jakarta.*` en
Jakarta EE 9 (2020), y **rompe compatibilidad binaria** con el código legado. Decirlo
demuestra que leíste, no que copiaste.

## Los tres errores que te delatan

| No digas | Decí |
|---|---|
| *"Usamos Jakarta"* (como si fuera un servidor) | *"Jakarta EE es la **especificación**; WildFly la implementa. Podría ser Payara u Open Liberty."* |
| *"WildFly es el framework"* | *"WildFly es el **servidor de aplicaciones**."* |
| *"El servidor gestiona los componentes"* | *"El **contenedor** gestiona los componentes; el servidor aloja contenedores."* |

---

\newpage

# 5 · ¿Dónde se despliega?

## Qué significa "desplegar"

> **Desplegar** es poner la aplicación a disposición de un contenedor, para que la
> ejecute y le dé sus servicios.

No es "subirla a un servidor" en el sentido de copiar archivos a un hosting. Es
entregarle un paquete con una estructura acordada a un contenedor que sabe leerlo,
instanciar los componentes que hay adentro y empezar a atender pedidos.

```
  código Java
      |  Maven empaqueta
      v
  ecommerce.war
      |  se copia a deployments/
      v
  WildFly  ->  http://localhost:8080/ecommerce/
```

Un **WAR** (*Web Application Archive*) es un zip con una estructura acordada: tus
clases compiladas, las librerías y el contenido web. Lo copiás a `deployments/` y **el
servidor lo detecta y lo despliega solo**. No hay instalador ni configuración.

```bash
./mvnw clean package                                     # → target/ecommerce.war
cp target/ecommerce.war $WILDFLY/standalone/deployments/
$WILDFLY/bin/standalone.sh                               # arranca
```

## ¿Y entonces qué es Vercel?

Vercel sirve **archivos estáticos**. No corre Java, no hay contenedor, no existen
`@Stateless` ni `@PostConstruct`.

**Vercel no puede ser el despliegue que evalúa la consigna**, que pide textual *"un
componente completo desplegado en un servidor de aplicaciones o contenedor real"*.

Pero no es inútil: el mismo front funciona en los dos destinos y **elige solo**. Al
arrancar consulta `GET api/salud`; si hay backend usa la API, si no usa el JSON
estático. Eso es el patrón **Factory** eligiendo entre dos implementaciones del mismo
contrato.

> **"Vercel demuestra que el cliente es independiente de la fuente. WildFly demuestra
> que hay un contenedor gestionando el ciclo de vida. Las dos suman; ninguna reemplaza
> a la otra."**

---

# 6 · Stateful contra stateless

## Las definiciones

> El **estado** de un componente es la información que conserva **entre una llamada y la
> siguiente**.

> Un componente **stateless** (sin estado) no conserva nada entre llamadas. Cada
> invocación se resuelve sola, con lo que le pasan por parámetro.

> Un componente **stateful** (con estado) conserva **estado conversacional**:
> información que pertenece a **un** cliente y se va acumulando a lo largo de una
> secuencia de operaciones.

Ojo con la palabra *conversacional*, que es la que importa: no es cualquier dato
guardado, es el hilo de una conversación con un cliente puntual. Un catálogo que cachea
resultados en memoria sigue siendo stateless, porque ese caché no pertenece a nadie en
particular ni cambia lo que contesta.

## El error más común

**No es "guarda cosas o no guarda".** La pregunta correcta es:

> ¿El resultado de una operación depende de lo que pasó antes?

| | **Catálogo** | **Carrito** |
|---|---|---|
| | `buscar("cafe")` da lo mismo siempre | `agregar(producto)` depende de qué agregaste antes |
| | el orden de las llamadas no importa | el orden **sí** importa |
| | cualquier instancia atiende a cualquiera | cada cliente necesita **su** instancia |
| | **stateless** → `@Stateless`, pool compartido | **stateful** → `@Stateful`, instancia por cliente |

La frase que lo cierra:

> **"La misma llamada, con el mismo argumento, da distinto resultado según la historia
> previa. Eso es estado conversacional."**

Y el detalle que suma: **el total del carrito nunca se persiste**, se recalcula desde
los ítems cada vez. Guardar un total sería guardar una verdad que puede vencer — si
cambia un precio o una cantidad, el total mentiría.

---

\newpage

# Las cinco frases

Si tenés diez minutos, estudiá estas:

1. **"Un componente no es una pantalla."**
2. **"Un objeto lo instancio yo; un componente me lo da el contenedor."**
3. **"Jakarta EE es la especificación; WildFly la implementa."**
4. **"El contenedor gestiona los componentes; el servidor aloja contenedores."**
5. **"Nadie llamó a `cargar()`. Lo llamó el contenedor."**

---

# Preguntas incómodas, y cómo salir

| Pregunta | Respuesta |
|---|---|
| *¿Qué diferencia hay entre un objeto y un componente?* | El objeto lo instancio yo y quien lo usa conoce su clase. El componente lo crea el contenedor y quien lo usa conoce sólo su interfaz. Uno es unidad de lenguaje, el otro de despliegue. |
| *¿Por qué decís que el catálogo es un componente y no una pantalla?* | Porque tiene interfaz explícita, tres capas y dos implementaciones intercambiables. La pantalla es apenas su capa de presentación. |
| *¿Qué hace exactamente el contenedor?* | Ciclo de vida, inyección de dependencias, transacciones y seguridad. Cuatro cosas que si no, las escribo a mano. |
| *¿Jakarta EE es un framework?* | No: es una **especificación**, un conjunto de contratos. El framework —o mejor dicho, el servidor— es WildFly, que la implementa. |
| *¿Dónde está desplegado?* | Como un WAR en WildFly 41, bajo el context root `/ecommerce`. Maven lo empaqueta y el servidor lo detecta solo al copiarlo a `deployments/`. |
| *¿Por qué no lo desplegaron en Vercel y listo?* | Porque Vercel es hosting estático: no corre Java ni tiene contenedor. La consigna pide un servidor de aplicaciones real. Vercel lo usamos como espejo del front, no como despliegue. |
| *Mostrame dónde está la capa de datos.* | En `componentes/<x>/datos/` del lado del cliente y en `catalogo/repository/` del lado del servidor. Son DAOs: entregan filas, no deciden nada. |
| *¿Por qué el carrito es stateful y el catálogo no?* | Porque `agregar()` depende de la historia previa del cliente y `buscar()` no depende de nada. Hay un test de cada cosa. |
| *Si te sacan el contenedor, ¿sigue funcionando?* | Las reglas de negocio sí: los tests las ejercitan sin contenedor. Lo que se pierde es lo que el contenedor aporta — pool, inyección, transacciones y seguridad declarativa. |
| *¿Qué harías distinto si empezaras de nuevo?* | Habría arrancado organizando por componente y capa desde el día uno. Empezamos por pantallas y tuvimos que reestructurar. |

---

# Autoexamen

Tapá la columna de la derecha del cuadro anterior y respondé en voz alta. Si dudás en
alguna, volvé a la sección que corresponde.

Y el test real: **¿podés explicar tu bloque entero sin abrir el navegador?**

Si la respuesta es sí, entendiste. Si sólo podés señalando la pantalla, estás
explicando una pantalla — y ese es justamente el error que la cátedra ya marcó.
