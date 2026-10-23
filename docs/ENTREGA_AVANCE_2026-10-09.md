---
title: "GOMARKET · avance para la clase del 09/10"
subtitle: "Guion por integrante y relación con Mensajería, Servicios Web y REST"
author: "Rodrigo · Iván · Tomás · Fol · Nicolás"
date: "9 de octubre de 2026"
lang: es-AR
geometry: margin=2.1cm
fontsize: 11pt
colorlinks: true
---

> **Mensaje común para abrir la exposición.** Esta es una entrega de avance.
> Funciona el recorrido **REST → cola JMS → consumidor MDB → consulta REST**
> en WildFly con el perfil `standalone-full.xml`. No es una compra real: aún no
> hay cobro, reserva de stock, autenticación del servidor ni base de datos de
> pedidos. El SOAP de la clase 9 se trabajó como diseño hipotético, no se
> presenta como integración funcional.

## Qué se cambió en el código — antes y ahora

**Antes de este avance:** el backend Java sólo exponía el catálogo. El botón
“Confirmar compra” fabricaba un pedido en el navegador, esperaba 1,1 segundos
para simular una operación y mostraba “Pago aprobado”, aunque no existían un
pedido en el servidor, una cola ni un cobro.

**Ahora, si la tienda corre en WildFly:** el botón envía productos y cantidades
al servidor. El servidor verifica stock y calcula el total, acepta el pedido
con HTTP `202`, lo envía a una cola JMS real y un MDB recibe el mensaje. La
interfaz muestra **Pedido recibido** y permite consultar el estado. En el sitio
estático sigue habiendo una simulación local, pero se identifica como tal.

| Cambio | Archivos | Resultado visible |
|:--|:--|:--|
| **Nuevo componente Pedido** | `PedidoResource.java`, `PedidoService.java`, `ProcesarPedidoMDB.java` | `POST /api/pedidos` acepta; el MDB consume; `GET /api/pedidos/{id}` informa estado. |
| **Nuevo contrato y estado** | `SolicitudPedido.java`, `Pedido.java`, `PedidoEnMemoria.java` | Se envían ítems sin precio; el servidor calcula total y guarda `PENDIENTE` / `PROCESADO` durante la demo. |
| **Interfaz modificada** | `carrito-view.js`, `cliente-http.js` | El carrito usa REST cuando hay backend, consulta el estado y no inventa un pago aprobado. |
| **Pruebas nuevas** | `PedidoServiceTest.java` | Verifican total, envío gratis, stock insuficiente, duplicados y pedido vacío. |
| **Instrucciones actualizadas** | `README.md` | Indica iniciar WildFly con `standalone-full.xml`, necesario para Artemis. |

Los seis archivos Java nuevos están en la carpeta `src/main/java/.../pedido/`;
la prueba en `src/test/java/.../pedido/` y los dos archivos de interfaz bajo
`src/main/webapp/assets/js/`. **No se cambió el catálogo existente ni se
implementó SOAP.**

**Ejemplo concreto:** `POST /api/pedidos` con
`{"items":[{"idProducto":1,"cantidad":1}]}` devuelve `202` y un `id`.
Luego `GET /api/pedidos/{id}` muestra `PROCESADO` cuando el MDB recibe el
mensaje. Si se piden 999 unidades, devuelve `400` por stock insuficiente.

## Qué mostrar en vivo (2 minutos)

1. En WildFly, entrar a la tienda, agregar un producto y enviar el pedido.
   La interfaz muestra **Pedido recibido**, no “pago aprobado”.
2. Pulsar **Consultar estado**: la respuesta pasa de `PENDIENTE` a `PROCESADO`
   cuando el MDB toma el mensaje. La consulta también puede hacerse con
   `GET /ecommerce/api/pedidos/{id}`.
3. Enviar una cantidad mayor al stock: `POST /ecommerce/api/pedidos` devuelve
   `400`, y el carrito no se vacía. Mostrar el test de esa regla.
4. Señalar en el código `PedidoResource`, `PedidoService`, `ProcesarPedidoMDB`
   y `PedidoEnMemoria`. El flujo depende de una cola real del broker Artemis.

**Arranque:** `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw clean package`
y WildFly con `standalone.sh -c standalone-full.xml`. Desplegar
`target/ecommerce.war`. El perfil `standalone.xml` común **no habilita
mensajería**.

\newpage

## Rodrigo — líder y coordinación

### Apertura de Rodrigo: presentación del equipo (aprox. 1 minuto)

“Buen día. Somos el equipo de GOMARKET: **Iván, Tomás, Fol, Nicolás y yo,
Rodrigo**. Dividimos el trabajo por responsabilidades principales para que
cada parte tenga alguien que la conduzca, pero revisamos el avance entre todos.

**Iván está en backend** y va a mostrar la API de pedidos, la cola JMS y el
consumidor que recibe los mensajes. **Tomás está en frontend** y va a enseñar
qué ve el usuario cuando envía un pedido y consulta su estado. **Fol lleva
datos**: nos va a explicar qué información viaja en el pedido, cómo se calcula
el total y qué parte todavía está en memoria. **Nicolás está en QA**: va a
mostrar las pruebas y cómo verificamos que el mensaje realmente llega al
consumidor, no sólo que la pantalla parece funcionar.

**Yo soy el líder del equipo**: organicé el avance y hoy voy a conectar esas
partes en una demostración única, explicar por qué elegimos esta arquitectura
y dejar claro qué funciona y qué falta. Antes, el pedido se simulaba sólo en
el navegador. Ahora podemos mostrar el recorrido real **REST → cola JMS → MDB
→ consulta de estado** en WildFly. No vamos a decir que hay un pago real,
porque todavía no lo hay.”

> Esta apertura presenta **roles y partes a exponer**. Antes de usarla, el
> equipo debe confirmar la autoría real de cada archivo; no se atribuyen
> commits individuales sin evidencia.

### Si me preguntan por las decisiones técnicas

“De la **clase 8** aplicamos una cola, no un tópico, porque cada pedido debe
procesarlo un solo consumidor. De la **clase del 09/10** usamos REST para
recibir el pedido y consultar su estado. De la **clase 9** tomamos SOAP/WSDL
como diseño hipotético para un ERP legado; no lo agregamos al código porque
nuestro negocio actual no tiene ese proveedor.”

**Dónde señalar (bajo `src/main/java/.../pedido/`):**

- `controller/PedidoResource.java`: entrada y consulta REST.
- `service/PedidoService.java`: validación y publicación.
- `service/ProcesarPedidoMDB.java`: consumo de la cola.

**Por qué y beneficio:** REST da una entrada simple y verificable; la cola
desacopla temporalmente la recepción del procesamiento; las responsabilidades
separadas hacen más fácil cambiar el consumidor sin tocar el endpoint.

**Pregunta probable:** “¿La respuesta 202 significa compra confirmada?”
**Respuesta:** “No. Sólo que se aceptó el pedido para procesarlo. `PROCESADO`
demuestra recepción por el MDB, no cobro ni reserva.”

\newpage

## Iván — backend

**Lo que diría:**

“Implementé una API JAX-RS. `POST /api/pedidos` recibe identificadores y
cantidades, no precios. `PedidoService` busca los productos en el catálogo,
revalida stock y calcula el total en el servidor. Luego serializa un mensaje
JSON y lo publica en `ProcesarPedidoQueue` con entrega persistente. El MDB,
que es un *Message-Driven Bean*, escucha esa cola y actualiza el estado. Con
`GET /api/pedidos/{id}` verificamos que el mensaje efectivamente llegó.”

“El destino se declara con `@JMSDestinationDefinition` y el consumidor con
`@MessageDriven`. WildFly usa Artemis con el perfil `standalone-full.xml`.
Cuando probamos el endpoint, devolvió 202 y segundos después el GET mostró
`PROCESADO`; una cantidad 999 para un producto con stock menor devolvió 400.”

“Esto implementa el esquema **productor → broker → consumidor de la clase 8**.
Elegimos punto a punto porque cada pedido es trabajo para un único consumidor.
La ventaja es responder rápido al usuario aunque el procesamiento continúe
después. Con REST usamos un `202`: aceptado no quiere decir terminado.”

**Dónde señalar:** `PedidoService.java` (validación y publicación),
`ProcesarPedidoMDB.java` (consumo) y `PedidoResource.java` (REST).

**Clase aplicada:** Unidad V / clase 8: productor, broker, cola y consumidor
P2P; clase del 09/10: recursos REST, verbos HTTP, JSON y códigos 202/400.

**Beneficio y límite:** Se evita que el tiempo del consumidor bloquee el
`POST`. La cola ayuda a amortiguar demoras; **todavía falta** una persistencia
transaccional de pedidos y una política robusta de reintentos/idempotencia
antes de llamarlo solución de producción.

\newpage

## Tomás — frontend

**Lo que diría:**

“La interfaz tiene dos modos. Si está servida por WildFly, detecta el backend,
manda el carrito con `POST /api/pedidos` y muestra un estado pendiente. No
declara que el pago fue aprobado. El botón ‘Consultar estado’ hace un GET y
refleja lo que respondió el servidor. Si la tienda se abre como sitio estático,
conserva el modo local, pero ahora se rotula claramente como simulación.”

“Enviamos sólo `idProducto` y `cantidad`. No mandamos el total calculado en el
navegador como verdad. Si el POST falla, dejamos el carrito intacto para que la
persona pueda corregirlo o reintentar.”

“Aplicamos **REST, tema de la clase del 09/10**: `POST` envía JSON al recurso
Pedido y `GET` consulta su estado. Así la interfaz no necesita saber cómo
funciona Artemis; el servidor puede cambiar su procesamiento sin cambiar la
pantalla.”

**Dónde señalar (bajo `src/main/webapp/assets/js/`):**

- `componentes/carrito/presentacion/carrito-view.js`: interfaz.
- `compartido/cliente-http.js`: transporte REST.

**Por qué y beneficio:** REST separa presentación de negocio; el usuario
recibe una respuesta rápida y honesta sobre el estado; no se pierde su trabajo
ante un error de red. El front es una ayuda de uso, **no la frontera de
seguridad**: la validación definitiva debe estar en el servidor.

**Pregunta probable:** “¿Qué pasa si no está WildFly?” **Respuesta:** “La
landing estática continúa, pero sólo simula el pedido y así lo informa.”

\newpage

## Fol — datos

**Lo que diría:**

“Definimos un contrato de entrada mínimo: una lista de ítems con identificador
de producto y cantidad. Se rechazan pedidos vacíos, cantidades no positivas,
productos repetidos y cantidades superiores al stock conocido por el servidor.
El precio y el envío se recalculan contra el catálogo. Así un usuario no puede
cambiar el total desde las herramientas del navegador.”

“Para este avance el estado del pedido vive en `PedidoEnMemoria`, un mapa
concurrente. Es suficiente para mostrar `PENDIENTE` y `PROCESADO`, pero no
sobrevive a reiniciar WildFly. La base persistente y la reserva real de stock
son próximos pasos; no diríamos que esta implementación ya los resuelve.”

“De la **clase 9 de servicios web** tomamos SOAP y WSDL para diseñar, aparte,
un caso hipotético con un ERP legado. Si el proveedor sólo ofreciera SOAP,
su contrato permitiría validar stock y devolver un Fault. Hoy ese proveedor
no existe en GOMARKET, así que no agregamos código SOAP innecesario.”

**Dónde señalar:** `SolicitudPedido.java`, `Pedido.java` y
`PedidoEnMemoria.java` en el componente Pedido; `CatalogoEnMemoria.java` en
Catálogo.

**Por qué y beneficio:** Un mensaje con `id` UUID permite correlacionar el
POST, el trabajo del MDB y el GET. Separar contrato de entrada y precio
autoritativo evita confiar en datos manipulables del cliente.

**Relación con SOAP:** Si hubiera un ERP legado externo, se definiría un WSDL
para validar/reservar stock y un Fault para stock insuficiente. Ese escenario
está diseñado en `clase09/Actividad_Clase09_ServiciosWeb.pdf`; **no existe como
código ni es necesario para el flujo real actual**.

\newpage

## Nicolás — QA

**Lo que diría:**

“Probamos tanto las reglas como el recorrido. Las pruebas Java del componente
Pedido verifican el recálculo del total, envío gratis desde el umbral,
rechazo de stock insuficiente, productos duplicados y pedido vacío. Las
pruebas anteriores del catálogo y el front siguen pasando. También desplegamos
el WAR en WildFly con Artemis: un POST válido devolvió 202, el GET posterior
mostró `PROCESADO`, y un POST con 999 unidades devolvió 400.”

“No alcanza con que compile: necesitamos observar al consumidor. La evidencia
manual muestra ese recorrido. Para una prueba de falla posterior tenemos que
detener el consumidor y comprobar reentrega, además de agregar persistencia
real y revisar seguridad del endpoint. Esas partes no se presentan como
terminadas.”

“La **clase 8** explica que el broker separa productor y consumidor. Por eso
no probamos sólo el `202` del POST: también verificamos que el GET cambie a
`PROCESADO`. Y con la API REST comprobamos un caso inválido que devuelve `400`
sin vaciar el carrito.”

**Dónde señalar:** `PedidoServiceTest.java` en `src/test/java`, los tests de
Catálogo, `src/test/js/` y las respuestas REST de la demo.

**Por qué y beneficio:** Los tests aíslan reglas de negocio del broker; la
prueba en WildFly confirma que las anotaciones, el destino JMS y el MDB
realmente se conectan. Ambas pruebas se complementan.

\newpage

## Mapa de lo visto en las últimas clases

| Tema | Uso en GOMARKET | Beneficio | Estado honesto |
|:--|:--|:--|:--|
| Mensajería / JMS, clase 8 | `PedidoService` publica en `ProcesarPedidoQueue`; `ProcesarPedidoMDB` consume | Desacoplamiento temporal, una tarea para un consumidor | **Funciona** en WildFly `standalone-full.xml` |
| Servicios Web / SOAP, clase 9 | Diseño de ERP legado con WSDL, operación y Fault en la actividad de clase | Contrato formal cuando un tercero sólo ofrece SOAP | **Diseño hipotético**, no implementado |
| REST, clase del 09/10 | `POST /api/pedidos`, `GET /api/pedidos/{id}` y API de catálogo JAX-RS | JSON simple, URL de recurso y estados HTTP claros | **Funciona** en WildFly |
| Capas y componentes, clases anteriores | Resource → Service → Repository; front organizado igual | Separación de responsabilidades y pruebas aisladas | Implementado parcialmente: pedidos aún en memoria |

**Próximo avance verificable:** persistir pedidos, autenticar y autorizar en
servidor, hacer reserva de stock transaccional, definir reintentos y DLQ, y
sumar un tópico sólo cuando haya varios consumidores reales. No mezclar
“pedido procesado por el MDB” con “compra pagada”.
